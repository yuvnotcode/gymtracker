param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$BackupFile
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$resolvedBackup = Resolve-Path -LiteralPath $BackupFile -ErrorAction Stop
$backupPath = $resolvedBackup.ProviderPath
$backupItem = Get-Item -LiteralPath $backupPath
$containerBackupPath = '/tmp/gymtracker-restore.sql'

if ($backupItem.Extension -ne '.sql' -or $backupItem.Length -eq 0) {
    throw 'Choose a non-empty .sql database backup file.'
}

Write-Warning 'Restoring replaces the current GymTracker database contents with the backup contents.'
$confirmation = Read-Host 'Type RESTORE to continue'
if ($confirmation -cne 'RESTORE') {
    Write-Output 'Restore cancelled. The database was not changed.'
    return
}

$appWasRunning = $false
$appStopped = $false
$restoreSucceeded = $false
$containerId = $null

Push-Location $repoRoot
try {
    $containerId = (& docker compose -f docker-compose.yml -f docker-compose.local.yml ps -q db | Out-String).Trim()
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($containerId)) {
        throw 'The database container is not running. Start the local stack first.'
    }

    $appContainerId = (& docker compose -f docker-compose.yml -f docker-compose.local.yml ps -q app | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not check the app container status.'
    }
    $appWasRunning = -not [string]::IsNullOrWhiteSpace($appContainerId)

    & (Join-Path $PSScriptRoot 'backup-database.ps1')
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not create a safety backup of the current database; restore was cancelled.'
    }

    if ($appWasRunning) {
        & docker compose -f docker-compose.yml -f docker-compose.local.yml stop app
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not stop the app; restore was cancelled.'
        }
        $appStopped = $true
    }

    & docker cp $backupPath "${containerId}:$containerBackupPath"
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not copy the selected backup into the database container.'
    }

    $restoreCommand = "PGPASSWORD=`$POSTGRES_PASSWORD psql --username=gymtracker_app --dbname=gymtracker --set ON_ERROR_STOP=1 < $containerBackupPath"
    & docker compose -f docker-compose.yml -f docker-compose.local.yml exec -T db sh -c $restoreCommand
    if ($LASTEXITCODE -ne 0) {
        throw 'Database restore failed. The pre-restore safety backup is in the backups folder.'
    }

    $restoreSucceeded = $true
    Write-Output 'Database restore completed.'
}
finally {
    if ($containerId) {
        & docker compose -f docker-compose.yml -f docker-compose.local.yml exec -T db rm -f $containerBackupPath 2>$null
    }

    if ($appStopped -and $restoreSucceeded) {
        & docker compose -f docker-compose.yml -f docker-compose.local.yml start app
        if ($LASTEXITCODE -ne 0) {
            Write-Warning 'The data was restored, but the app did not restart. Start it with docker compose up -d.'
        }
    } elseif ($appStopped) {
        Write-Warning 'The app was left stopped because the restore did not complete. Restore the safety backup before restarting it.'
    }

    Pop-Location
}

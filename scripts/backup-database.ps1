$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$backupDirectory = Join-Path $repoRoot 'backups'
$backupTimestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupPath = Join-Path $backupDirectory "gymtracker-$backupTimestamp.sql"
$containerBackupPath = '/tmp/gymtracker-backup.sql'

Push-Location $repoRoot
try {
    $containerId = (& docker compose -f docker-compose.yml -f docker-compose.local.yml ps -q db | Out-String).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not check the database container. Make sure Docker Desktop is running.'
    }
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw 'The GymTracker database is not running. Start it with docker compose -f docker-compose.yml -f docker-compose.local.yml up -d.'
    }

    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null

    $dumpCommand = "PGPASSWORD=`$POSTGRES_PASSWORD pg_dump --username=gymtracker_app --dbname=gymtracker --no-owner --no-privileges > $containerBackupPath"
    & docker compose -f docker-compose.yml -f docker-compose.local.yml exec -T db sh -c $dumpCommand
    if ($LASTEXITCODE -ne 0) {
        throw 'Database export failed. No backup file was created.'
    }

    & docker cp "${containerId}:$containerBackupPath" $backupPath
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not copy the database export to the backups folder.'
    }

    & docker compose -f docker-compose.yml -f docker-compose.local.yml exec -T db rm -f $containerBackupPath
    if ($LASTEXITCODE -ne 0) {
        Write-Warning 'The backup was saved, but the temporary copy could not be removed from the database container.'
    }

    $backupFile = Get-Item -LiteralPath $backupPath
    if ($backupFile.Length -eq 0) {
        Remove-Item -LiteralPath $backupPath -Force
        throw 'The database export was empty, so the empty backup file was removed.'
    }

    Write-Output "Database backup saved: $backupPath"
    Write-Output "Size: $([Math]::Round($backupFile.Length / 1KB, 1)) KB"
}
finally {
    Pop-Location
}

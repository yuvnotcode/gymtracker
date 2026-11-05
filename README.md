# Gymtracker

Gymtracker is a personal workout logging app built with Java 25, Spring Boot, MySQL, and a responsive browser interface. It lets you record workout sessions and working sets, keep unfinished sessions as drafts, review workout history, track exercise progress, choose a training split, and use a rest timer while training.

## Features

- Account registration and sign in
- Workout sessions with multiple exercises
- Per-set reps, weight, and reps in reserve (RIR)
- Draft workouts that can be minimized and resumed on the same browser
- Workout history with expandable exercise and set details
- Session deletion
- Weekly session count, training volume, and streak summary
- Exercise progress report and personal bests
- Saved training splits: Push/Pull/Legs, Upper/Lower, Bro Split, and Full Body
- Rest timer with 60, 90, 120, and 180 second intervals
- Responsive layout for desktop and mobile browsers

## Requirements

- Java Development Kit (JDK) 25
- Apache Maven 3.9 or newer
- MySQL Server running locally
- MySQL Workbench (optional GUI for creating and viewing the database)

> MySQL Workbench is a database client; make sure MySQL Server is installed and running as well.

## 1. Create the database

1. Open MySQL Workbench and connect to your local MySQL Server.
2. Open a new SQL tab.
3. Run this statement using the lightning bolt button:

   ```sql
   CREATE DATABASE gymtracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER 'gymtracker_app'@'localhost' IDENTIFIED BY 'choose-a-strong-local-password';
   GRANT ALL PRIVILEGES ON gymtracker.* TO 'gymtracker_app'@'localhost';
   ```

The application creates or updates its tables when it starts.

## 2. Set Java and database settings in PowerShell

Open PowerShell and go to the Maven project folder:

```powershell
cd C:\Users\yuva8\Documents\gymtracker\gymtracker
```

If Java 25 is not already selected in this PowerShell window, set it (adjust the path if your JDK is installed elsewhere):

```powershell
$env:JAVA_HOME = "C:\Users\yuva8\.jdks\azul-25.0.5.0.3"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Check Java and Maven use Java 25:

```powershell
java -version
mvn -version
```

Set the MySQL connection values. When prompted, enter the password you chose for `gymtracker_app`:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/gymtracker"
$env:DB_USERNAME = "gymtracker_app"
$env:DB_PASSWORD = Read-Host "MySQL password"
$jwtBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

These values apply only to the current PowerShell window. Keep the JWT secret private. If you close the window, set the values again before starting the app.

## 3. Start Gymtracker

From the `gymtracker` Maven project folder, run:

```powershell
mvn spring-boot:run
```

Keep that PowerShell window open while using the app. Open **http://localhost:8081** in your browser and create an account.

Stop the app by selecting its PowerShell window and pressing **Ctrl+C**.

## Troubleshooting

- **`release version 25 not supported`:** `mvn -version` is using an older Java runtime. Set `JAVA_HOME` and `Path` to your JDK 25 `bin` folder, then check `mvn -version` again.
- **Cannot connect to MySQL:** Confirm MySQL Server is running, the `gymtracker` schema exists, and `DB_USERNAME` / `DB_PASSWORD` are correct.
- **Missing JWT secret:** Set `JWT_SECRET` in the same PowerShell window before starting the app. Use the command above to generate a fresh one.
- **Changes do not appear in the browser:** Restart the app, then hard-refresh the page with **Ctrl+Shift+R**.

## Project layout

```text
gymtracker/                 Git repository root
├── README.md
└── gymtracker/              Spring Boot / Maven project
    ├── pom.xml
    └── src/main/
        ├── java/            API, security, services, and database entities
        └── resources/
            ├── application.yaml
            └── static/      Browser application (HTML, CSS, JavaScript)
```

## Security note

Database credentials and the JWT signing key are supplied through environment variables and should never be committed. The generated JWT secret is temporary for this local run; changing it signs out existing sessions. Earlier versions of this project had local credentials in tracked files. If those versions were pushed to a public GitHub repository, change the exposed MySQL password and treat the old JWT key as compromised; deleting the values in a new commit does not erase them from Git history. This project is intended for local development and has not been configured for public production deployment.

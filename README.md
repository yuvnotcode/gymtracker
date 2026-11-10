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
- Custom weekly split sequences with editable training days and rest days
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
   CREATE USER 'gymtracker_app'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'choose-a-strong-local-password';
   GRANT ALL PRIVILEGES ON gymtracker.* TO 'gymtracker_app'@'localhost';
   ```

The application creates or updates its tables when it starts.

If `gymtracker_app` already exists, update it to use the supported MySQL password plugin and choose a new password:

```sql
ALTER USER 'gymtracker_app'@'localhost' IDENTIFIED WITH caching_sha2_password BY 'choose-a-new-strong-local-password';
GRANT ALL PRIVILEGES ON gymtracker.* TO 'gymtracker_app'@'localhost';
```

If you are upgrading an existing local database from before custom splits were added, run this one-time migration in Workbench. The earlier MySQL `ENUM` column cannot store the new `CUSTOM` split value:

```sql
ALTER TABLE gymtracker.users MODIFY COLUMN split_type VARCHAR(32) NULL;
```

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
- **`Unable to load authentication plugin 'auth_gssapi_client'`:** The MySQL account is configured for GSSAPI authentication, which Connector/J cannot load here. Run the `ALTER USER` statement above in Workbench, then enter that new password for `$env:DB_PASSWORD` in PowerShell.
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

Database credentials and the JWT signing key are supplied through environment variables and should never be committed. The generated JWT secret is temporary for this local run; changing it signs out existing sessions. Earlier versions of this project had local credentials in tracked files. If those versions were pushed to a public GitHub repository, change the exposed MySQL password and treat the old JWT key as compromised; deleting the values in a new commit does not erase them from Git history. The Docker setup below is for a small private beta, not a production launch.

## Private beta on a server

The repository includes a Docker Compose setup for an invite-only beta. It runs the Spring Boot app, a persistent MySQL database, and Caddy as an HTTPS reverse proxy. Friends use the same web address from their phones; their accounts and workouts are stored in the shared database. The beta access code is required on the registration form.

### What you need

- A Linux server reachable from the internet, with Docker Engine and the Docker Compose plugin installed.
- A domain name with an `A` record pointing to the server's public IP address.
- Inbound TCP ports 80 and 443 open in the server firewall and hosting provider firewall. Caddy obtains and renews the HTTPS certificate automatically.

### Start the beta

1. Clone this repository onto the server and enter its folder.
2. Create the server environment file from the example:

   ```sh
   cp .env.example .env
   ```

3. Edit `.env`. Set `DOMAIN` to the DNS name you pointed at the server. Replace all sample secrets with unique values. Generate secrets with:

   ```sh
   openssl rand -base64 36
   ```

   Use different generated values for `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `JWT_SECRET`, and `APP_REGISTRATION_CODE`. Keep `.env` private and share only the beta access code with testers.

4. Start the services:

   ```sh
   docker compose --profile server up -d --build
   docker compose logs -f app
   ```

5. Visit `https://<your-domain>` and give friends that address and the beta access code. Stop following logs with Ctrl+C; this does not stop the app. Use `docker compose down` to stop the stack. The database stays in its Docker volume when stopped.

The database is persisted in the `gymtracker-db` Docker volume. Back it up regularly before updating the server or removing volumes. Do not use `docker compose down -v` unless you intend to erase the beta database. To update after pulling code changes, run `docker compose up -d --build` again.

This is an early beta deployment setup. Before a broader public launch, add automated database backups and restore checks, monitoring, rate limiting for sign-in and registration, and managed schema migrations. Hibernate currently updates the beta schema at startup; review/migrate that before production.

### Share a temporary public beta link for free

You can let friends reach this local app over the internet without purchasing a server or domain by using a Cloudflare Quick Tunnel. It creates a random public `trycloudflare.com` URL for temporary testing, so it is not a permanent production address. The computer running Docker must stay powered on, awake, and connected to the internet. Cloudflare documents Quick Tunnels as development tunnels with a limit of 200 in-flight requests.

From PowerShell in the repository folder, start the app, database, and temporary tunnel:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml --profile quick-tunnel up -d --build
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f tunnel
```

Wait for the `trycloudflare.com` URL in the tunnel log and open it on your phone or share it with testers. They still need the beta access code from your private `.env` file to create accounts. Do not post that code publicly. Stop following logs with Ctrl+C. Stop the tunnel and app with:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

The URL is temporary and may change when the tunnel restarts. While the tunnel is active, the app is reachable by anyone who has the URL; the registration code limits who can create accounts. Keep this for a small beta with people you trust. For a stable always-on public site, move the same Compose stack to a paid server and domain using the instructions above.

### Run on your Windows computer first

For a same-Wi-Fi trial, start Docker Desktop, copy `.env.example` to `.env`, and set `DOMAIN=localhost` plus unique values for all four secrets/codes. From PowerShell in the repository folder, run:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f app
```

Open `http://localhost:8081`. To let a friend on the same Wi-Fi try it, find your computer's local IPv4 address with `ipconfig` and share `http://<IPv4-address>:8081` plus the beta access code. Your computer and Docker Desktop must stay on. This address is only reachable on your local network. Stop the local stack with `docker compose -f docker-compose.yml -f docker-compose.local.yml down`.

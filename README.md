# Gymtracker

Gymtracker is a personal workout logging app built with Java 25, Spring Boot, PostgreSQL, and a responsive browser interface. It lets you record workout sessions and working sets, keep unfinished sessions as drafts, review workout history, track exercise progress, choose a training split, and use a rest timer while training.

## Features

- Account registration and sign in
- Account data export as CSV and password-confirmed account deletion
- Workout sessions with multiple exercises
- Per-set reps, weight, and reps in reserve (RIR)
- Searchable exercise library with muscle-group and equipment filters, plus custom exercise names
- Draft workouts that can be minimized and resumed on the same browser
- Workout history with expandable exercise and set details
- Session deletion
- Weekly session count, training volume, and streak summary
- Exercise progress report and personal bests
- Saved training splits: Push/Pull/Legs, Upper/Lower, Bro Split, and Full Body
- Custom weekly split sequences with editable training days and rest days
- Next workout exercise ideas matched to the upcoming split day and varied using the most recent workout
- Rest timer with 60, 90, 120, and 180 second intervals
- Responsive layout for desktop and mobile browsers
- Installable phone app (PWA) with cached app shell for offline loading

## Run locally with Docker

Requirements: Docker Desktop with Compose, and a `.env` file containing `DB_PASSWORD`, `JWT_SECRET`, and `APP_REGISTRATION_CODE`.

```powershell
Copy-Item .env.example .env
notepad .env
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
```

Open **http://localhost:8081**. PostgreSQL runs in a Docker container and stores its data in the `gymtracker-postgres-data` volume. To stop the app while keeping the data, run:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml down
```

The app creates or updates its tables at startup. For backup and restore commands, see the database scripts below.

## Deploy on Render with Aiven PostgreSQL

The repository includes `render.yaml` for the Render web service. The app listens on Render's assigned `PORT` and uses environment variables for its Aiven database connection and secrets.

1. In Aiven, create a free PostgreSQL service and wait for it to become **Running**. From **Quick connect**, copy its hostname, port, database name, username, and password. The free tier has 1 GB storage and may power down after extended inactivity. See [Aiven free tier details](https://aiven.io/docs/products/postgresql/concepts/pg-free-tier).
2. Push this repository to GitHub, then in Render choose **New → Blueprint** and connect the repository. Render reads `render.yaml` and prompts for the database and secret environment variables.
3. Set `DB_URL` to `jdbc:postgresql://<HOST>:<PORT>/<DATABASE>?sslmode=require`, using the exact connection values from Aiven. Set `DB_USERNAME` and `DB_PASSWORD` to the Aiven username and password.
4. Set `JWT_SECRET` to a new private 32-byte Base64 secret. Set `APP_REGISTRATION_CODE` to a private invite code for your testers. Do not commit either secret or share the JWT secret.
5. Create the Blueprint service and wait for its first deploy to finish. Open its `onrender.com` URL and verify registration, sign-in, workout saving, and progress reports before sharing the invite code.

Render's free web service sleeps after 15 minutes without traffic and can take about a minute to wake up. [Render free plan details](https://render.com/docs/free).

### Optional: configure Google sign-in

1. In Google Cloud Console, create an OAuth client ID with application type **Web application**.
2. Add `http://localhost:8081` as an authorized JavaScript origin for local development. After the Render service has a URL, add that exact HTTPS origin too.
3. Set `GOOGLE_CLIENT_ID` in the local `.env` or Render service environment and restart/redeploy the app. A Google client secret is not used by this sign-in flow.

New Google accounts still need the beta registration code. Returning Google accounts do not need to enter it again. An existing password account is not automatically linked to Google by matching email; its owner should continue using the existing sign-in method.

## Create a JWT secret in Windows PowerShell

PowerShell 5.1 compatible command:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

Keep the output private and paste it only into the `JWT_SECRET` environment variable.

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

Database credentials and the JWT signing key are supplied through environment variables and should never be committed. Changing the JWT secret signs out existing sessions. Earlier versions of this project had local credentials in tracked files. If those versions were pushed to a public GitHub repository, change any exposed database password and treat the old JWT key as compromised; deleting the values in a new commit does not erase them from Git history. The Docker setup below is for a small private beta, not a production launch.

## Private beta on a server

### Install Gymtracker on a phone

Gymtracker can be added to a phone's home screen from its browser. The app shell and exercise catalog are cached for faster repeat visits and to open the interface offline. Signing in, loading saved workouts, and saving workouts still require an internet connection.

- **Android (Chrome):** open the HTTPS app URL, tap the browser menu, then choose **Install app** or **Add to Home screen**.
- **iPhone (Safari):** open the HTTPS app URL, tap **Share**, choose **Add to Home Screen**, then tap **Add**.

The temporary Cloudflare Quick Tunnel uses HTTPS, so it can be installed, but its random address may change when the tunnel is restarted. For a stable address without renting a server, use the named Cloudflare Tunnel setup below. It gives you a fixed `*.trycloudflare.com` hostname, but the Windows computer running Gymtracker must stay on and connected. For an always-on service, use a server and your own domain instead.

The repository includes a Docker Compose setup for an invite-only beta. It runs the Spring Boot app, a persistent PostgreSQL database, and Caddy as an HTTPS reverse proxy. Friends use the same web address from their phones; their accounts and workouts are stored in the shared database. The beta access code is required on the registration form.

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

   Use different values for `DB_PASSWORD`, `JWT_SECRET`, and `APP_REGISTRATION_CODE`. Keep `.env` private and share only the beta access code with testers.

4. Start the services:

   ```sh
   docker compose --profile server up -d --build
   docker compose logs -f app
   ```

5. Visit `https://<your-domain>` and give friends that address and the beta access code. Stop following logs with Ctrl+C; this does not stop the app. Use `docker compose down` to stop the stack. The database stays in its Docker volume when stopped.

The database is persisted in the `gymtracker-postgres-data` Docker volume. Back it up regularly before updating the server or removing volumes. Do not use `docker compose down -v` unless you intend to erase the beta database. To update after pulling code changes, run `docker compose up -d --build` again.

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
docker compose -f docker-compose.yml -f docker-compose.local.yml --profile quick-tunnel down
```

The URL is temporary and may change when the tunnel restarts. While the tunnel is active, the app is reachable by anyone who has the URL; the registration code limits who can create accounts. Keep this for a small beta with people you trust. For a stable always-on public site, move the same Compose stack to a paid server and domain using the instructions above.

### Friends beta checklist

Before inviting friends, create a dated database backup with `.\scripts\backup-database.ps1`. Then use this checklist with a spare/test account where possible:

1. Register a second account with the beta code and confirm it can sign in on a phone.
2. Log a workout with more than one exercise and more than one set. Confirm it appears in the history and progress report.
3. Sign out and sign in to another account. Confirm the first account's workouts are not shown.
4. Open **Account & data**, download the CSV, and check that it opens in Excel or Google Sheets with each set on its own row.
5. Install Gymtracker from the HTTPS link. Open it once online, then enable airplane mode and reopen it. The cached interface should load and explain that saving needs internet; reconnect before saving real workout data.
6. If you want to verify account deletion, use a disposable test account only. It requires the password and typing `DELETE`, and removes that account's workouts and training split.
7. Stop and restart the Compose stack, then sign back in and confirm the database data remains.

Keep the backup somewhere private. Never share a real user's CSV export or database backup with testers.

### Keep one Cloudflare address without renting a server

A named Cloudflare Tunnel keeps the same public hostname across restarts. Cloudflare assigns a stable `*.trycloudflare.com` hostname when you create the named tunnel; a hostname under your own domain requires a domain added to your Cloudflare account. The tunnel is still hosted by your Windows computer, so it is reachable only while that computer and Docker Desktop are on and connected.

1. In the Cloudflare dashboard, open **Zero Trust → Networks → Tunnels** and create a named tunnel. Choose the Windows connector and follow Cloudflare's steps to install `cloudflared` as a Windows service using the tunnel token. Keep that token private; anyone with it can run your tunnel.
2. In the tunnel's **Public Hostnames** section, add a hostname. For the service, choose `HTTP` and enter `localhost:8081` (or `http://localhost:8081`, depending on the dashboard field). If you use your own domain, first add it to Cloudflare and select it here. Cloudflare also offers a `trycloudflare.com` hostname for named tunnels, depending on account availability.
3. Start Gymtracker locally, without the Quick Tunnel profile:

   ```powershell
   docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
   ```

4. Keep Docker Desktop and the `cloudflared` service running. Open the configured hostname and share that same address with testers. Restarting the app or tunnel keeps the hostname; stopping the computer takes the site offline.

The existing `quick-tunnel` Compose profile remains useful for a one-off preview. Do not run it alongside the named tunnel unless you intentionally want two public links. The hostname is stable, but this home-hosted setup is not an always-on production deployment. Use the server setup above for that.

### Run on your Windows computer first

For a same-Wi-Fi trial, start Docker Desktop, copy `.env.example` to `.env`, and set `DOMAIN=localhost` plus unique values for all four secrets/codes. From PowerShell in the repository folder, run:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.local.yml logs -f app
```

Open `http://localhost:8081`. To let a friend on the same Wi-Fi try it, find your computer's local IPv4 address with `ipconfig` and share `http://<IPv4-address>:8081` plus the beta access code. Your computer and Docker Desktop must stay on. This address is only reachable on your local network. Stop the local stack with `docker compose -f docker-compose.yml -f docker-compose.local.yml down`.

### Back up the local beta database

Make sure Docker Desktop is running and start the local app/database if needed:

```powershell
docker compose -f docker-compose.yml -f docker-compose.local.yml up -d
```

Then create a dated SQL backup:

```powershell
.\scripts\backup-database.ps1
```

The backup is saved under `backups/`. That folder is ignored by Git because the dump contains account and workout data. Keep a copy somewhere private and separate from this computer. The Docker database volume remains the live database; a dump is the copy to use if you need to restore or migrate the data later.

To restore a backup to this local Docker database, use the exact backup filename and type `RESTORE` when prompted:

```powershell
.\scripts\restore-database.ps1 .\backups\gymtracker-YYYYMMDD-HHmmss.sql
```

Restore replaces the current database contents. The script makes a safety backup first, stops the app during the import if it was running, and restarts it after a successful restore. Do not restore a dump from an unfamiliar source.

The app displays an offline notice when the device loses its connection. In offline mode, the cached interface can open, but account data and workout logs cannot be loaded or saved.

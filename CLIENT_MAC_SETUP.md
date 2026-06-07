# Client Mac Setup

This guide is for running the Muqmeen Group Takaful app on a client's Mac for code review, demo, or presentation.

Docker is the recommended route because it avoids manual Java, Maven, and Node setup.

## 1. Install Required Apps

Install these on the Mac:

- Docker Desktop for Mac
- Git
- Visual Studio Code

After installing Docker Desktop, open it once and wait until it says Docker is running.

## 2. Clone The Project

Open Terminal:

```bash
git clone https://github.com/s72370/smart-takaful-application-and-consultation.git
cd smart-takaful-application-and-consultation
```

Open the code in VS Code:

```bash
code .
```

If the `code` command is not available, open VS Code manually and choose `File -> Open Folder`.

## 3. Run The App

For the local demo, no real Supabase or ToyyibPay credentials are required.

```bash
docker compose up --build
```

Open:

```text
http://localhost:8080
```

The first build can take a few minutes because Docker downloads Java, Node, Maven dependencies, and Tailwind dependencies.

## 4. Demo Login Details

Local Docker uses the dev profile and an in-memory H2 database.

Admin:

```text
URL: http://localhost:8080/admin
Username: admin
Password: password
```

Customer:

```text
Register a new customer account from the public site.
```

Demo data resets when the app/container is recreated because the local database is in memory.

## 5. ToyyibPay Demo Mode

ToyyibPay is intentionally kept as mock mode in Docker Compose.

That means:

- Consultation submission still works.
- Optional tips do not call real ToyyibPay.
- The app redirects to the local mock payment screen.
- No ToyyibPay category code or secret key is needed for the Mac demo.

## 6. If Port 8080 Is Already Used

Run on another local port, for example 8081:

```bash
PORT=8081 docker compose up --build
```

Then open:

```text
http://localhost:8081
```

## 7. Stop The App

Press `Control + C` in the Terminal running Docker Compose.

Then cleanly stop containers:

```bash
docker compose down
```

## 8. Useful Files To Show

- `src/main/java/com/muqmeen/takaful` - Java backend code
- `src/main/resources/templates` - Thymeleaf page templates
- `src/main/resources/static/css/input.css` - main custom styling source
- `src/main/resources/static/js/theme-select.js` - custom dropdown UI
- `Dockerfile` - container build
- `docker-compose.yml` - local Mac demo runtime

## 9. Troubleshooting

If Docker says the daemon is not running:

```text
Open Docker Desktop and wait until it finishes starting.
```

If `docker compose` is not found:

```text
Update Docker Desktop. Modern Docker Desktop includes Compose v2.
```

If the page does not load:

```bash
docker compose logs app
```

If a dependency build gets stuck or stale:

```bash
docker compose build --no-cache
docker compose up
```

## 10. Production Deployment

This Docker Compose setup is only for local demo and code review.

Railway deployment should use the Railway project environment variables for Supabase, email delivery, Gemini, admin credentials, and later ToyyibPay sandbox/live credentials.

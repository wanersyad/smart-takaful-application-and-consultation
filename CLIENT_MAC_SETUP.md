# Client Mac Setup

This guide is for running the Muqmeen Group Takaful app on a client's Mac for code review, demo, or presentation.

Docker is the recommended route because it avoids manual Java, Maven, and Node setup.

## 0. What This Project Is

This is a web application for Muqmeen Group's Takaful consultation flow.

It includes:

- Public landing page with product brochures.
- Customer registration and login.
- Customer profile and profile picture updates.
- Structured Takaful application drafts and submission.
- Customer application history, quotation status, and payment status.
- Admin login.
- Admin application review dashboard.
- Admin product management with structured product data.
- Admin quotation builder after reviewing an application.
- Stored contact enquiries from the public contact form.
- Chat assistant for basic Takaful/product questions.
- Quotation-linked ToyyibPay payment, currently mocked for local demo.

For local demo on Mac, the app runs inside Docker and uses temporary demo data. This means it is safe to test without touching the real Railway/Supabase production setup.

## 1. Install Required Apps

Install these on the Mac:

- Docker Desktop for Mac
- Git
- Visual Studio Code

After installing Docker Desktop, open it once and wait until it says Docker is running.

Do this before running any Terminal command that starts with `docker`.

## 2. Clone The Project

Open Terminal:

```bash
git clone https://github.com/wanersyad/smart-takaful-application-and-consultation.git
cd smart-takaful-application-and-consultation
```

Open the code in VS Code:

```bash
code .
```

If the `code` command is not available, open VS Code manually and choose `File -> Open Folder`.

Choose the folder named:

```text
smart-takaful-application-and-consultation
```

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

This is normal. The first run is usually the slowest.

When the app is ready, Terminal should show lines that mention Spring Boot has started.

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

## 5. Suggested Demo Flow

Use this flow when showing the app to a boss or evaluator:

1. Open `http://localhost:8080`.
2. Show the landing page, dynamic product section, FAQ, and contact form.
3. Click a product card to open its database-backed details.
4. Click the product application button.
5. Register a customer account if not signed in.
6. Save an application draft, then complete required applicant, IC, bank, measurement, nominee, and signature details.
7. Submit the application.
8. Open the customer account page to show application history.
9. Log out.
10. Open `http://localhost:8080/admin`.
11. Sign in as admin.
12. Show application management and contact enquiries.
13. Review the submitted application and create a quotation.
14. Publish the quotation, then return as the customer to show the mock payment flow.
15. Show product management and explain that admins can add/edit/archive product records.

Recommended boss-demo note:

```text
This local Docker demo uses temporary local data. The deployed Railway version uses the production environment variables and Supabase database.
```

## 6. ToyyibPay Demo Mode

ToyyibPay is intentionally kept as mock mode in Docker Compose.

That means:

- Application submission still works.
- Payment is only available after the admin publishes a quotation.
- The app redirects to the local mock payment screen.
- No ToyyibPay category code or secret key is needed for the Mac demo.

## 7. If Port 8080 Is Already Used

Run on another local port, for example 8081:

```bash
PORT=8081 docker compose up --build
```

Then open:

```text
http://localhost:8081
```

## 8. Stop The App

Press `Control + C` in the Terminal running Docker Compose.

Then cleanly stop containers:

```bash
docker compose down
```

## 9. Useful Files To Show

- `src/main/java/com/muqmeen/takaful` - Java backend code
- `src/main/resources/templates` - Thymeleaf page templates
- `src/main/resources/static/css/input.css` - main custom styling source
- `src/main/resources/static/js/theme-select.js` - custom dropdown UI
- `Dockerfile` - container build
- `docker-compose.yml` - local Mac demo runtime

## 10. Simple Folder Explanation

For a non-technical walkthrough, explain the folders like this:

- `src/main/java` - application logic, controllers, services, database models.
- `src/main/resources/templates` - HTML pages rendered by Spring Boot.
- `src/main/resources/static` - CSS, images, brochures, and browser JavaScript.
- `src/test` - automated tests.
- `Dockerfile` - tells Docker how to build the app image.
- `docker-compose.yml` - tells Docker how to run the local demo.
- `.env.example` - example environment variables. Do not put real secrets in this file.

## 11. What Not To Touch During Demo

Avoid changing these unless the developer specifically asks:

- `.env` or production environment variables.
- Railway project settings.
- Supabase database settings.
- Dockerfile build steps.
- Admin password in production.
- Any API keys or payment keys.

Safe things to click during demo:

- Product cards.
- Brochure/details buttons.
- Application form.
- Customer registration/login.
- Customer account page.
- Admin application/product screens.
- Admin quotation builder.

## 12. Local Demo vs Live Deployment

Local Docker demo:

- Runs on the Mac.
- Uses temporary in-memory data.
- Uses mock ToyyibPay.
- Uses local file storage inside the container.
- Good for code review and presentation.

Railway deployment:

- Runs online.
- Uses real environment variables.
- Connects to Supabase.
- Can use real email delivery.
- Should use private Supabase Storage for persistent file uploads in final production.
- Can later use ToyyibPay sandbox/live.

Do not assume a local Docker change is live online until it is committed, pushed, and deployed.

## 13. Troubleshooting

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

If the first build takes a long time:

```text
Wait. Docker may be downloading Java, Node, Maven dependencies, and Tailwind packages.
```

If the browser shows an old version:

```text
Refresh the page. If still unchanged, stop Docker and run docker compose up --build again.
```

If login does not work in local Docker:

```text
Use admin/password for admin, or register a fresh customer account for customer login.
```

## 14. How To Ask For Help

When asking the developer for help, send:

- Screenshot of the browser page.
- Screenshot or copied text from Terminal.
- The exact URL being opened.
- What was clicked before the issue happened.
- Whether Docker Desktop says it is running.

Useful command for logs:

```bash
docker compose logs app
```

## 15. Production Deployment

This Docker Compose setup is only for local demo and code review.

Railway deployment should use the Railway project environment variables for Supabase, email delivery, Gemini, admin credentials, and later ToyyibPay sandbox/live credentials.

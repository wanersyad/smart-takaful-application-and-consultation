# Smart Takaful & Consultation System

Muqmeen Group dynamic Takaful application system: a Spring Boot monolith for public product browsing, customer profiles, structured product applications, admin review, quotation generation, quotation-linked payment, and a grounded chatbot.

## Stack

- Java 17, Spring Boot 3.5
- Thymeleaf server-side templates
- Tailwind CSS via npm/PostCSS build pipeline
- Alpine.js and vanilla JavaScript for browser interactions
- Spring Data JPA on Supabase PostgreSQL in prod; H2 in dev
- Deployment target: Railway

## Requirements

- JDK 17 or newer
- Node.js + npm for Tailwind CSS builds
- No global Maven install required; the Maven Wrapper ships with the project
- Docker Desktop, if running through containers

## Running Locally

```bash
npm install
npm run build:css
./mvnw spring-boot:run
```

On Windows PowerShell, `./mvnw.cmd spring-boot:run` also works when the wrapper can run in the shell.

App starts on `http://localhost:8080`.

## Running With Docker

Docker is the easiest path for a new machine because it avoids installing Java, Maven, and Node separately.

```bash
docker compose up --build
```

The app will start on `http://localhost:8080` using the `dev` profile, H2 in-memory database, mock ToyyibPay, and local admin defaults:

```text
username: admin
password: password
```

The included Compose file is intentionally local-dev only, so it always uses the `dev` profile, H2, and mock ToyyibPay. Railway/client deployment should use Railway environment variables rather than this local Compose file.

For a client Mac setup and boss-demo checklist, see `CLIENT_MAC_SETUP.md`.

Useful Docker commands:

```bash
docker compose down
docker compose up --build
docker build -t muqmeen-takaful-web:local .
```

If port 8080 is already used:

```bash
PORT=8081 docker compose up --build
```

## Key Routes

| Route | Purpose |
|---|---|
| `GET /` | Landing page with database-backed product cards and chatbot |
| `GET /products/{id}` | Public product detail page with DB-backed benefits, coverage, requirements, and documents |
| `GET /login` / `POST /login` | Shared customer/admin login |
| `GET /register` / `POST /register` | Customer account signup |
| `GET /account` | Customer profile summary, application history, quotation/payment status |
| `GET /account/profile` / `POST /account/profile` | Customer profile and profile picture update |
| `GET /applications/new` | Start a product application draft |
| `GET /applications/{id}` / `POST /applications/{id}` | Customer application detail, draft update, and submission |
| `GET /files/{id}` | Authenticated private file download for owners/admins |
| `POST /quotations/{id}/pay` | Customer starts payment for a published quotation |
| `GET /payment/mock/{billCode}` | Simulated ToyyibPay gateway |
| `GET /payment/return` | User-facing ToyyibPay return page |
| `POST /payment/callback` | ToyyibPay callback; hash-verified before payment status changes |
| `GET /success` | Post-submission confirmation |
| `POST /api/chat` | Public chatbot endpoint with CSRF and rate limiting |
| `POST /contact` | Public contact form; sends product enquiries to the configured recipient |
| `GET /admin/dashboard` | Protected application review queue |
| `GET /admin/applications/{id}` | Admin application detail, files, nominee data, and status control |
| `GET /admin/applications/{id}/quotation` | Admin quotation builder |
| `GET /admin/products` | Protected structured product CRUD |

## Environment

Environment variables are listed in `.env.example`. Copy it to a local `.env` file and fill in real values when needed.

- `SPRING_PROFILES_ACTIVE` - `dev` by default, or `prod`
- `SPRING_DATASOURCE_URL` - Supabase transaction pooler JDBC URL
- `SPRING_DATASOURCE_USERNAME` - Supabase DB username
- `SPRING_DATASOURCE_PASSWORD` - Supabase DB password
- `ADMIN_USERNAME` / `ADMIN_PASSWORD` - Spring Security admin login for `/admin/**`
- `GEMINI_API_KEY` - Google AI Studio key for the floating chatbot
- `APP_BASE_URL` - public base URL used in ToyyibPay return/callback URLs
- `TOYYIBPAY_MODE` - `mock`, `sandbox`, or `live`
- `TOYYIBPAY_SECRET_KEY` / `TOYYIBPAY_CATEGORY_CODE` - ToyyibPay bill credentials
- `TOYYIBPAY_BASE_URL` - `https://dev.toyyibpay.com` for sandbox or `https://toyyibpay.com` for live
- `CONTACT_RECIPIENT` - email address that receives landing-page contact form messages
- `CONTACT_DELIVERY` - `auto`, `resend`, `smtp`, or `formsubmit`; use `resend` on Railway when SMTP is blocked
- `RESEND_API_KEY` / `RESEND_BASE_URL` - HTTPS email API settings for contact form delivery
- `SPRING_MAIL_HOST` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` - SMTP settings used by the contact form
- `CONTACT_FORMSUBMIT_ENABLED` - fallback email delivery through FormSubmit when SMTP is not configured; defaults to `true`
- `FILE_STORAGE_MODE` - `local` for Docker/dev uploads or `supabase` for private Supabase Storage
- `LOCAL_UPLOAD_DIR` - local upload folder used by Docker/dev
- `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` / `SUPABASE_STORAGE_BUCKET` - private Supabase Storage settings
- `DEMO_SEED_ENABLED` - dev-only optional sample product seed; keep `false` for Railway/prod

Spring Boot reads these env vars directly, so Railway env management works without committed secrets.

## Demo Seed and Supabase Hardening

Production should start from real admin-created product records, not hardcoded products. For a quick local demo, set `SPRING_PROFILES_ACTIVE=dev` and `DEMO_SEED_ENABLED=true`; if the product table is empty, the app inserts sample structured products with benefits, coverage items, requirements, and documents.

After Railway/Supabase creates the dynamic tables through Hibernate, run `supabase/dynamic-system-rls.sql` in the Supabase SQL Editor. It enables RLS on the public tables, keeps the private storage bucket private, and revokes direct Data API access so sensitive customer/application rows remain accessible through the Java server routes only.

## Accounts, Applications, and Payments

Customers can browse products publicly, but starting an application requires a customer account. Product records are stored in the database with structured benefits, coverage items, requirements, notes, images, and optional documents.

Customers can maintain profile data and upload a profile picture. Applications store their own submitted snapshot: applicant details, IC front/back images, work and income information, bank details, height/weight, nominee information, and signature image.

Payment happens only after admin review. The admin creates and publishes a quotation with selected payable items, then the customer starts ToyyibPay payment from the quotation. Dev/Docker uses mock ToyyibPay; sandbox/live can be configured later through environment variables.

## Frontend Build

Tailwind is compiled into `src/main/resources/static/css/app.css`.

```bash
npm run build:css
npm run watch:css
```

Use `build:css` before running tests, packaging, or deploying after changing template classes or `input.css`.

## Project Layout

```text
takaful-web-java/
|-- pom.xml
|-- package.json
|-- tailwind.config.js
|-- postcss.config.js
|-- .env.example
|-- src/main/
|   |-- java/com/muqmeen/takaful/
|   |   |-- SmartTakafulApplication.java
|   |   |-- config/
|   |   |-- domain/
|   |   |-- repository/
|   |   |-- service/
|   |   `-- web/
|   `-- resources/
|       |-- application.yml
|       |-- application-dev.yml
|       |-- application-prod.yml
|       |-- static/css/
|       `-- templates/
`-- src/test/
```

## Roadmap Status

1. Architecture setup - complete
2. Database integration - complete
3. Customer profiles and application intake - in progress
4. Structured admin product CRUD - complete
5. Admin review and quotation builder - in progress
6. Quotation-linked ToyyibPay mock - complete locally
7. Gemini chatbot - complete

Spring Security protects `/admin/**` for admins and `/account/**`, `/applications/**`, `/quotations/**`, and `/files/**` for authenticated users. Public browsing, signup/login, product detail pages, payment return/callback routes, contact, and chat remain accessible as needed.

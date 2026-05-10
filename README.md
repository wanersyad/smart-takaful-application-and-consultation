# Smart Takaful & Consultation System

Muqmeen Group digital funnel: a Spring Boot monolith that replaces manual Takaful lead generation with a public consultation form, protected admin dashboard, product management, and a grounded chatbot.

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

## Running Locally

```bash
npm install
npm run build:css
./mvnw spring-boot:run
```

On Windows PowerShell, `./mvnw.cmd spring-boot:run` also works when the wrapper can run in the shell.

App starts on `http://localhost:8080`.

## Key Routes

| Route | Purpose |
|---|---|
| `GET /` | Landing page with product cards, consultation modal, and chatbot |
| `GET /login` / `POST /login` | Shared customer/admin login |
| `GET /register` / `POST /register` | Customer account signup |
| `GET /account` | Customer consultation and tip history |
| `POST /submit-lead` | Authenticated customer consultation request; redirects to ToyyibPay or success |
| `GET /payment/mock/{billCode}` | Simulated ToyyibPay gateway |
| `GET /payment/return` | User-facing ToyyibPay return page |
| `POST /payment/callback` | ToyyibPay callback; hash-verified before payment status changes |
| `GET /success` | Post-submission confirmation |
| `POST /api/chat` | Public chatbot endpoint with CSRF and rate limiting |
| `POST /contact` | Public contact form; sends product enquiries to the configured recipient |
| `GET /admin/dashboard` | Protected leads table and total tips KPI |
| `GET /admin/products` | Protected product CRUD |

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
- `SPRING_MAIL_HOST` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` - SMTP settings used by the contact form

Spring Boot reads these env vars directly, so Railway env management works without committed secrets.

## Accounts and Payments

Customers can browse products publicly, but submitting a consultation requires a customer account. Product CTAs redirect anonymous visitors to login/register and return them to the selected product after sign-in.

Consultations remain free. Optional tips use ToyyibPay bills in sandbox/live mode and a local mock gateway in dev. ToyyibPay callbacks are verified with the documented MD5 formula before the app marks a payment as paid, pending, or failed.

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
3. Lead persistence - complete
4. Admin product CRUD - complete
5. UI overhaul - complete
6. Gemini chatbot - complete

Spring Security protects `/admin/**` for admins and `/account` plus `/submit-lead` for signed-in customers. Public browsing, signup/login, payment return/callback routes, success pages, brochures, contact, and chat remain accessible as needed.

# Smart Takaful & Consultation System

Muqmeen Group dynamic Takaful application system. This is no longer a static brochure or simple lead-capture site: product data, customer profiles, application records, private files, admin review, quotations, contact enquiries, and payment status are all database-backed and flow through the Spring Boot application.

## Current Status

The dynamic MVP is implemented and ready for final verification/polish:

- Public users can browse live product records from the database.
- Customers can register, log in, update their profile, upload a profile picture, start application drafts, submit full Takaful applications, and track status/quotations.
- Admins can manage products, review customer applications, access uploaded files through protected routes, request corrections, create quotations, publish payable items, and manage contact enquiries.
- Product details are stored as structured database fields and child rows, not as PDF-only content.
- Sensitive uploads use file metadata in the database and local/Supabase storage for bytes.
- Payment is only available after admin review and quotation publication.
- Local payment is currently simulated through ToyyibPay mock mode. Real ToyyibPay sandbox/live is intentionally left for the final integration pass.
- Feedback/review ratings and optional tips were removed from the application flow.

## Stack

- Java 17
- Spring Boot 3.5
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA / Hibernate
- Supabase PostgreSQL for production
- H2 in-memory database for local `dev`
- Tailwind CSS via npm/PostCSS build pipeline
- Vanilla JavaScript and small Alpine-style interactions where needed
- Font Awesome icons
- Railway deployment target

No React, Vue, Next.js, SPA framework, microservices, or alternate ORM is used.

## Main System Flows

### 1. Manage User

Input:

- customer registration/login
- profile details
- profile picture

Process:

- validates account/profile form data
- stores customer and `CustomerProfile` records
- stores profile picture through `StoredFile`

Output:

- authenticated customer account
- retrievable profile data
- application forms can prefill from profile data

### 2. Manage Products

Input:

- admin product fields
- benefits
- coverage items
- requirements
- product images/documents
- active/featured/archive state

Process:

- admin CRUD writes structured product tables
- public pages query active product records
- archived products disappear publicly but remain in admin history

Output:

- public product cards/detail pages
- selectable products for customer applications
- optional supporting documents

### 3. Book Consultation / Apply Product

Input:

- selected product
- applicant details
- IC front image
- IC back image
- email/phone/address
- occupation and position
- employer name and workplace address
- annual income
- bank name and account number
- height and weight
- nominee rows
- signature image

Process:

- customer creates a draft
- profile data can prefill the form
- submitted application stores its own snapshot
- files are stored privately with database metadata
- admin reviews and can request corrections

Output:

- customer-visible application status
- admin-visible application record, documents, nominees, and review notes

### 4. Make Payment

Input:

- admin-generated quotation
- selected payable quotation items

Process:

- admin publishes quotation after review
- customer views selected items and total
- customer starts payment
- payment status updates quotation/application

Output:

- quotation/payment status visible to customer and admin
- local mock ToyyibPay flow works for development/demo

## Key Domain Models

- `Customer`
- `CustomerProfile`
- `Product`
- `ProductBenefit`
- `ProductCoverageItem`
- `ProductRequirement`
- `ProductDocument`
- `ConsultationApplication`
- `ApplicationNominee`
- `StoredFile`
- `ContactInquiry`
- `Quotation`
- `QuotationItem`
- `Payment`
- `SiteContentBlock`
- `PasswordResetToken`

## Key Routes

| Route | Purpose |
|---|---|
| `GET /` | Landing page with database-backed products, metrics, dynamic content, contact form, and chatbot |
| `GET /products/{id}` | Public product detail page with DB-backed benefits, coverage, requirements, notes, and documents |
| `GET /login` / `POST /login` | Customer login; admin also authenticates through Spring Security |
| `GET /register` / `POST /register` | Customer account signup |
| `GET /forgot-password` / `POST /forgot-password` | Customer password reset request |
| `GET /reset-password` / `POST /reset-password` | Customer password reset completion |
| `GET /account` | Customer profile summary, application history, quotation/payment status |
| `GET /account/profile` / `POST /account/profile` | Customer profile and profile picture update |
| `GET /applications/new` | Start a product application draft |
| `GET /applications/{id}` | Customer application detail and quotation view |
| `GET /applications/{id}/edit` | Edit a draft or correction-requested application |
| `POST /applications/{id}` | Save draft or submit application |
| `POST /applications/{id}/delete` | Delete draft application |
| `GET /files/{id}` | Authenticated private file download for owner/admin |
| `POST /quotations/{id}/pay` | Customer starts payment for published quotation |
| `GET /payment/mock/{billCode}` | Local simulated ToyyibPay gateway |
| `GET /payment/return` | User-facing payment return page |
| `POST /payment/callback` | ToyyibPay callback; hash-verified before status changes |
| `POST /contact` | Public contact form; stores enquiries and sends email when configured |
| `POST /api/chat` | Public chatbot endpoint with CSRF and rate limiting |
| `GET /admin/dashboard` | Admin application review queue and contact enquiries |
| `GET /admin/applications/{id}` | Admin application detail, files, nominees, status, and review notes |
| `GET /admin/applications/{id}/quotation` | Admin quotation builder |
| `POST /admin/applications/{id}/quotation` | Save quotation items |
| `POST /admin/quotations/{id}/publish` | Publish quotation to customer |
| `GET /admin/products` | Admin product list |
| `GET /admin/products/new` | Admin create product form |
| `GET /admin/products/{id}` | Admin full product detail |
| `GET /admin/products/{id}/edit` | Admin edit product form |
| `POST /admin/products/{id}/delete` | Archive product |
| `GET /admin/customers` | Admin customer list |
| `GET /admin/customers/{id}` | Admin customer profile/application detail |
| `GET /admin/content` | Admin editable landing/content blocks |

## Security

Spring Security protects:

- `/admin/**` for `ROLE_ADMIN`
- `/account/**` for authenticated customers
- `/applications/**` for authenticated customers
- `/quotations/**` for authenticated customers
- `/files/**` for authenticated file owners or admins

Public routes remain open for browsing, signup/login, product detail pages, contact form, chatbot, payment return, and callback routes.

CSRF is enabled for form mutations and JavaScript POST calls.

## File Storage

The app stores file metadata in `stored_files` and stores actual bytes through the configured storage mode:

- `local`: stores files under `LOCAL_UPLOAD_DIR`, used for dev/Docker.
- `supabase`: stores files in a private Supabase Storage bucket, used for production.

Sensitive files such as IC images, signatures, bank-related application documents, and profile pictures should not be served as public static assets. Access them through `GET /files/{id}`.

## Environment

Copy `.env.example` to `.env` for local configuration. Railway should use environment variables directly.

Core variables:

- `SPRING_PROFILES_ACTIVE` - `dev` or `prod`
- `SPRING_DATASOURCE_URL` - Supabase transaction pooler JDBC URL
- `SPRING_DATASOURCE_USERNAME` - usually `postgres.<project-ref>`
- `SPRING_DATASOURCE_PASSWORD` - Supabase database password
- `ADMIN_USERNAME` / `ADMIN_PASSWORD` - admin login
- `APP_BASE_URL` - public app URL for payment return/callback URLs
- `GEMINI_API_KEY` - chatbot key
- `CONTACT_RECIPIENT` - contact form recipient email
- `CONTACT_DELIVERY` - `auto`, `resend`, `smtp`, or `formsubmit`
- `RESEND_API_KEY` - HTTPS email API key when using Resend
- `SPRING_MAIL_HOST` / `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` - SMTP settings
- `FILE_STORAGE_MODE` - `local` or `supabase`
- `LOCAL_UPLOAD_DIR` - local upload folder
- `SUPABASE_URL` / `SUPABASE_SERVICE_ROLE_KEY` / `SUPABASE_STORAGE_BUCKET` - private Supabase Storage settings
- `TOYYIBPAY_MODE` - `mock`, `sandbox`, or `live`
- `TOYYIBPAY_SECRET_KEY` / `TOYYIBPAY_CATEGORY_CODE` / `TOYYIBPAY_BASE_URL` - ToyyibPay settings
- `DEMO_SEED_ENABLED` - optional dev starter data; keep `false` in production

## Supabase Notes

Production uses Supabase PostgreSQL through the transaction pooler on port `6543`.

Use a JDBC URL in this shape:

```text
jdbc:postgresql://<pooler-host>:6543/postgres
```

Use username:

```text
postgres.<project-ref>
```

The transaction pooler does not support server-side prepared statements reliably, so production datasource config disables prepared statement caching/threshold settings.

After Hibernate creates or updates the tables, run `supabase-rls.sql` in Supabase SQL Editor. It enables RLS, creates/locks the private storage bucket, and blocks direct public Data API/Storage access. The Java server remains the trusted access layer.

## Running Locally

Install dependencies and build CSS:

```bash
npm install
npm run build:css
```

Run with Maven:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The app starts on:

```text
http://localhost:8080
```

## Running With Docker

Docker is the easiest path for a new machine because it avoids installing Java, Maven, and Node separately.

```bash
docker compose up --build
```

The included Compose setup is intentionally local-dev only:

- `dev` profile
- H2 in-memory database
- local upload folder
- local ToyyibPay mock
- local admin defaults unless overridden

Useful commands:

```bash
docker compose down
docker compose up --build
docker build -t muqmeen-takaful-web:local .
```

If port `8080` is already used:

```bash
PORT=8081 docker compose up --build
```

## Frontend Build

Tailwind source:

```text
src/main/resources/static/css/input.css
```

Compiled output:

```text
src/main/resources/static/css/app.css
```

Commands:

```bash
npm run build:css
npm run watch:css
```

Run `npm run build:css` after changing template classes or Tailwind input CSS.

## Tests

Run tests through Git Bash on Windows:

```bash
./mvnw test
```

From PowerShell:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' -lc './mvnw test'
```

Current integration coverage includes:

- public product browsing
- admin product CRUD
- customer registration/login/profile update
- application draft/submission/edit-lock behavior
- private file access rules
- admin review and status changes
- quotation builder and local payment mock
- contact inquiry persistence
- admin route protection

## Demo Flow

For a supervisor/client walkthrough:

1. Admin logs in.
2. Admin creates or updates structured products.
3. Public user browses products.
4. Customer registers/logs in.
5. Customer updates profile.
6. Customer starts a product application draft.
7. Customer uploads IC front/back, fills employment/income/bank/measurement details, adds nominees, and signs.
8. Customer submits application.
9. Admin reviews application and private files.
10. Admin requests corrections or proceeds to quotation.
11. Admin creates quotation items and publishes quotation.
12. Customer views quotation and starts mock payment.
13. Payment callback updates quotation/application status.

## Remaining Work

The core dynamic pivot is implemented. Remaining project work is:

1. Production verification on Railway/Supabase:
   - env vars
   - Supabase transaction pooler
   - private storage mode
   - RLS script
   - full end-to-end flow

2. Real ToyyibPay integration:
   - configure sandbox/live credentials
   - confirm category code
   - test bill creation
   - test callback hash/status mapping

3. Final UI polish:
   - dynamic application form usability
   - admin application review layout
   - quotation builder readability
   - mobile polish after the pivot

4. Demo data and presentation readiness:
   - create realistic products through admin CRUD
   - prepare a clean customer/application/quotation walkthrough
   - explain the four use case process flows clearly

## Project Layout

```text
takaful-web-java/
|-- pom.xml
|-- package.json
|-- tailwind.config.js
|-- postcss.config.js
|-- Dockerfile
|-- docker-compose.yml
|-- .env.example
|-- supabase-rls.sql
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
|       |-- static/
|       `-- templates/
`-- src/test/
```

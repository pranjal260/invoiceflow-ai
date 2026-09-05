# InvoiceFlow AI

InvoiceFlow AI is a production-inspired SaaS application that helps freelancers, small businesses, and startups manage invoices, track expenses, and understand financial activity through analytics and AI-assisted document extraction.

## Highlights

- JWT-secured user, category, invoice, and invoice-item workflows
- User-scoped ownership checks across the REST API
- Dashboard and currency-aware monthly, category, and revenue analytics
- Invoice document upload, replacement, deletion, and presigned access
- Gemini-assisted invoice extraction with persisted invoice and line-item data
- PostgreSQL persistence managed with Flyway migrations
- Responsive Next.js web application with loading, error, and empty states

## Technology Stack

| Area | Technologies |
| --- | --- |
| Frontend | Next.js 15, React 19, TypeScript, Tailwind CSS |
| Backend | Spring Boot 3, Java 21, Spring Web, Spring Security |
| Persistence | PostgreSQL, Spring Data JPA, Hibernate, Flyway |
| Integrations | Backblaze B2-compatible object storage, Gemini AI |
| Deployment target | Vercel, Render, Neon PostgreSQL |

## Repository Structure

```text
frontend/   Next.js App Router application
backend/    Spring Boot modular monolith and REST API
docs/       Product, architecture, schema, API, and delivery documentation
.github/    Repository automation and development metadata
```

## Local Development

### Prerequisites

- Node.js 20+
- Java 21
- Maven 3.9+
- A PostgreSQL database (Neon is supported)

### Backend

Configure the environment variables used by `backend/src/main/resources/application.yml`, including `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and the storage/Gemini variables required for document features. Then run:

```bash
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080` by default.

### Frontend

Copy `frontend/.env.example` to `frontend/.env.local`, adjust the API URL if needed, and run:

```bash
cd frontend
npm install
npm run dev
```

The web application listens on `http://localhost:3000` by default.

## Verification

```bash
# Backend
cd backend
mvn test
mvn package -DskipTests

# Frontend
cd frontend
npm run typecheck
npm run lint
npm run build
```

## Documentation

The `docs/` directory contains the [PRD](docs/PRD.md), [architecture](docs/ARCHITECTURE.md), [database schema](docs/DATABASE_SCHEMA.md), [API specification](docs/API_SPEC.md), roadmap, UI guidelines, and coding standards.

## Project Status

**Current phase:** MVP implementation and integration verification.

The core backend and frontend workflows are implemented. Deployment hardening, broader frontend test coverage, and final product polish remain part of the release-readiness work.

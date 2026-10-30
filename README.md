# InvoiceFlow AI

InvoiceFlow AI is a production-inspired SaaS application for freelancers, small businesses, and startups to manage invoices, track expenses, and review financial insights with AI-assisted document extraction.

[![CI](https://github.com/pranjal260/invoiceflow-ai/actions/workflows/ci.yml/badge.svg)](https://github.com/pranjal260/invoiceflow-ai/actions/workflows/ci.yml)

## Why this project

InvoiceFlow AI is designed as a realistic full-stack portfolio project rather than a standalone CRUD demo. It demonstrates a modular Spring Boot backend, a typed Next.js frontend, secure user-scoped data access, PostgreSQL persistence, private document storage, and a human-in-the-loop AI workflow.

## Core capabilities

- JWT authentication with BCrypt password hashing
- User-scoped categories, invoices, and invoice line items
- Invoice CRUD and payment-status updates
- Upload-first workflow: upload → AI extraction → review → confirm
- Gemini-powered extraction of invoice metadata and line items
- Private PDF/image storage with presigned document access
- Currency-aware dashboard and analytics without mixing currencies
- Flyway-managed PostgreSQL schema
- Centralized API validation and error responses
- Responsive Next.js interface with loading, empty, and error states

## Architecture

```text
Browser
  ↓
Next.js 15 + TypeScript + Tailwind CSS
  ↓ HTTPS / REST + JWT
Spring Boot 3 modular monolith
  ↓ Controller → Service → Repository
PostgreSQL + Flyway

Document upload → Object storage
AI extraction → User review → Confirmed invoice data
```

The backend remains intentionally modular without the operational complexity of microservices. AI processing is encapsulated behind a service boundary, and uploaded documents are kept outside PostgreSQL.

## Technology stack

| Area | Technologies |
| --- | --- |
| Frontend | Next.js 15, React 19, TypeScript, Tailwind CSS |
| Backend | Spring Boot 3, Java 21, Spring Web, Spring Security |
| Persistence | PostgreSQL, Spring Data JPA, Hibernate, Flyway |
| AI | Google Gemini API |
| File storage | Backblaze B2-compatible S3 object storage |
| Delivery | Vercel, Render, Neon PostgreSQL |

## Repository structure

```text
frontend/   Next.js App Router application and reusable UI
backend/    Spring Boot modular monolith and REST API
docs/       Product, architecture, schema, API, UI, and coding documentation
.github/    CI workflow and repository automation
```

## Local setup

### Prerequisites

- Node.js 20+
- Java 21
- Maven 3.9+
- PostgreSQL or Neon PostgreSQL
- Gemini API key for AI extraction
- S3-compatible storage credentials for document features

### Backend

```bash
cd backend
copy .env.example .env
```

Set the values in `.env` and export them in your shell. Never commit real credentials. Flyway runs schema migrations and Hibernate validates the mappings at startup.

```bash
mvn spring-boot:run
```

The API runs at `http://localhost:8080` by default.

### Frontend

```bash
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

The web application runs at `http://localhost:3000` by default.

## Verification

```bash
# Frontend
cd frontend
npm run typecheck
npm run lint
npm run build

# Backend
cd backend
mvn test
mvn package -DskipTests
```

The PostgreSQL-backed integration tests use the configured database environment. Gemini and object-storage integration tests require their respective opt-in credentials.

## Documentation

- [Product Requirements Document](docs/PRD.md)
- [Software Architecture](docs/ARCHITECTURE.md)
- [Database Schema](docs/DATABASE_SCHEMA.md)
- [API Specification](docs/API_SPEC.md)
- [Roadmap](docs/ROADMAP.md)
- [UI Guidelines](docs/UI_GUIDELINES.md)
- [Coding Standards](docs/CODING_STANDARDS.md)

## Current status

The MVP backend and frontend workflows are implemented and verified. The project is currently in the presentation and deployment-hardening stage. Future work includes public deployment verification, broader frontend test coverage, and operational safeguards such as rate limiting and abandoned-upload cleanup.

## License

This project is released under the MIT License. See [LICENSE](LICENSE).

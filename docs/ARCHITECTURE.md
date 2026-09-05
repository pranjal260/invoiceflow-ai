# Software Architecture

## 1. Architecture Overview

InvoiceFlow AI follows a **Modular Monolithic Architecture** with a clear separation of concerns. The application is deployed as a single backend service, while each business feature is organized into independent modules. This structure improves maintainability, readability, testability, and the ability to scale the codebase as the product evolves.

A modular monolith is intentionally selected instead of microservices for the MVP because it provides:

- Simpler deployment
- Easier debugging
- Lower operational complexity
- An appropriate level of architecture for an MVP product
- A more manageable development model for small development teams

This approach provides clear internal boundaries without introducing distributed-system complexity before it is justified by product requirements.

### Why this Architecture?

This architecture was selected because it balances simplicity, maintainability, and scalability for a production-inspired MVP. It gives a small development team clear application boundaries and an understandable request flow while avoiding the operational overhead of a distributed system.

## 2. High-Level System Architecture

InvoiceFlow AI uses a web client and REST API architecture. Requests move through the system as follows:

```text
User
  ↓
Next.js Frontend
  ↓
REST API (Spring Boot)
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
PostgreSQL Database
```

```text
                Browser
                    │
                    ▼
           Next.js Frontend
                    │
             HTTPS / REST API
                    │
                    ▼
         Spring Boot Backend
        ┌──────────────────┐
        │ Controller Layer │
        ├──────────────────┤
        │  Service Layer   │
        ├──────────────────┤
        │ Repository Layer │
        └──────────────────┘
                    │
                    ▼
          PostgreSQL Database
```

- **User:** Interacts with the application through the responsive web interface.
- **Next.js Frontend:** Renders the user interface, manages client-side interactions, and sends requests to the backend API.
- **REST API (Spring Boot):** Exposes authenticated REST endpoints and receives application requests.
- **Controller:** Accepts and validates HTTP requests, then delegates work to the service layer.
- **Service:** Applies product rules, validates workflows, processes invoices, and coordinates persistence.
- **Repository:** Provides data-access operations through Spring Data JPA.
- **PostgreSQL Database:** Stores users, invoices, invoice items, categories, and related application data.

## 3. Frontend Architecture

### Technology

- Next.js 15
- React
- TypeScript
- Tailwind CSS
- shadcn/ui

The frontend uses the Next.js **App Router**. The user interface is organized into reusable, composable components, with TypeScript used to maintain clear contracts between views, services, and data models.

Business logic must not live inside UI components. Components are responsible for rendering and user interaction, while API communication is handled through a dedicated service layer. Shared behavior and presentation concerns are separated to keep the frontend understandable and maintainable.

### Suggested Folder Structure

| Folder | Responsibility |
| --- | --- |
| `app/` | Route definitions, page-level layouts, and App Router entry points. |
| `components/` | Reusable UI components and feature-oriented presentation components. |
| `services/` | Dedicated API communication and request-handling layer. |
| `hooks/` | Reusable React hooks for shared client-side behavior. |
| `types/` | Shared TypeScript type definitions and interfaces. |
| `utils/` | General-purpose helper functions. |
| `lib/` | Shared library configuration and application-level utilities. |

## 4. Backend Architecture

### Technology

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate

The backend follows a **Layered Architecture**, separating HTTP concerns, business rules, and database access. This keeps individual classes focused and makes the system easier to test, change, and reason about.

### Request Flow

```text
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

- **Controller:** Defines REST endpoints, accepts and validates HTTP requests, and returns appropriate API responses.
- **Service:** Contains business logic, coordinates workflows, and enforces application rules.
- **Repository:** Provides data-access operations through Spring Data JPA.
- **Database:** Persists and retrieves application data in PostgreSQL.

### Package Structure

| Package | Responsibility |
| --- | --- |
| `config/` | Application configuration. |
| `controller/` | REST controllers and HTTP request handling. |
| `dto/` | Data transfer objects used for API requests and responses. |
| `entity/` | JPA entity models mapped to database records. |
| `exception/` | Custom exceptions and global exception-handling components. |
| `mapper/` | Transformations between entities and DTOs. |
| `repository/` | Spring Data JPA repository interfaces. |
| `security/` | Authentication, authorization, and JWT-related components. |
| `service/` | Business logic and application workflows. |
| `util/` | Shared backend utility classes. |

## 5. Authentication Architecture

InvoiceFlow AI uses a JWT-based, stateless authentication model. The authentication flow is:

```text
Register
  ↓
BCrypt Password Hashing
  ↓
Database
  ↓
Login
  ↓
JWT Generation
  ↓
Protected REST APIs
  ↓
Authorization
```

During registration, passwords are hashed with BCrypt before being persisted. On successful login, the backend generates a JWT that the frontend includes with requests to protected REST APIs. Spring Security validates the token and applies authorization rules before access is granted.

JWT is selected because it supports stateless authentication, fits naturally with a REST API, and avoids server-side session storage. This keeps the backend simpler to deploy and scale for the MVP.

## 6. Invoice Processing Flow

Invoice uploads follow a controlled review workflow:

```text
User Uploads Invoice
  ↓
Validate File
  ↓
Store Uploaded File
  ↓
AI Extracts Invoice Data
  ↓
User Reviews Extracted Data
  ↓
Save Invoice
  ↓
Dashboard Updates
```

The system validates each uploaded file before it is processed and stores the accepted upload. AI-assisted processing extracts relevant invoice information for review. AI-generated information is **never stored automatically**: the user must review and confirm the extracted data before it is persisted as an invoice. After the invoice is saved, dashboard totals and analytics reflect the confirmed record.

## 7. Database Architecture

The core database model consists of the following entities:

- **Users:** Stores account identity, authentication-related data, and profile information.
- **Invoices:** Stores invoice-level financial data, payment status, dates, and ownership.
- **Invoice Items:** Stores individual line items associated with an invoice.
- **Categories:** Stores expense categories used to organize invoice and expense information.

At a high level, a user owns their invoices. An invoice can contain multiple invoice items. Categories are used to classify relevant financial records and support category-based analytics. The detailed schema, fields, and constraints are documented separately in `DATABASE_SCHEMA.md`.

## 8. Deployment Architecture

The planned deployment flow is:

```text
Frontend
  ↓
Vercel
  ↓
Backend
  ↓
Render
  ↓
PostgreSQL (Neon)
```

- **Vercel:** Hosts and delivers the Next.js frontend.
- **Render:** Hosts the Spring Boot backend service.
- **Neon:** Provides the managed PostgreSQL database.

Managed cloud services are selected to reduce infrastructure administration, provide a straightforward deployment path, and let the project focus on product quality and application engineering. This is appropriate for a portfolio-quality SaaS application and its MVP scope.

## 9. Security Architecture

Security is applied throughout the application architecture:

- **Spring Security:** Secures application endpoints and enforces authentication and authorization rules.
- **JWT:** Provides stateless authentication for protected API requests.
- **BCrypt:** Hashes passwords before storage to protect user credentials.
- **Input Validation:** Validates incoming request data and file uploads before processing.
- **Authorization:** Ensures users can access only the resources they are permitted to access.
- **Global Exception Handling:** Returns consistent, controlled error responses without exposing internal implementation details.
- **CORS:** Restricts browser-based API access to permitted frontend origins.
- **Parameterized database queries through JPA:** Uses JPA data-access patterns to avoid constructing database queries from untrusted input.

## 10. Scalability Considerations

The modular monolith is designed to evolve without premature complexity. As product needs grow, the architecture can support:

- Additional internal modules for new product capabilities
- Cloud storage for uploaded invoice documents
- AI processing improvements
- Caching for frequently accessed dashboard and analytics data
- Horizontal backend scaling through additional service instances

If business requirements grow significantly, further architectural options may be evaluated, including more distributed approaches. These are not required for the MVP and should be introduced only when justified by clear product and operational needs.

## 11. Design Principles

- **Separation of Concerns:** Each layer and module has a focused responsibility.
- **Clean Code:** Names, structure, and implementation should favor clarity and straightforward maintenance.
- **Reusability:** Shared components, services, and utilities should be designed for consistent reuse.
- **Maintainability:** The architecture should make changes, testing, and onboarding easier.
- **Modularity:** Features should have clear boundaries within the single backend service.
- **Security by Design:** Authentication, authorization, validation, and safe data access are incorporated into core workflows.
- **Scalability:** The application should accommodate growth while retaining an appropriate level of complexity for the project stage.

## 12. Architecture Summary

InvoiceFlow AI uses a modular monolith and layered application design to prioritize maintainability, clarity, scalability, and professional engineering practices. The architecture provides clear frontend and backend boundaries, secure stateless authentication, controlled invoice processing, and managed deployment services while remaining intentionally appropriate for a portfolio-quality SaaS application.

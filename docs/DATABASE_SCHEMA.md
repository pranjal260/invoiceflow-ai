# Database Schema

## 1. Overview

InvoiceFlow AI uses PostgreSQL as its relational database. The schema is normalized to reduce redundancy, maintain data integrity, and support future scalability while remaining appropriate for the MVP.

The database design focuses on the core invoice-management workflow: user accounts, invoice categorization, invoice records, and invoice line items.

## 2. Design Goals

The schema is designed to be:

- **Simple:** Focused on the MVP domain and free of unnecessary tables.
- **Maintainable:** Organized with clear relationships and responsibilities.
- **Scalable:** Structured to support future product growth without redesigning the core model.
- **Easy to understand:** Readable for developers, reviewers, and contributors.
- **Suitable for a production-inspired SaaS application:** Designed with data integrity and practical implementation in mind.

## 3. Core Tables

### users

**Purpose:** Stores user account information and profile details.

| Field | Purpose |
| --- | --- |
| `id` | Unique identifier for the user. |
| `first_name` | User's given name. |
| `last_name` | User's family name. |
| `company_name` | Optional business or company name associated with the account. |
| `email` | Unique email address used for account identification and authentication. |
| `password_hash` | Securely hashed user password. |
| `phone_number` | Optional contact phone number. |
| `country` | User's country for profile and business context. |
| `created_at` | Timestamp indicating when the account was created. |
| `updated_at` | Timestamp indicating when the account was last updated. |

### categories

**Purpose:** Stores user-owned invoice categories used for organization and analytics.

| Field | Purpose |
| --- | --- |
| `id` | Unique identifier for the category. |
| `user_id` | Reference to the user who owns the category. |
| `name` | Category name used to classify invoices. |
| `description` | Optional explanation of the category's intended use. |
| `created_at` | Timestamp indicating when the category was created. |

Example categories include:

- Software
- Marketing
- Office
- Travel
- Utilities
- Other

### invoices

**Purpose:** Stores invoice metadata, payment information, AI-confirmed extracted data, and the uploaded document reference.

| Field | Purpose |
| --- | --- |
| `id` | Unique identifier for the invoice. |
| `user_id` | Reference to the user who owns the invoice. |
| `category_id` | Reference to the category used to classify the invoice. |
| `invoice_number` | Invoice identifier provided by the vendor or entered by the user. |
| `vendor_name` | Name of the vendor associated with the invoice. |
| `invoice_date` | Date on which the invoice was issued. |
| `due_date` | Date by which the invoice is due for payment. |
| `status` | Invoice payment status, represented by the enum values `PENDING`, `PAID`, or `OVERDUE`. |
| `direction` | Financial direction, represented by the enum values `INCOME` or `EXPENSE`, used to separate revenue and expense reporting. |
| `subtotal` | Precise decimal monetary value representing the invoice amount before tax. |
| `tax_amount` | Precise decimal monetary value representing the tax recorded for the invoice. |
| `total_amount` | Precise decimal monetary value representing the final invoice amount, including applicable tax. |
| `currency` | ISO currency code, such as `INR`, `USD`, `EUR`, or `GBP`. |
| `notes` | Optional user-provided notes about the invoice. |
| `file_url` | Reference to the uploaded invoice document stored outside the database. |
| `created_at` | Timestamp indicating when the invoice record was created. |
| `updated_at` | Timestamp indicating when the invoice record was last updated. |

The `direction` field was introduced after the initial MVP schema to support accurate dashboard and analytics calculations. Existing invoice records are classified as `EXPENSE` by the migration because the original MVP workflow focused on vendor and expense invoice management. New invoice creation and update requests must explicitly provide either `INCOME` or `EXPENSE`.

### invoice_items

**Purpose:** Stores individual line items belonging to an invoice.

| Field | Purpose |
| --- | --- |
| `id` | Unique identifier for the invoice item. |
| `invoice_id` | Reference to the invoice that owns the line item. |
| `description` | Description of the purchased product or service. |
| `quantity` | Number of units included in the line item. |
| `unit_price` | Precise decimal monetary value for one unit of the item. |
| `amount` | Precise decimal monetary value for the total line-item value. |

One invoice can contain multiple invoice items.

## 4. Entity Relationships

- One **User** can have many **Categories**.
- One **User** can have many **Invoices**.
- One **Category** can be associated with many **Invoices** owned by the same user.
- One **Invoice** can contain many **Invoice Items**.

```text
User (1)
  │
  ├────< Categories
  │
  └────< Invoices
            │
            └────< Invoice Items
```

## 5. Normalization

The schema follows normalization principles to:

- Reduce redundancy
- Improve consistency
- Simplify maintenance
- Preserve data integrity

User, category, invoice, and invoice-item data are separated according to their responsibilities. Categories are owned by their users, preventing category definitions from being shared unintentionally across accounts. This avoids duplicating shared information across invoice records and keeps updates more reliable.

## 6. Storage Strategy

Uploaded invoice documents are **not** stored inside PostgreSQL. Files are stored using cloud object storage, while PostgreSQL stores only the corresponding `file_url`.

This approach is preferred because relational databases are optimized for structured application data rather than document storage. Keeping files outside the database reduces database size, supports efficient file delivery, and keeps the core data model focused on invoice metadata and relationships.

## 7. AI Data Handling

AI-generated information is not stored separately. The workflow is:

```text
Invoice Upload
  ↓
AI Extraction
  ↓
User Review
  ↓
Confirmed Invoice Data Saved
```

Only user-approved data is persisted. This ensures that invoice records reflect reviewed information rather than unverified AI output.

## 8. Future Expansion

Future versions may introduce:

- Organizations
- Team Members
- Payments
- Notifications

These capabilities are intentionally excluded from the MVP to keep the database focused, maintainable, and aligned with the initial product scope.

## 9. Database Summary

The InvoiceFlow AI database schema is designed for clarity, maintainability, scalability, and ease of implementation. It provides a normalized relational foundation for the MVP invoice workflow while remaining appropriately focused for a portfolio-quality SaaS application.

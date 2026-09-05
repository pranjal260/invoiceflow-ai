# API Specification

## 1. Overview

InvoiceFlow AI exposes a REST API consumed by the Next.js frontend. The API provides the MVP capabilities for authentication, user profiles, categories, invoices, AI-assisted extraction, dashboard summaries, and analytics.

All protected endpoints require JWT authentication. JSON is used for request and response payloads.

## 2. Authentication APIs

| Endpoint | Purpose |
| --- | --- |
| `POST /api/auth/register` | Creates a new user account. |
| `POST /api/auth/login` | Authenticates a user and returns a JWT for subsequent protected requests. |
| `GET /api/auth/me` | Returns the authenticated user's account information. |
| `PUT /api/auth/change-password` | Updates the authenticated user's password after validating the request. |

## 3. User APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/users/profile` | Returns the authenticated user's profile details. |
| `PUT /api/users/profile` | Updates the authenticated user's profile details. |

## 4. Category APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/categories` | Returns the categories owned by the authenticated user. |
| `POST /api/categories` | Creates a category for the authenticated user. |
| `PUT /api/categories/{id}` | Updates an existing category owned by the authenticated user. |
| `DELETE /api/categories/{id}` | Deletes an existing category owned by the authenticated user. |

## 5. Invoice APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/invoices` | Returns invoices owned by the authenticated user. The current frontend provides local search and filtering over this collection. |
| `GET /api/invoices/{id}` | Returns a specific invoice owned by the authenticated user. |
| `POST /api/invoices` | Creates an invoice for the authenticated user. |
| `PUT /api/invoices/{id}` | Updates a specific invoice owned by the authenticated user. |
| `DELETE /api/invoices/{id}` | Deletes a specific invoice owned by the authenticated user. |
| `PATCH /api/invoices/{id}/status` | Updates the payment status of an owned invoice. |

Authenticated users can access only their own invoices. Ownership checks are required for every invoice operation.

## 6. Invoice Item APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/invoices/{invoiceId}/items` | Lists line items for an owned invoice. |
| `GET /api/invoices/{invoiceId}/items/{itemId}` | Returns one line item belonging to an owned invoice. |
| `POST /api/invoices/{invoiceId}/items` | Adds a line item to an owned invoice. |
| `PUT /api/invoices/{invoiceId}/items/{itemId}` | Updates a line item belonging to an owned invoice. |
| `DELETE /api/invoices/{invoiceId}/items/{itemId}` | Deletes a line item belonging to an owned invoice. |

Invoice-item operations verify both invoice ownership and item-to-invoice membership.

## 7. Invoice Document and AI APIs

| Endpoint | Purpose |
| --- | --- |
| `POST /api/invoices/{id}/document` | Uploads or replaces the document attached to an existing invoice. |
| `GET /api/invoices/{id}/document` | Returns a short-lived presigned URL for the authenticated user to view the invoice document. |
| `DELETE /api/invoices/{id}/document` | Removes the document attached to an existing invoice. |
| `POST /api/invoices/{id}/extract` | Runs internal AI extraction against the attached document and updates the invoice and line items with the extracted result. |

AI processing is an internal backend service rather than a separate public AI API. Extraction is available only for an existing, owned invoice with an uploaded document; the backend persists the returned invoice fields and line items as part of that extraction operation.

## 8. Dashboard API

| Endpoint | Purpose |
| --- | --- |
| `GET /api/dashboard/summary` | Returns a financial overview for the authenticated user. |

The dashboard summary returns:

- Revenue
- Expenses
- Paid invoices
- Pending invoices
- Invoice count

## 9. Analytics APIs

| Endpoint | Purpose |
| --- | --- |
| `GET /api/analytics/monthly` | Returns monthly financial reporting data for the authenticated user. |
| `GET /api/analytics/categories` | Returns category-based financial breakdown data for the authenticated user. |
| `GET /api/analytics/revenue` | Returns revenue trend data for the authenticated user. |

## 10. Response Standards

The API returns appropriate HTTP status codes, JSON responses, and consistent success and error formats.

| Status Code | Meaning |
| --- | --- |
| `200` | Request completed successfully. |
| `201` | A resource was created successfully. |
| `400` | The request is invalid or fails validation. |
| `401` | Authentication is required or the JWT is invalid. |
| `403` | The authenticated user is not authorized to access the resource. |
| `404` | The requested resource does not exist. |
| `500` | An unexpected server error occurred. |

## 11. Security

- **JWT Authentication:** Protected endpoints require a valid JWT.
- **Authorization:** The API verifies resource ownership so users can access only their own data.
- **Input Validation:** Requests are validated before business logic or persistence operations are performed.
- **Secure API Design:** The API returns controlled error responses and applies access restrictions consistently across protected resources.

## 12. Summary

The InvoiceFlow AI API is intentionally simple, RESTful, and maintainable. It defines a focused contract for the MVP while supporting secure, user-scoped financial workflows appropriate for a production-inspired portfolio project.

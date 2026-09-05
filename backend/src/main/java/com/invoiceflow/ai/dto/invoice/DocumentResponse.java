package com.invoiceflow.ai.dto.invoice;

import java.util.UUID;

/**
 * Response for the document access endpoint.
 *
 * Contains a time-limited presigned URL for downloading the invoice document.
 * This URL is generated on demand and must never be persisted.
 *
 * @param invoiceId the invoice this document belongs to
 * @param url       a time-limited presigned URL for private file access
 */
public record DocumentResponse(UUID invoiceId, String url) {
}

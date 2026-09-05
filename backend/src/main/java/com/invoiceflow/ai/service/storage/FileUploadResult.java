package com.invoiceflow.ai.service.storage;

/**
 * Result of a successful file upload.
 *
 * Contains only the permanent object key — never a temporary presigned URL.
 * The object key is what gets persisted (e.g., in Invoice.fileUrl).
 * Temporary access URLs are generated on demand via
 * {@link FileStorageService#generatePresignedUrl(String)}.
 *
 * @param objectKey the permanent storage key (e.g., invoices/{userId}/{uuid}.pdf)
 */
public record FileUploadResult(String objectKey) {
}

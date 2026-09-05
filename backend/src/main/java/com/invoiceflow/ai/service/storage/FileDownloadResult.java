package com.invoiceflow.ai.service.storage;

/**
 * Result of a successful file download from storage.
 *
 * Contains the raw file content and the MIME type as stored during upload.
 *
 * @param content     raw file bytes
 * @param contentType MIME type (e.g., application/pdf, image/jpeg, image/png)
 */
public record FileDownloadResult(byte[] content, String contentType) {
}

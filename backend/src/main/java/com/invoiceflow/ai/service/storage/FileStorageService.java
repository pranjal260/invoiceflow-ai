package com.invoiceflow.ai.service.storage;

import java.util.UUID;

/**
 * Provider-agnostic file storage abstraction.
 *
 * Implementations handle the actual interaction with an external storage provider
 * (e.g., Backblaze B2, Cloudflare R2, AWS S3).
 *
 * Business logic depends on this interface, never on provider-specific SDK classes.
 */
public interface FileStorageService {

    /**
     * Uploads file bytes to storage under a server-generated object key.
     *
     * @param fileBytes   validated file content (non-empty, within size limit)
     * @param contentType validated MIME type (application/pdf, image/jpeg, image/png)
     * @param userId      authenticated user ID for key namespacing
     * @return result containing the permanent object key
     */
    FileUploadResult upload(byte[] fileBytes, String contentType, UUID userId);

    /**
     * Generates a time-limited presigned URL for private file access.
     *
     * @param objectKey the permanent storage key (e.g., invoices/{userId}/{uuid}.pdf)
     * @return a temporary URL that grants read access for a limited duration
     */
    String generatePresignedUrl(String objectKey);

    /**
     * Deletes a file from storage.
     *
     * @param objectKey the permanent storage key to delete
     */
    void delete(String objectKey);

    /**
     * Downloads file content from storage.
     *
     * @param objectKey the permanent storage key (e.g., invoices/{userId}/{uuid}.pdf)
     * @return result containing the file bytes and content type
     */
    FileDownloadResult download(String objectKey);
}

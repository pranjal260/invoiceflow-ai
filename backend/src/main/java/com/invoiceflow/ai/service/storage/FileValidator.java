package com.invoiceflow.ai.service.storage;

import com.invoiceflow.ai.exception.EmptyFileException;
import com.invoiceflow.ai.exception.FileSizeLimitExceededException;
import com.invoiceflow.ai.exception.UnsupportedFileTypeException;
import java.util.Set;

/**
 * Validates uploaded files before any storage attempt.
 *
 * Checks content type, file size, and emptiness. Validation runs entirely
 * in-process — no external calls are made.
 */
public class FileValidator {

    /** Maximum file size: 10 MB. */
    static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    /**
     * Validates that the file meets all upload requirements.
     *
     * @param fileBytes   the raw file content
     * @param contentType the MIME type reported for the file
     * @throws EmptyFileException             if fileBytes is null or empty
     * @throws UnsupportedFileTypeException   if contentType is not PDF, JPEG, or PNG
     * @throws FileSizeLimitExceededException if fileBytes exceeds 10 MB
     */
    public void validate(byte[] fileBytes, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new EmptyFileException();
        }

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedFileTypeException(contentType);
        }

        if (fileBytes.length > MAX_FILE_SIZE) {
            throw new FileSizeLimitExceededException(fileBytes.length, MAX_FILE_SIZE);
        }
    }

    /**
     * Derives a safe file extension from a validated MIME content type.
     *
     * @param contentType a validated MIME type (must be one of the allowed types)
     * @return the file extension without a leading dot (e.g., "pdf", "jpg", "png")
     */
    public String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "application/pdf" -> "pdf";
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            default -> throw new UnsupportedFileTypeException(contentType);
        };
    }
}

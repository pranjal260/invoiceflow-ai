package com.invoiceflow.ai.exception;

/**
 * Thrown when AI-powered invoice extraction fails.
 *
 * Wraps all provider-specific errors (HTTP failures, parsing errors, API errors)
 * into a safe application-level exception. The message never includes API keys,
 * raw provider responses, or internal implementation details.
 */
public class InvoiceExtractionException extends RuntimeException {

    public InvoiceExtractionException(String message) {
        super(message);
    }

    public InvoiceExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

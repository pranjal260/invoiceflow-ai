package com.invoiceflow.ai.service.ai;

/**
 * Provider-agnostic invoice document extraction abstraction.
 *
 * Implementations handle the actual interaction with an AI provider
 * (e.g., Google Gemini, OpenAI, Anthropic).
 *
 * Business logic depends on this interface, never on provider-specific classes.
 */
public interface InvoiceExtractionService {

    /**
     * Extracts structured invoice data from a document.
     *
     * @param documentBytes raw document content (PDF, JPEG, PNG)
     * @param contentType   MIME type of the document
     * @return structured extraction result
     * @throws com.invoiceflow.ai.exception.InvoiceExtractionException if extraction fails
     */
    InvoiceExtractionResult extract(byte[] documentBytes, String contentType);
}

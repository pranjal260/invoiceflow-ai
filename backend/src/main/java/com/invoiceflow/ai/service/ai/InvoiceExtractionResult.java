package com.invoiceflow.ai.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Structured result of AI invoice extraction.
 *
 * This is an AI-layer result object only — it is NOT mapped to JPA entities
 * and is NOT persisted directly. The downstream consumer is responsible for
 * validating and mapping these values to the Invoice domain.
 *
 * Fields may be null when the AI provider cannot determine a value from the document.
 */
public record InvoiceExtractionResult(
        String invoiceNumber,
        String vendorName,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String currency,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        List<LineItem> items
) {
    /**
     * A single line item extracted from the invoice.
     */
    public record LineItem(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount
    ) {
    }
}

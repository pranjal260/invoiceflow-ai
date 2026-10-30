package com.invoiceflow.ai.dto.invoice;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AI-extracted invoice data awaiting user review. This response is not persisted
 * until the client confirms it through the normal invoice create endpoint.
 */
public record InvoiceUploadPreviewResponse(
        String invoiceNumber,
        String vendorName,
        LocalDate invoiceDate,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String currency,
        List<InvoiceItemPreview> items,
        String fileUrl
) {
    public record InvoiceItemPreview(
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal amount
    ) { }
}

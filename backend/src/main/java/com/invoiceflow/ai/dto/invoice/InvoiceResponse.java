package com.invoiceflow.ai.dto.invoice;

import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.InvoiceDirection;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(UUID id, UUID userId, UUID categoryId, String invoiceNumber, String vendorName,
                              LocalDate invoiceDate, LocalDate dueDate, InvoiceStatus status, BigDecimal subtotal,
                              BigDecimal taxAmount, BigDecimal totalAmount, String currency, String notes, String fileUrl,
                              Instant createdAt, Instant updatedAt, InvoiceDirection direction) {
    public InvoiceResponse(UUID id, UUID userId, UUID categoryId, String invoiceNumber, String vendorName,
                           LocalDate invoiceDate, LocalDate dueDate, InvoiceStatus status, BigDecimal subtotal,
                           BigDecimal taxAmount, BigDecimal totalAmount, String currency, String notes, String fileUrl,
                           Instant createdAt, Instant updatedAt) {
        this(id, userId, categoryId, invoiceNumber, vendorName, invoiceDate, dueDate, status, subtotal, taxAmount,
                totalAmount, currency, notes, fileUrl, createdAt, updatedAt, InvoiceDirection.EXPENSE);
    }
}

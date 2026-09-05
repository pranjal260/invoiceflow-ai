package com.invoiceflow.ai.dto.invoice;

import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.InvoiceDirection;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceUpdateRequest(
        UUID categoryId, @NotBlank @Size(max = 100) String invoiceNumber,
        @NotBlank @Size(max = 150) String vendorName, @NotNull LocalDate invoiceDate, LocalDate dueDate,
        @NotNull InvoiceStatus status, @NotNull @Digits(integer = 17, fraction = 2) BigDecimal subtotal,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal taxAmount,
        @NotNull @Digits(integer = 17, fraction = 2) BigDecimal totalAmount,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency, @Size(max = 2_000) String notes,
        @Size(max = 2_048) String fileUrl, @NotNull InvoiceDirection direction
) {
    public InvoiceUpdateRequest(UUID categoryId, String invoiceNumber, String vendorName, LocalDate invoiceDate,
                                LocalDate dueDate, InvoiceStatus status, BigDecimal subtotal, BigDecimal taxAmount,
                                BigDecimal totalAmount, String currency, String notes, String fileUrl) {
        this(categoryId, invoiceNumber, vendorName, invoiceDate, dueDate, status, subtotal, taxAmount, totalAmount,
                currency, notes, fileUrl, InvoiceDirection.EXPENSE);
    }
}

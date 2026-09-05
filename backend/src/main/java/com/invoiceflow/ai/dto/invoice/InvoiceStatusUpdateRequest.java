package com.invoiceflow.ai.dto.invoice;

import com.invoiceflow.ai.entity.InvoiceStatus;
import jakarta.validation.constraints.NotNull;

public record InvoiceStatusUpdateRequest(@NotNull InvoiceStatus status) {
}

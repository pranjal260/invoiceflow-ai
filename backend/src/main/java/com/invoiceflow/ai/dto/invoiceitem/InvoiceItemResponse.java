package com.invoiceflow.ai.dto.invoiceitem;

import java.math.BigDecimal;
import java.util.UUID;

public record InvoiceItemResponse(UUID id, UUID invoiceId, String description, BigDecimal quantity,
                                  BigDecimal unitPrice, BigDecimal amount) {
}

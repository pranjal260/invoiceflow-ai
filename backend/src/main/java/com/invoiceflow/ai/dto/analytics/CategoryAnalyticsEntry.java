package com.invoiceflow.ai.dto.analytics;

import com.invoiceflow.ai.entity.InvoiceDirection;
import java.math.BigDecimal;
import java.util.UUID;

public record CategoryAnalyticsEntry(UUID categoryId, String categoryName, InvoiceDirection direction,
                                     String currency, BigDecimal totalAmount, long invoiceCount) {
}

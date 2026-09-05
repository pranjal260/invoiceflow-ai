package com.invoiceflow.ai.dto.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RevenueAnalyticsEntry(YearMonth month, String currency, BigDecimal revenue, long invoiceCount) {
}

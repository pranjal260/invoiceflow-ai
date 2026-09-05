package com.invoiceflow.ai.dto.analytics;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyAnalyticsEntry(YearMonth month, String currency, BigDecimal revenue, BigDecimal expenses,
                                    long invoiceCount) {
}

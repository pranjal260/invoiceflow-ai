package com.invoiceflow.ai.dto.dashboard;

import java.math.BigDecimal;

public record DashboardCurrencySummary(String currency, BigDecimal revenue, BigDecimal expenses,
                                       long paidInvoices, long pendingInvoices, long invoiceCount) {
}

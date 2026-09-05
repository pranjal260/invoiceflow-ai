package com.invoiceflow.ai.dto.dashboard;

import java.util.List;

public record DashboardSummaryResponse(List<DashboardCurrencySummary> summaries) {
}

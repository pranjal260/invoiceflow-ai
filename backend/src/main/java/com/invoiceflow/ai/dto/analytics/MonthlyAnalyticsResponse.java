package com.invoiceflow.ai.dto.analytics;

import java.util.List;

public record MonthlyAnalyticsResponse(List<MonthlyAnalyticsEntry> months) {
}

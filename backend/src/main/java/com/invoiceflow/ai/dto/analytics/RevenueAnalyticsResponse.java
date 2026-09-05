package com.invoiceflow.ai.dto.analytics;

import java.util.List;

public record RevenueAnalyticsResponse(List<RevenueAnalyticsEntry> trends) {
}

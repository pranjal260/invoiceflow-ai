import { apiClient } from "@/services/api-client";
import type { CategoryAnalyticsResponse, MonthlyAnalyticsResponse, RevenueAnalyticsResponse } from "@/types/api";

export const analyticsService = {
  monthly: () => apiClient.get<MonthlyAnalyticsResponse>("/api/analytics/monthly"),
  categories: () => apiClient.get<CategoryAnalyticsResponse>("/api/analytics/categories"),
  revenue: () => apiClient.get<RevenueAnalyticsResponse>("/api/analytics/revenue"),
};

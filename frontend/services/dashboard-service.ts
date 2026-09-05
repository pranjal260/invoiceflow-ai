import { apiClient } from "@/services/api-client";
import type { DashboardSummaryResponse } from "@/types/api";

export const dashboardService = {
  getSummary: () => apiClient.get<DashboardSummaryResponse>("/api/dashboard/summary"),
};

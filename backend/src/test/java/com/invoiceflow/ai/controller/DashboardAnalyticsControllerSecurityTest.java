package com.invoiceflow.ai.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceflow.ai.dto.analytics.CategoryAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.MonthlyAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.RevenueAnalyticsResponse;
import com.invoiceflow.ai.dto.dashboard.DashboardSummaryResponse;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.AnalyticsService;
import com.invoiceflow.ai.service.DashboardService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardAnalyticsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void dashboardAndAnalyticsEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analytics/monthly")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analytics/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/analytics/revenue")).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedIdentityScopesDashboardAndAnalyticsRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        String authorization = "Bearer " + jwtService.generateToken(userId);
        when(dashboardService.getSummary(userId)).thenReturn(new DashboardSummaryResponse(List.of()));
        when(analyticsService.getMonthlyAnalytics(userId)).thenReturn(new MonthlyAnalyticsResponse(List.of()));
        when(analyticsService.getCategoryAnalytics(userId)).thenReturn(new CategoryAnalyticsResponse(List.of()));
        when(analyticsService.getRevenueAnalytics(userId)).thenReturn(new RevenueAnalyticsResponse(List.of()));

        mockMvc.perform(get("/api/dashboard/summary").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", authorization)).andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/monthly").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", authorization)).andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/categories").header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/revenue").header("Authorization", authorization))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary(userId);
        verify(analyticsService).getMonthlyAnalytics(userId);
        verify(analyticsService).getCategoryAnalytics(userId);
        verify(analyticsService).getRevenueAnalytics(userId);
    }
}

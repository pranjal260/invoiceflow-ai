package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.analytics.CategoryAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.MonthlyAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.RevenueAnalyticsResponse;
import com.invoiceflow.ai.service.AnalyticsService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/monthly")
    public MonthlyAnalyticsResponse getMonthlyAnalytics(@AuthenticationPrincipal UUID userId) {
        return analyticsService.getMonthlyAnalytics(userId);
    }

    @GetMapping("/categories")
    public CategoryAnalyticsResponse getCategoryAnalytics(@AuthenticationPrincipal UUID userId) {
        return analyticsService.getCategoryAnalytics(userId);
    }

    @GetMapping("/revenue")
    public RevenueAnalyticsResponse getRevenueAnalytics(@AuthenticationPrincipal UUID userId) {
        return analyticsService.getRevenueAnalytics(userId);
    }
}

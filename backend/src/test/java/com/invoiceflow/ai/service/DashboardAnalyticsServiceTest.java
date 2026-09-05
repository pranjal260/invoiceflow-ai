package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.analytics.CategoryAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.MonthlyAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.RevenueAnalyticsResponse;
import com.invoiceflow.ai.dto.dashboard.DashboardSummaryResponse;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceDirection;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private DashboardService dashboardService;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(invoiceRepository);
        analyticsService = new AnalyticsService(invoiceRepository);
    }

    @Test
    void dashboardSeparatesIncomeExpensesCurrenciesAndUsers() {
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findAllByUserId(userId)).thenReturn(List.of(
                invoice("IN-USD", LocalDate.of(2026, 1, 10), "USD", InvoiceDirection.INCOME, "100.00", null),
                invoice("EX-USD", LocalDate.of(2026, 1, 11), "USD", InvoiceDirection.EXPENSE, "25.00", null),
                invoice("EX-INR", LocalDate.of(2026, 1, 12), "INR", InvoiceDirection.EXPENSE, "500.00", null)
        ));

        DashboardSummaryResponse response = dashboardService.getSummary(userId);

        assertThat(response.summaries()).hasSize(2);
        assertThat(response.summaries()).anySatisfy(summary -> {
            assertThat(summary.currency()).isEqualTo("USD");
            assertThat(summary.revenue()).isEqualByComparingTo("100.00");
            assertThat(summary.expenses()).isEqualByComparingTo("25.00");
            assertThat(summary.invoiceCount()).isEqualTo(2);
        });
        assertThat(response.summaries()).anySatisfy(summary -> {
            assertThat(summary.currency()).isEqualTo("INR");
            assertThat(summary.revenue()).isEqualByComparingTo("0");
            assertThat(summary.expenses()).isEqualByComparingTo("500.00");
        });
        verify(invoiceRepository).findAllByUserId(userId);
    }

    @Test
    void analyticsSeparatesMonthlyCategoryRevenueAndCurrenciesForUser() {
        UUID userId = UUID.randomUUID();
        Category software = new Category(user(), "Software", "Subscriptions");
        when(invoiceRepository.findAllByUserId(userId)).thenReturn(List.of(
                invoice("IN-USD", LocalDate.of(2026, 1, 10), "USD", InvoiceDirection.INCOME, "100.00", null),
                invoice("EX-USD", LocalDate.of(2026, 1, 11), "USD", InvoiceDirection.EXPENSE, "25.00", software),
                invoice("IN-INR", LocalDate.of(2026, 2, 1), "INR", InvoiceDirection.INCOME, "500.00", null)
        ));

        MonthlyAnalyticsResponse monthly = analyticsService.getMonthlyAnalytics(userId);
        CategoryAnalyticsResponse categories = analyticsService.getCategoryAnalytics(userId);
        RevenueAnalyticsResponse revenue = analyticsService.getRevenueAnalytics(userId);

        assertThat(monthly.months()).anySatisfy(entry -> {
            assertThat(entry.month().toString()).isEqualTo("2026-01");
            assertThat(entry.currency()).isEqualTo("USD");
            assertThat(entry.revenue()).isEqualByComparingTo("100.00");
            assertThat(entry.expenses()).isEqualByComparingTo("25.00");
        });
        assertThat(categories.categories()).anySatisfy(entry -> {
            assertThat(entry.categoryName()).isEqualTo("Software");
            assertThat(entry.direction()).isEqualTo(InvoiceDirection.EXPENSE);
            assertThat(entry.currency()).isEqualTo("USD");
            assertThat(entry.totalAmount()).isEqualByComparingTo("25.00");
        });
        assertThat(revenue.trends()).extracting(entry -> entry.currency() + ":" + entry.revenue())
                .containsExactlyInAnyOrder("USD:100.00", "INR:500.00");
        verify(invoiceRepository, org.mockito.Mockito.times(3)).findAllByUserId(userId);
    }

    private static Invoice invoice(String number, LocalDate date, String currency, InvoiceDirection direction,
                                   String amount, Category category) {
        return new Invoice(user(), category, number, "Example Vendor", date, null, InvoiceStatus.PAID, direction,
                new BigDecimal(amount), BigDecimal.ZERO, new BigDecimal(amount), currency, null, null);
    }

    private static User user() {
        return new User("Ada", "Lovelace", null, "ada@example.com", "password-hash", null, null);
    }
}

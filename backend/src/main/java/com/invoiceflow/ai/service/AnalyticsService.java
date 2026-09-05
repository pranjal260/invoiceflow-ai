package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.analytics.CategoryAnalyticsEntry;
import com.invoiceflow.ai.dto.analytics.CategoryAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.MonthlyAnalyticsEntry;
import com.invoiceflow.ai.dto.analytics.MonthlyAnalyticsResponse;
import com.invoiceflow.ai.dto.analytics.RevenueAnalyticsEntry;
import com.invoiceflow.ai.dto.analytics.RevenueAnalyticsResponse;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceDirection;
import com.invoiceflow.ai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final InvoiceRepository invoiceRepository;

    public AnalyticsService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlyAnalytics(UUID userId) {
        Map<MonthlyKey, List<Invoice>> grouped = invoicesForUser(userId).stream()
                .collect(Collectors.groupingBy(invoice -> new MonthlyKey(YearMonth.from(invoice.getInvoiceDate()),
                        invoice.getCurrency())));
        List<MonthlyAnalyticsEntry> months = grouped.entrySet().stream()
                .map(entry -> new MonthlyAnalyticsEntry(entry.getKey().month(), entry.getKey().currency(),
                        totalForDirection(entry.getValue(), InvoiceDirection.INCOME),
                        totalForDirection(entry.getValue(), InvoiceDirection.EXPENSE), entry.getValue().size()))
                .sorted(Comparator.comparing(MonthlyAnalyticsEntry::month)
                        .thenComparing(MonthlyAnalyticsEntry::currency))
                .toList();
        return new MonthlyAnalyticsResponse(months);
    }

    @Transactional(readOnly = true)
    public CategoryAnalyticsResponse getCategoryAnalytics(UUID userId) {
        Map<CategoryKey, List<Invoice>> grouped = invoicesForUser(userId).stream()
                .collect(Collectors.groupingBy(invoice -> new CategoryKey(
                        invoice.getCategory() == null ? null : invoice.getCategory().getId(),
                        invoice.getCategory() == null ? "Uncategorized" : invoice.getCategory().getName(),
                        invoice.getDirection(), invoice.getCurrency())));
        List<CategoryAnalyticsEntry> categories = grouped.entrySet().stream()
                .map(entry -> new CategoryAnalyticsEntry(entry.getKey().categoryId(), entry.getKey().categoryName(),
                        entry.getKey().direction(), entry.getKey().currency(), total(entry.getValue()), entry.getValue().size()))
                .sorted(Comparator.comparing(CategoryAnalyticsEntry::categoryName)
                        .thenComparing(CategoryAnalyticsEntry::direction)
                        .thenComparing(CategoryAnalyticsEntry::currency))
                .toList();
        return new CategoryAnalyticsResponse(categories);
    }

    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse getRevenueAnalytics(UUID userId) {
        Map<MonthlyKey, List<Invoice>> grouped = invoicesForUser(userId).stream()
                .filter(invoice -> invoice.getDirection() == InvoiceDirection.INCOME)
                .collect(Collectors.groupingBy(invoice -> new MonthlyKey(YearMonth.from(invoice.getInvoiceDate()),
                        invoice.getCurrency())));
        List<RevenueAnalyticsEntry> trends = grouped.entrySet().stream()
                .map(entry -> new RevenueAnalyticsEntry(entry.getKey().month(), entry.getKey().currency(),
                        total(entry.getValue()), entry.getValue().size()))
                .sorted(Comparator.comparing(RevenueAnalyticsEntry::month)
                        .thenComparing(RevenueAnalyticsEntry::currency))
                .toList();
        return new RevenueAnalyticsResponse(trends);
    }

    private List<Invoice> invoicesForUser(UUID userId) {
        return invoiceRepository.findAllByUserId(userId);
    }

    private BigDecimal totalForDirection(List<Invoice> invoices, InvoiceDirection direction) {
        return invoices.stream().filter(invoice -> invoice.getDirection() == direction)
                .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal total(List<Invoice> invoices) {
        return invoices.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record MonthlyKey(YearMonth month, String currency) {
    }

    private record CategoryKey(UUID categoryId, String categoryName, InvoiceDirection direction, String currency) {
    }
}

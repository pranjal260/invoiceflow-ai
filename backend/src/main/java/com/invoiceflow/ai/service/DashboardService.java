package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.dashboard.DashboardCurrencySummary;
import com.invoiceflow.ai.dto.dashboard.DashboardSummaryResponse;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceDirection;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final InvoiceRepository invoiceRepository;

    public DashboardService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public DashboardSummaryResponse getSummary(UUID userId) {
        Map<String, List<Invoice>> invoicesByCurrency = invoiceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(Invoice::getCurrency));

        List<DashboardCurrencySummary> summaries = invoicesByCurrency.entrySet().stream()
                .map(entry -> toSummary(entry.getKey(), entry.getValue()))
                .sorted(java.util.Comparator.comparing(DashboardCurrencySummary::currency))
                .toList();
        return new DashboardSummaryResponse(summaries);
    }

    private DashboardCurrencySummary toSummary(String currency, List<Invoice> invoices) {
        BigDecimal revenue = totalForDirection(invoices, InvoiceDirection.INCOME);
        BigDecimal expenses = totalForDirection(invoices, InvoiceDirection.EXPENSE);
        long paidInvoices = invoices.stream().filter(invoice -> invoice.getStatus() == InvoiceStatus.PAID).count();
        long pendingInvoices = invoices.stream().filter(invoice -> invoice.getStatus() == InvoiceStatus.PENDING).count();
        return new DashboardCurrencySummary(currency, revenue, expenses, paidInvoices, pendingInvoices, invoices.size());
    }

    private BigDecimal totalForDirection(List<Invoice> invoices, InvoiceDirection direction) {
        return invoices.stream()
                .filter(invoice -> invoice.getDirection() == direction)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

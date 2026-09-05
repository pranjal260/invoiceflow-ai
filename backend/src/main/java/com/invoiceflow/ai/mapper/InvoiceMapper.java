package com.invoiceflow.ai.mapper;

import com.invoiceflow.ai.dto.invoice.InvoiceCreateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.User;

public final class InvoiceMapper {
    private InvoiceMapper() { }

    public static Invoice toEntity(InvoiceCreateRequest request, User user, Category category) {
        return new Invoice(user, category, request.invoiceNumber(), request.vendorName(), request.invoiceDate(),
                request.dueDate(), request.status(), request.direction(), request.subtotal(), request.taxAmount(), request.totalAmount(),
                request.currency(), request.notes(), request.fileUrl());
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(invoice.getId(), invoice.getUser().getId(),
                invoice.getCategory() == null ? null : invoice.getCategory().getId(), invoice.getInvoiceNumber(),
                invoice.getVendorName(), invoice.getInvoiceDate(), invoice.getDueDate(), invoice.getStatus(),
                invoice.getSubtotal(), invoice.getTaxAmount(), invoice.getTotalAmount(), invoice.getCurrency(),
                invoice.getNotes(), invoice.getFileUrl(), invoice.getCreatedAt(), invoice.getUpdatedAt(), invoice.getDirection());
    }

    public static void updateEntity(InvoiceUpdateRequest request, Invoice invoice, Category category) {
        invoice.updateDetails(category, request.invoiceNumber(), request.vendorName(), request.invoiceDate(),
                request.dueDate(), request.status(), request.subtotal(), request.taxAmount(), request.totalAmount(),
                request.currency(), request.notes(), request.fileUrl(), request.direction());
    }
}

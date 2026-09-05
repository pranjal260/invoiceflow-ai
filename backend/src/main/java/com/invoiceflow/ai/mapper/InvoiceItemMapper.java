package com.invoiceflow.ai.mapper;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemRequest;
import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;

public final class InvoiceItemMapper {
    private InvoiceItemMapper() { }

    public static InvoiceItem toEntity(InvoiceItemRequest request, Invoice invoice) {
        return new InvoiceItem(invoice, request.description(), request.quantity(), request.unitPrice(), request.amount());
    }

    public static InvoiceItemResponse toResponse(InvoiceItem invoiceItem) {
        return new InvoiceItemResponse(invoiceItem.getId(), invoiceItem.getInvoice().getId(), invoiceItem.getDescription(),
                invoiceItem.getQuantity(), invoiceItem.getUnitPrice(), invoiceItem.getAmount());
    }

    public static void updateEntity(InvoiceItemRequest request, InvoiceItem invoiceItem) {
        invoiceItem.updateDetails(request.description(), request.quantity(), request.unitPrice(), request.amount());
    }
}

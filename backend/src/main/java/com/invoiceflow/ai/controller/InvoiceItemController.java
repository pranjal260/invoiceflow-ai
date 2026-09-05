package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemRequest;
import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import com.invoiceflow.ai.service.InvoiceItemService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/items")
public class InvoiceItemController {

    private final InvoiceItemService invoiceItemService;

    public InvoiceItemController(InvoiceItemService invoiceItemService) {
        this.invoiceItemService = invoiceItemService;
    }

    @GetMapping
    public List<InvoiceItemResponse> findAll(@PathVariable UUID invoiceId, @AuthenticationPrincipal UUID userId) {
        return invoiceItemService.findAllByInvoiceId(invoiceId, userId);
    }

    @GetMapping("/{itemId}")
    public InvoiceItemResponse findById(@PathVariable UUID invoiceId, @PathVariable UUID itemId,
                                        @AuthenticationPrincipal UUID userId) {
        return invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceItemResponse create(@PathVariable UUID invoiceId, @AuthenticationPrincipal UUID userId,
                                      @Valid @RequestBody InvoiceItemRequest request) {
        return invoiceItemService.createInvoiceItem(invoiceId, userId, request);
    }

    @PutMapping("/{itemId}")
    public InvoiceItemResponse update(@PathVariable UUID invoiceId, @PathVariable UUID itemId,
                                      @AuthenticationPrincipal UUID userId,
                                      @Valid @RequestBody InvoiceItemRequest request) {
        return invoiceItemService.updateInvoiceItem(itemId, invoiceId, userId, request);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID invoiceId, @PathVariable UUID itemId,
                       @AuthenticationPrincipal UUID userId) {
        invoiceItemService.deleteInvoiceItem(itemId, invoiceId, userId);
    }
}

package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.invoice.DocumentResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceCreateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceStatusUpdateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceUpdateRequest;
import com.invoiceflow.ai.service.InvoiceService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<InvoiceResponse> findAll(@AuthenticationPrincipal UUID userId) {
        return invoiceService.findAllByUserId(userId);
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        return invoiceService.findByIdForUser(id, userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@AuthenticationPrincipal UUID userId,
                                  @Valid @RequestBody InvoiceCreateRequest request) {
        return invoiceService.createInvoice(userId, request);
    }

    @PutMapping("/{id}")
    public InvoiceResponse update(@PathVariable UUID id, @AuthenticationPrincipal UUID userId,
                                  @Valid @RequestBody InvoiceUpdateRequest request) {
        return invoiceService.updateInvoice(id, userId, request);
    }

    @PatchMapping("/{id}/status")
    public InvoiceResponse updateStatus(@PathVariable UUID id, @AuthenticationPrincipal UUID userId,
                                        @Valid @RequestBody InvoiceStatusUpdateRequest request) {
        return invoiceService.updateStatus(id, userId, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        invoiceService.deleteInvoice(id, userId);
    }

    @PostMapping("/{id}/document")
    public InvoiceResponse uploadDocument(@PathVariable UUID id,
                                           @AuthenticationPrincipal UUID userId,
                                           @RequestParam("file") MultipartFile file) throws IOException {
        return invoiceService.uploadDocument(id, userId, file.getBytes(), file.getContentType());
    }

    @GetMapping("/{id}/document")
    public DocumentResponse getDocument(@PathVariable UUID id,
                                         @AuthenticationPrincipal UUID userId) {
        return invoiceService.getDocumentUrl(id, userId);
    }

    @DeleteMapping("/{id}/document")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id,
                                @AuthenticationPrincipal UUID userId) {
        invoiceService.deleteDocument(id, userId);
    }

    @PostMapping("/{id}/extract")
    public InvoiceResponse extractInvoice(@PathVariable UUID id,
                                           @AuthenticationPrincipal UUID userId) {
        return invoiceService.extractInvoice(id, userId);
    }
}

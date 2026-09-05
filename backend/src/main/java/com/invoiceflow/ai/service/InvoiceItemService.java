package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemRequest;
import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.exception.InvoiceItemNotFoundException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.mapper.InvoiceItemMapper;
import com.invoiceflow.ai.repository.InvoiceItemRepository;
import com.invoiceflow.ai.repository.InvoiceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvoiceItemService {

    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;

    public InvoiceItemService(InvoiceItemRepository invoiceItemRepository, InvoiceRepository invoiceRepository) {
        this.invoiceItemRepository = invoiceItemRepository;
        this.invoiceRepository = invoiceRepository;
    }

    public List<InvoiceItemResponse> findAllByInvoiceId(UUID invoiceId, UUID userId) {
        getInvoiceForUser(invoiceId, userId);
        return invoiceItemRepository.findAllByInvoiceId(invoiceId).stream()
                .map(InvoiceItemMapper::toResponse)
                .toList();
    }

    public InvoiceItemResponse findByIdForInvoice(UUID invoiceItemId, UUID invoiceId, UUID userId) {
        getInvoiceForUser(invoiceId, userId);
        return InvoiceItemMapper.toResponse(getInvoiceItemForInvoice(invoiceItemId, invoiceId));
    }

    public InvoiceItemResponse createInvoiceItem(UUID invoiceId, UUID userId, InvoiceItemRequest request) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        InvoiceItem invoiceItem = InvoiceItemMapper.toEntity(request, invoice);
        return InvoiceItemMapper.toResponse(invoiceItemRepository.save(invoiceItem));
    }

    public InvoiceItemResponse updateInvoiceItem(UUID invoiceItemId, UUID invoiceId, UUID userId,
                                                 InvoiceItemRequest request) {
        getInvoiceForUser(invoiceId, userId);
        InvoiceItem invoiceItem = getInvoiceItemForInvoice(invoiceItemId, invoiceId);
        InvoiceItemMapper.updateEntity(request, invoiceItem);
        return InvoiceItemMapper.toResponse(invoiceItemRepository.save(invoiceItem));
    }

    public void deleteInvoiceItem(UUID invoiceItemId, UUID invoiceId, UUID userId) {
        getInvoiceForUser(invoiceId, userId);
        invoiceItemRepository.delete(getInvoiceItemForInvoice(invoiceItemId, invoiceId));
    }

    private Invoice getInvoiceForUser(UUID invoiceId, UUID userId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    private InvoiceItem getInvoiceItemForInvoice(UUID invoiceItemId, UUID invoiceId) {
        return invoiceItemRepository.findByIdAndInvoiceId(invoiceItemId, invoiceId)
                .orElseThrow(() -> new InvoiceItemNotFoundException(invoiceItemId));
    }
}

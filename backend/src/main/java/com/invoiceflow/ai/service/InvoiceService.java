package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.invoice.DocumentResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceCreateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceUploadPreviewResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DocumentNotFoundException;
import com.invoiceflow.ai.exception.DuplicateInvoiceNumberException;
import com.invoiceflow.ai.exception.InvoiceExtractionException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.mapper.InvoiceMapper;
import com.invoiceflow.ai.repository.CategoryRepository;
import com.invoiceflow.ai.repository.InvoiceItemRepository;
import com.invoiceflow.ai.repository.InvoiceRepository;
import com.invoiceflow.ai.repository.UserRepository;
import com.invoiceflow.ai.service.ai.InvoiceExtractionResult;
import com.invoiceflow.ai.service.ai.InvoiceExtractionService;
import com.invoiceflow.ai.service.storage.FileDownloadResult;
import com.invoiceflow.ai.service.storage.FileStorageService;
import com.invoiceflow.ai.service.storage.FileUploadResult;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final InvoiceExtractionService invoiceExtractionService;
    private final InvoiceItemRepository invoiceItemRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, UserRepository userRepository,
                          CategoryRepository categoryRepository, FileStorageService fileStorageService,
                          InvoiceExtractionService invoiceExtractionService,
                          InvoiceItemRepository invoiceItemRepository) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.fileStorageService = fileStorageService;
        this.invoiceExtractionService = invoiceExtractionService;
        this.invoiceItemRepository = invoiceItemRepository;
    }

    public List<InvoiceResponse> findAllByUserId(UUID userId) {
        return invoiceRepository.findAllByUserId(userId).stream()
                .map(InvoiceMapper::toResponse)
                .toList();
    }

    public InvoiceResponse findByIdForUser(UUID invoiceId, UUID userId) {
        return InvoiceMapper.toResponse(getInvoiceForUser(invoiceId, userId));
    }

    public InvoiceResponse createInvoice(UUID userId, InvoiceCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (invoiceNumberExists(userId, request.invoiceNumber())) {
            throw new DuplicateInvoiceNumberException(request.invoiceNumber());
        }

        Category category = getCategoryForUser(request.categoryId(), userId);
        Invoice invoice = InvoiceMapper.toEntity(request, user, category);
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse updateInvoice(UUID invoiceId, UUID userId, InvoiceUpdateRequest request) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        if (!invoice.getInvoiceNumber().equals(request.invoiceNumber())
                && invoiceNumberExists(userId, request.invoiceNumber())) {
            throw new DuplicateInvoiceNumberException(request.invoiceNumber());
        }

        Category category = getCategoryForUser(request.categoryId(), userId);
        InvoiceMapper.updateEntity(request, invoice, category);
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse updateStatus(UUID invoiceId, UUID userId, InvoiceStatus status) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        invoice.updateStatus(status);
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void deleteInvoice(UUID invoiceId, UUID userId) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        invoiceItemRepository.deleteAllByInvoiceId(invoiceId);
        invoiceRepository.delete(invoice);
    }

    public InvoiceResponse uploadDocument(UUID invoiceId, UUID userId,
                                           byte[] fileBytes, String contentType) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        String previousKey = invoice.getFileUrl();

        FileUploadResult result = fileStorageService.upload(fileBytes, contentType, userId);
        invoice.updateFileUrl(result.objectKey());
        Invoice saved = invoiceRepository.save(invoice);

        if (previousKey != null && !previousKey.isBlank()) {
            fileStorageService.delete(previousKey);
        }

        return InvoiceMapper.toResponse(saved);
    }

    /**
     * Uploads a document and returns an AI preview without creating an invoice.
     * The caller must review the preview and confirm it through createInvoice.
     */
    public InvoiceUploadPreviewResponse uploadAndExtract(UUID userId, byte[] fileBytes, String contentType) {
        FileUploadResult upload = fileStorageService.upload(fileBytes, contentType, userId);
        try {
            InvoiceExtractionResult result = invoiceExtractionService.extract(fileBytes, contentType);
            validateExtractionResult(result);
            String currency = normalizeExtractedCurrency(result.currency());
            List<InvoiceUploadPreviewResponse.InvoiceItemPreview> items = result.items() == null
                    ? Collections.emptyList()
                    : result.items().stream().peek(this::validateLineItem)
                    .map(item -> new InvoiceUploadPreviewResponse.InvoiceItemPreview(
                            item.description(), item.quantity(), item.unitPrice(), item.amount()))
                    .toList();
            return new InvoiceUploadPreviewResponse(result.invoiceNumber(), result.vendorName(),
                    result.invoiceDate(), result.dueDate(), result.subtotal(), result.taxAmount(),
                    result.totalAmount(), currency, items, upload.objectKey());
        } catch (RuntimeException exception) {
            try {
                fileStorageService.delete(upload.objectKey());
            } catch (RuntimeException ignored) {
                // Preserve the extraction error; cleanup is best effort.
            }
            throw exception;
        }
    }

    public DocumentResponse getDocumentUrl(UUID invoiceId, UUID userId) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        if (invoice.getFileUrl() == null || invoice.getFileUrl().isBlank()) {
            throw new DocumentNotFoundException(invoiceId);
        }
        String url = fileStorageService.generatePresignedUrl(invoice.getFileUrl());
        return new DocumentResponse(invoiceId, url);
    }

    public void deleteDocument(UUID invoiceId, UUID userId) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);
        if (invoice.getFileUrl() == null || invoice.getFileUrl().isBlank()) {
            throw new DocumentNotFoundException(invoiceId);
        }
        String objectKey = invoice.getFileUrl();
        invoice.updateFileUrl(null);
        invoiceRepository.save(invoice);
        fileStorageService.delete(objectKey);
    }

    public boolean invoiceNumberExists(UUID userId, String invoiceNumber) {
        return invoiceRepository.existsByUserIdAndInvoiceNumber(userId, invoiceNumber);
    }

    @Transactional
    public InvoiceResponse extractInvoice(UUID invoiceId, UUID userId) {
        Invoice invoice = getInvoiceForUser(invoiceId, userId);

        if (invoice.getFileUrl() == null || invoice.getFileUrl().isBlank()) {
            throw new DocumentNotFoundException(invoiceId);
        }

        // External calls — before any database mutation
        FileDownloadResult document = fileStorageService.download(invoice.getFileUrl());
        InvoiceExtractionResult result = invoiceExtractionService.extract(
                document.content(), document.contentType());

        // Validate extraction result
        validateExtractionResult(result);

        // Check duplicate invoice number (only if changed)
        if (!result.invoiceNumber().equals(invoice.getInvoiceNumber())
                && invoiceNumberExists(userId, result.invoiceNumber())) {
            throw new DuplicateInvoiceNumberException(result.invoiceNumber());
        }

        String currency = normalizeExtractedCurrency(result.currency());

        // Database mutations — atomic within this @Transactional method
        invoice.updateFromExtraction(
                result.invoiceNumber(), result.vendorName(),
                result.invoiceDate(), result.dueDate(),
                currency, result.subtotal(),
                result.taxAmount(), result.totalAmount());

        invoiceItemRepository.deleteAllByInvoiceId(invoiceId);

        if (result.items() != null) {
            for (InvoiceExtractionResult.LineItem lineItem : result.items()) {
                validateLineItem(lineItem);
                invoiceItemRepository.save(new InvoiceItem(
                        invoice, lineItem.description(),
                        lineItem.quantity(), lineItem.unitPrice(), lineItem.amount()));
            }
        }

        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    private void validateExtractionResult(InvoiceExtractionResult result) {
        if (result.invoiceNumber() == null || result.invoiceNumber().isBlank()
                || result.vendorName() == null || result.vendorName().isBlank()
                || result.invoiceDate() == null
                || result.currency() == null || result.currency().isBlank()
                || result.subtotal() == null
                || result.taxAmount() == null
                || result.totalAmount() == null) {
            throw new InvoiceExtractionException("Extraction returned incomplete invoice data.");
        }
    }

    private String normalizeExtractedCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new InvoiceExtractionException("Extraction returned an invalid currency.");
        }

        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        normalized = switch (normalized) {
            case "$", "US$" -> "USD";
            case "€" -> "EUR";
            case "£" -> "GBP";
            case "₹", "RS", "RS." -> "INR";
            default -> normalized;
        };

        if (!normalized.matches("[A-Z]{3}")) {
            throw new InvoiceExtractionException("Extraction returned an invalid currency.");
        }
        return normalized;
    }

    private void validateLineItem(InvoiceExtractionResult.LineItem item) {
        if (item.description() == null || item.description().isBlank()
                || item.quantity() == null || item.quantity().compareTo(new BigDecimal("0.001")) < 0
                || item.unitPrice() == null
                || item.amount() == null) {
            throw new InvoiceExtractionException("Extraction returned invalid line item data.");
        }
    }

    private Invoice getInvoiceForUser(UUID invoiceId, UUID userId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));
    }

    private Category getCategoryForUser(UUID categoryId, UUID userId) {
        if (categoryId == null) {
            return null;
        }

        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}

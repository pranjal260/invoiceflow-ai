package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.invoice.DocumentResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceCreateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DocumentNotFoundException;
import com.invoiceflow.ai.exception.DuplicateInvoiceNumberException;
import com.invoiceflow.ai.exception.FileStorageException;
import com.invoiceflow.ai.exception.InvoiceExtractionException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private InvoiceExtractionService invoiceExtractionService;

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(invoiceRepository, userRepository, categoryRepository,
                fileStorageService, invoiceExtractionService, invoiceItemRepository);
    }

    @Test
    void listsInvoicesForUser() {
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findAllByUserId(userId)).thenReturn(List.of(invoice("INV-001"), invoice("INV-002")));

        List<InvoiceResponse> responses = invoiceService.findAllByUserId(userId);

        assertThat(responses).extracting(InvoiceResponse::invoiceNumber).containsExactly("INV-001", "INV-002");
        verify(invoiceRepository).findAllByUserId(userId);
    }

    @Test
    void findsInvoiceWhenOwnedByUser() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice("INV-001")));

        InvoiceResponse response = invoiceService.findByIdForUser(invoiceId, userId);

        assertThat(response.invoiceNumber()).isEqualTo("INV-001");
    }

    @Test
    void rejectsInaccessibleInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.findByIdForUser(invoiceId, userId))
                .isInstanceOf(InvoiceNotFoundException.class);
    }

    @Test
    void createsInvoiceForExistingUser() {
        UUID userId = UUID.randomUUID();
        InvoiceCreateRequest request = createRequest(null, "INV-001");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(userId, "INV-001")).thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceResponse response = invoiceService.createInvoice(userId, request);

        assertThat(response.invoiceNumber()).isEqualTo("INV-001");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void rejectsDuplicateInvoiceNumberForUser() {
        UUID userId = UUID.randomUUID();
        InvoiceCreateRequest request = createRequest(null, "INV-001");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(userId, "INV-001")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.createInvoice(userId, request))
                .isInstanceOf(DuplicateInvoiceNumberException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void rejectsCategoryNotOwnedByUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user()));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(userId, "INV-001")).thenReturn(false);
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.createInvoice(userId, createRequest(categoryId, "INV-001")))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void updatesOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        InvoiceUpdateRequest request = updateRequest(null, "INV-002", InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(userId, "INV-002")).thenReturn(false);
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        InvoiceResponse response = invoiceService.updateInvoice(invoiceId, userId, request);

        assertThat(response.invoiceNumber()).isEqualTo("INV-002");
        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void rejectsUpdateForMissingOrInaccessibleInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.updateInvoice(invoiceId, userId,
                updateRequest(null, "INV-002", InvoiceStatus.PAID)))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void deletesOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));

        invoiceService.deleteInvoice(invoiceId, userId);

        verify(invoiceItemRepository).deleteAllByInvoiceId(invoiceId);
        verify(invoiceRepository).delete(invoice);
    }

    @Test
    void rejectsDeletionForMissingOrInaccessibleInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.deleteInvoice(invoiceId, userId))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(invoiceRepository, never()).delete(any());
    }

    @Test
    void updatesStatusForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        InvoiceResponse response = invoiceService.updateStatus(invoiceId, userId, InvoiceStatus.PAID);

        assertThat(response.status()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void uploadsDocumentToOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        String objectKey = "invoices/" + userId + "/test.pdf";
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.upload(any(), eq("application/pdf"), eq(userId)))
                .thenReturn(new FileUploadResult(objectKey));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        InvoiceResponse response = invoiceService.uploadDocument(invoiceId, userId,
                new byte[]{0x25}, "application/pdf");

        assertThat(invoice.getFileUrl()).isEqualTo(objectKey);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void uploadDocumentReplacesExistingAndDeletesOldObject() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        String oldKey = "invoices/" + userId + "/old.pdf";
        String newKey = "invoices/" + userId + "/new.pdf";
        invoice.updateFileUrl(oldKey);
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.upload(any(), eq("application/pdf"), eq(userId)))
                .thenReturn(new FileUploadResult(newKey));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceService.uploadDocument(invoiceId, userId, new byte[]{0x25}, "application/pdf");

        assertThat(invoice.getFileUrl()).isEqualTo(newKey);
        verify(fileStorageService).delete(oldKey);
    }

    @Test
    void uploadDocumentRejectsInaccessibleInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.uploadDocument(invoiceId, userId,
                new byte[]{0x25}, "application/pdf"))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(fileStorageService, never()).upload(any(), any(), any());
    }

    @Test
    void getsDocumentUrlForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        String objectKey = "invoices/" + userId + "/test.pdf";
        invoice.updateFileUrl(objectKey);
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.generatePresignedUrl(objectKey)).thenReturn("https://presigned-url");

        DocumentResponse response = invoiceService.getDocumentUrl(invoiceId, userId);

        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.url()).isEqualTo("https://presigned-url");
    }

    @Test
    void getDocumentUrlThrowsWhenNoDocumentAttached() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.getDocumentUrl(invoiceId, userId))
                .isInstanceOf(DocumentNotFoundException.class);
        verify(fileStorageService, never()).generatePresignedUrl(any());
    }

    @Test
    void deletesDocumentForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        String objectKey = "invoices/" + userId + "/test.pdf";
        invoice.updateFileUrl(objectKey);
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceService.deleteDocument(invoiceId, userId);

        assertThat(invoice.getFileUrl()).isNull();
        verify(fileStorageService).delete(objectKey);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void deleteDocumentThrowsWhenNoDocumentAttached() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.deleteDocument(invoiceId, userId))
                .isInstanceOf(DocumentNotFoundException.class);
        verify(fileStorageService, never()).delete(any());
    }

    // --- AI extraction tests ---

    @Test
    void extractsInvoiceAndUpdatesFields() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download("invoices/key/doc.pdf"))
                .thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), eq("application/pdf")))
                .thenReturn(extractionResult("INV-EXTRACTED"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        InvoiceResponse response = invoiceService.extractInvoice(invoiceId, userId);

        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-EXTRACTED");
        assertThat(invoice.getVendorName()).isEqualTo("Extracted Vendor");
        assertThat(invoice.getInvoiceDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2024, 2, 15));
        assertThat(invoice.getCurrency()).isEqualTo("USD");
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTaxAmount()).isEqualByComparingTo("10.00");
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("110.00");
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void extractionCreatesLineItems() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), any())).thenReturn(extractionResult("INV-001"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceService.extractInvoice(invoiceId, userId);

        ArgumentCaptor<InvoiceItem> captor = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(invoiceItemRepository).save(captor.capture());
        InvoiceItem saved = captor.getValue();
        assertThat(saved.getDescription()).isEqualTo("Widget");
        assertThat(saved.getQuantity()).isEqualByComparingTo("2.000");
        assertThat(saved.getUnitPrice()).isEqualByComparingTo("50.00");
        assertThat(saved.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void reExtractionDeletesExistingItemsBeforeCreatingNew() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), any())).thenReturn(extractionResult("INV-001"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceService.extractInvoice(invoiceId, userId);

        verify(invoiceItemRepository).deleteAllByInvoiceId(invoiceId);
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }

    @Test
    void extractionEnforcesOwnership() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void extractionRejectsMissingDocument() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoice("INV-001"); // no document
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(DocumentNotFoundException.class);
        verify(fileStorageService, never()).download(any());
    }

    @Test
    void extractionWrapsStorageDownloadFailure() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenThrow(new FileStorageException("Failed to download file.", null));

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(FileStorageException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void extractionWrapsGeminiFailure() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), any()))
                .thenThrow(new InvoiceExtractionException("Invoice extraction failed."));

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(InvoiceExtractionException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void extractionRejectsIncompleteResult() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        // Missing vendorName
        when(invoiceExtractionService.extract(any(), any())).thenReturn(
                new InvoiceExtractionResult("INV-X", null, LocalDate.now(), null, "USD",
                        BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN, List.of()));

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(InvoiceExtractionException.class)
                .hasMessageContaining("incomplete");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void extractionRejectsDuplicateInvoiceNumber() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), any())).thenReturn(extractionResult("INV-DUPLICATE"));
        when(invoiceRepository.existsByUserIdAndInvoiceNumber(userId, "INV-DUPLICATE")).thenReturn(true);

        assertThatThrownBy(() -> invoiceService.extractInvoice(invoiceId, userId))
                .isInstanceOf(DuplicateInvoiceNumberException.class);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void extractionAllowsSameInvoiceNumberWithoutDuplicateCheck() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice invoice = invoiceWithDocument("INV-001");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(fileStorageService.download(any())).thenReturn(new FileDownloadResult(new byte[]{0x25}, "application/pdf"));
        when(invoiceExtractionService.extract(any(), any())).thenReturn(extractionResult("INV-001"));
        when(invoiceRepository.save(invoice)).thenReturn(invoice);

        invoiceService.extractInvoice(invoiceId, userId);

        // Should NOT check for duplicates when number is unchanged
        verify(invoiceRepository, never()).existsByUserIdAndInvoiceNumber(any(), any());
        verify(invoiceRepository).save(invoice);
    }

    private static InvoiceExtractionResult extractionResult(String invoiceNumber) {
        return new InvoiceExtractionResult(
                invoiceNumber, "Extracted Vendor",
                LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 15),
                "USD", new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("110.00"),
                List.of(new InvoiceExtractionResult.LineItem(
                        "Widget", new BigDecimal("2.000"), new BigDecimal("50.00"), new BigDecimal("100.00"))));
    }

    private static Invoice invoiceWithDocument(String invoiceNumber) {
        Invoice inv = invoice(invoiceNumber);
        inv.updateFileUrl("invoices/key/doc.pdf");
        return inv;
    }

    private static InvoiceCreateRequest createRequest(UUID categoryId, String invoiceNumber) {
        return new InvoiceCreateRequest(categoryId, invoiceNumber, "Example Vendor", LocalDate.of(2026, 8, 9), null,
                InvoiceStatus.PENDING, new BigDecimal("200.00"), new BigDecimal("36.00"),
                new BigDecimal("236.00"), "INR", "Notes", null);
    }

    private static InvoiceUpdateRequest updateRequest(UUID categoryId, String invoiceNumber, InvoiceStatus status) {
        return new InvoiceUpdateRequest(categoryId, invoiceNumber, "Updated Vendor", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 30), status, new BigDecimal("200.00"), new BigDecimal("36.00"),
                new BigDecimal("236.00"), "INR", "Updated notes", "https://example.com/invoice.pdf");
    }

    private static Invoice invoice(String invoiceNumber) {
        return new Invoice(user(), null, invoiceNumber, "Example Vendor", LocalDate.of(2026, 8, 9), null,
                InvoiceStatus.PENDING, new BigDecimal("200.00"), new BigDecimal("36.00"),
                new BigDecimal("236.00"), "INR", "Notes", null);
    }

    private static User user() {
        return new User("Ada", "Lovelace", "Analytical Engines", "ada@example.com", "password-hash",
                "+44 20 7946 0958", "United Kingdom");
    }
}

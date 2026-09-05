package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemRequest;
import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.InvoiceItemNotFoundException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.repository.InvoiceItemRepository;
import com.invoiceflow.ai.repository.InvoiceRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceItemServiceTest {

    @Mock
    private InvoiceItemRepository invoiceItemRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private InvoiceItemService invoiceItemService;

    @BeforeEach
    void setUp() {
        invoiceItemService = new InvoiceItemService(invoiceItemRepository, invoiceRepository);
    }

    @Test
    void listsItemsForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findAllByInvoiceId(invoiceId))
                .thenReturn(List.of(item("Subscription"), item("Consulting")));

        List<InvoiceItemResponse> responses = invoiceItemService.findAllByInvoiceId(invoiceId, userId);

        assertThat(responses).extracting(InvoiceItemResponse::description)
                .containsExactly("Subscription", "Consulting");
    }

    @Test
    void findsItemForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.of(item("Subscription")));

        InvoiceItemResponse response = invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId);

        assertThat(response.description()).isEqualTo("Subscription");
    }

    @Test
    void rejectsInvoiceNotOwnedByUser() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.findAllByInvoiceId(invoiceId, userId))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(invoiceItemRepository, never()).findAllByInvoiceId(any());
    }

    @Test
    void rejectsItemBelongingToAnotherInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId))
                .isInstanceOf(InvoiceItemNotFoundException.class);
    }

    @Test
    void createsItemForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceItemResponse response = invoiceItemService.createInvoiceItem(invoiceId, userId, request("Subscription"));

        assertThat(response.description()).isEqualTo("Subscription");
        assertThat(response.amount()).isEqualByComparingTo("100.00");
        verify(invoiceItemRepository).save(any(InvoiceItem.class));
    }

    @Test
    void rejectsCreationForInvoiceNotOwnedByUser() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.createInvoiceItem(invoiceId, userId, request("Subscription")))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(invoiceItemRepository, never()).save(any());
    }

    @Test
    void updatesItemForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        InvoiceItem invoiceItem = item("Subscription");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.of(invoiceItem));
        when(invoiceItemRepository.save(invoiceItem)).thenReturn(invoiceItem);

        InvoiceItemResponse response = invoiceItemService.updateInvoiceItem(itemId, invoiceId, userId,
                request("Consulting"));

        assertThat(response.description()).isEqualTo("Consulting");
        verify(invoiceItemRepository).save(invoiceItem);
    }

    @Test
    void rejectsInaccessibleItemUpdate() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.updateInvoiceItem(itemId, invoiceId, userId,
                request("Consulting"))).isInstanceOf(InvoiceItemNotFoundException.class);
        verify(invoiceItemRepository, never()).save(any());
    }

    @Test
    void deletesItemForOwnedInvoice() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        InvoiceItem invoiceItem = item("Subscription");
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.of(invoiceItem));

        invoiceItemService.deleteInvoiceItem(itemId, invoiceId, userId);

        verify(invoiceItemRepository).delete(invoiceItem);
    }

    @Test
    void rejectsInaccessibleItemDeletion() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice()));
        when(invoiceItemRepository.findByIdAndInvoiceId(itemId, invoiceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.deleteInvoiceItem(itemId, invoiceId, userId))
                .isInstanceOf(InvoiceItemNotFoundException.class);
        verify(invoiceItemRepository, never()).delete(any());
    }

    @Test
    void rejectsMissingInvoiceBeforeLookingUpItem() {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceRepository.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId))
                .isInstanceOf(InvoiceNotFoundException.class);
        verify(invoiceItemRepository, never()).findByIdAndInvoiceId(any(), any());
    }

    private static InvoiceItemRequest request(String description) {
        return new InvoiceItemRequest(description, new BigDecimal("1.000"), new BigDecimal("100.00"),
                new BigDecimal("100.00"));
    }

    private static InvoiceItem item(String description) {
        return new InvoiceItem(invoice(), description, new BigDecimal("1.000"), new BigDecimal("100.00"),
                new BigDecimal("100.00"));
    }

    private static Invoice invoice() {
        return new Invoice(user(), null, "INV-001", "Example Vendor", LocalDate.of(2026, 8, 9), null,
                InvoiceStatus.PENDING, new BigDecimal("200.00"), new BigDecimal("36.00"),
                new BigDecimal("236.00"), "INR", "Notes", null);
    }

    private static User user() {
        return new User("Ada", "Lovelace", "Analytical Engines", "ada@example.com", "password-hash",
                "+44 20 7946 0958", "United Kingdom");
    }
}

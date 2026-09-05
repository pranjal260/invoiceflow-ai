package com.invoiceflow.ai.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceflow.ai.dto.category.CategoryCreateRequest;
import com.invoiceflow.ai.dto.category.CategoryUpdateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceCreateRequest;
import com.invoiceflow.ai.dto.invoice.InvoiceUpdateRequest;
import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemRequest;
import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.dto.user.UserProfileUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceDirection;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MapperTest {

    @Test
    void mapsUserProfileWithoutExposingPasswordHash() {
        User user = new User("Ada", "Lovelace", "Analytical Engines", "ada@example.com", "password-hash",
                "+44 20 7946 0958", "United Kingdom");

        UserMapper.updateEntity(new UserProfileUpdateRequest("Grace", "Hopper", "Compilers Inc.",
                "grace@example.com", "+1 212 555 0100", "United States"), user);
        UserProfileResponse response = UserMapper.toResponse(user);

        assertThat(response.firstName()).isEqualTo("Grace");
        assertThat(response.email()).isEqualTo("grace@example.com");
        assertThat(response.phoneNumber()).isEqualTo("+1 212 555 0100");
        assertThat(Arrays.stream(UserProfileResponse.class.getRecordComponents()).map(RecordComponent::getName))
                .noneMatch(name -> name.toLowerCase().contains("password"));
    }

    @Test
    void mapsCategoryCreateAndUpdateData() {
        User user = user();
        Category category = CategoryMapper.toEntity(new CategoryCreateRequest("Software", "Subscriptions"), user);

        CategoryMapper.updateEntity(new CategoryUpdateRequest("Infrastructure", "Cloud services"), category);

        assertThat(category.getUser()).isSameAs(user);
        assertThat(category.getName()).isEqualTo("Infrastructure");
        assertThat(category.getDescription()).isEqualTo("Cloud services");
    }

    @Test
    void mapsInvoiceCreateAndUpdateData() {
        User user = user();
        Category category = new Category(user, "Software", "Subscriptions");
        Invoice invoice = InvoiceMapper.toEntity(invoiceRequest("INV-001", InvoiceStatus.PENDING), user, category);

        InvoiceMapper.updateEntity(invoiceUpdateRequest("INV-002", InvoiceStatus.PAID), invoice, null);

        assertThat(invoice.getUser()).isSameAs(user);
        assertThat(invoice.getCategory()).isNull();
        assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-002");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getDirection()).isEqualTo(InvoiceDirection.EXPENSE);
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("236.00");
    }

    @Test
    void mapsInvoiceItemCreateAndUpdateData() {
        Invoice invoice = InvoiceMapper.toEntity(invoiceRequest("INV-001", InvoiceStatus.PENDING), user(), null);
        InvoiceItem invoiceItem = InvoiceItemMapper.toEntity(itemRequest("Subscription", "1.000", "100.00"), invoice);

        InvoiceItemMapper.updateEntity(itemRequest("Consulting", "2.500", "250.00"), invoiceItem);

        assertThat(invoiceItem.getInvoice()).isSameAs(invoice);
        assertThat(invoiceItem.getDescription()).isEqualTo("Consulting");
        assertThat(invoiceItem.getQuantity()).isEqualByComparingTo("2.500");
        assertThat(invoiceItem.getAmount()).isEqualByComparingTo("250.00");
    }

    private static User user() {
        return new User("Ada", "Lovelace", "Analytical Engines", "ada@example.com", "password-hash",
                "+44 20 7946 0958", "United Kingdom");
    }

    private static InvoiceCreateRequest invoiceRequest(String invoiceNumber, InvoiceStatus status) {
        return new InvoiceCreateRequest(null, invoiceNumber, "Example Vendor", LocalDate.of(2026, 8, 9), null, status,
                new BigDecimal("200.00"), new BigDecimal("36.00"), new BigDecimal("236.00"), "INR", "Notes", null);
    }

    private static InvoiceUpdateRequest invoiceUpdateRequest(String invoiceNumber, InvoiceStatus status) {
        return new InvoiceUpdateRequest(null, invoiceNumber, "Updated Vendor", LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 30), status, new BigDecimal("200.00"), new BigDecimal("36.00"),
                new BigDecimal("236.00"), "INR", "Updated notes", "https://example.com/invoice.pdf");
    }

    private static InvoiceItemRequest itemRequest(String description, String quantity, String amount) {
        return new InvoiceItemRequest(description, new BigDecimal(quantity), new BigDecimal("100.00"),
                new BigDecimal(amount));
    }
}

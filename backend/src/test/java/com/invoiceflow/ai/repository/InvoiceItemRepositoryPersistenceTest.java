package com.invoiceflow.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
import com.invoiceflow.ai.entity.InvoiceItem;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf(value = "databaseConfigurationAvailable", disabledReason = "Requires PostgreSQL database configuration")
class InvoiceItemRepositoryPersistenceTest {

    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndRetrievesInvoiceItemBelongingToInvoice() {
        User user = userRepository.saveAndFlush(new User(
                "Ada",
                "Lovelace",
                "Analytical Engines Ltd.",
                "ada.items@example.com",
                "bcrypt-password-hash-placeholder",
                "+44 20 7946 0958",
                "United Kingdom"
        ));
        Category category = categoryRepository.saveAndFlush(new Category(
                user,
                "Software",
                "Software subscriptions and services"
        ));
        Invoice invoice = invoiceRepository.saveAndFlush(new Invoice(
                user,
                category,
                "INV-ITEM-001",
                "Example Vendor Ltd.",
                LocalDate.of(2026, 8, 9),
                null,
                InvoiceStatus.PENDING,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00"),
                "INR",
                null,
                null
        ));
        InvoiceItem invoiceItem = invoiceItemRepository.saveAndFlush(new InvoiceItem(
                invoice,
                "Monthly software subscription",
                new BigDecimal("1.000"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        ));

        assertThat(invoiceItem.getId()).isNotNull();
        assertThat(invoiceItemRepository.findById(invoiceItem.getId()))
                .isPresent()
                .get()
                .extracting(
                        InvoiceItem::getDescription,
                        savedItem -> savedItem.getInvoice().getId(),
                        InvoiceItem::getQuantity,
                        InvoiceItem::getUnitPrice,
                        InvoiceItem::getAmount
                )
                .containsExactly(
                        "Monthly software subscription",
                        invoice.getId(),
                        new BigDecimal("1.000"),
                        new BigDecimal("100.00"),
                        new BigDecimal("100.00")
                );
    }

    static boolean databaseConfigurationAvailable() {
        return hasText("DB_URL") && hasText("DB_USERNAME") && hasText("DB_PASSWORD");
    }

    private static boolean hasText(String variableName) {
        String value = System.getenv(variableName);
        return value != null && !value.isBlank();
    }
}

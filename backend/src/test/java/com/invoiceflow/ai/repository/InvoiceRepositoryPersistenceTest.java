package com.invoiceflow.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.Invoice;
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
class InvoiceRepositoryPersistenceTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void savesAndRetrievesInvoiceOwnedByUserAndAssignedToCategory() {
        User user = userRepository.saveAndFlush(new User(
                "Ada",
                "Lovelace",
                "Analytical Engines Ltd.",
                "ada.invoices@example.com",
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
                "INV-001",
                "Example Vendor Ltd.",
                LocalDate.of(2026, 8, 9),
                LocalDate.of(2026, 8, 30),
                InvoiceStatus.PENDING,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                new BigDecimal("118.00"),
                "INR",
                "Monthly subscription",
                null
        ));

        assertThat(invoice.getId()).isNotNull();
        assertThat(invoice.getCreatedAt()).isNotNull();
        assertThat(invoice.getUpdatedAt()).isNotNull();
        assertThat(invoiceRepository.findById(invoice.getId()))
                .isPresent()
                .get()
                .extracting(
                        Invoice::getInvoiceNumber,
                        savedInvoice -> savedInvoice.getUser().getId(),
                        savedInvoice -> savedInvoice.getCategory().getId(),
                        Invoice::getStatus,
                        Invoice::getTotalAmount
                )
                .containsExactly("INV-001", user.getId(), category.getId(), InvoiceStatus.PENDING, new BigDecimal("118.00"));
    }

    static boolean databaseConfigurationAvailable() {
        return hasText("DB_URL") && hasText("DB_USERNAME") && hasText("DB_PASSWORD");
    }

    private static boolean hasText(String variableName) {
        String value = System.getenv(variableName);
        return value != null && !value.isBlank();
    }
}

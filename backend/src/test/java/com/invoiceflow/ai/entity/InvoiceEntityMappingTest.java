package com.invoiceflow.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvoiceEntityMappingTest {

    @Test
    void mapsInvoiceOwnershipCategoryStatusDatesMoneyAndScopedUniqueness() throws NoSuchFieldException {
        Table table = Invoice.class.getAnnotation(Table.class);
        ManyToOne userRelationship = Invoice.class.getDeclaredField("user").getAnnotation(ManyToOne.class);
        JoinColumn userJoinColumn = Invoice.class.getDeclaredField("user").getAnnotation(JoinColumn.class);
        ManyToOne categoryRelationship = Invoice.class.getDeclaredField("category").getAnnotation(ManyToOne.class);
        JoinColumn categoryJoinColumn = Invoice.class.getDeclaredField("category").getAnnotation(JoinColumn.class);
        Enumerated statusMapping = Invoice.class.getDeclaredField("status").getAnnotation(Enumerated.class);
        Enumerated directionMapping = Invoice.class.getDeclaredField("direction").getAnnotation(Enumerated.class);

        assertThat(userRelationship.optional()).isFalse();
        assertThat(userJoinColumn.name()).isEqualTo("user_id");
        assertThat(userJoinColumn.nullable()).isFalse();
        assertThat(categoryRelationship.optional()).isTrue();
        assertThat(categoryJoinColumn.name()).isEqualTo("category_id");
        assertThat(statusMapping.value()).isEqualTo(EnumType.STRING);
        assertThat(directionMapping.value()).isEqualTo(EnumType.STRING);
        assertThat(Invoice.class.getDeclaredField("invoiceDate").getType()).isEqualTo(LocalDate.class);
        assertThat(Invoice.class.getDeclaredField("dueDate").getType()).isEqualTo(LocalDate.class);
        assertThat(Invoice.class.getDeclaredField("subtotal").getType()).isEqualTo(BigDecimal.class);
        assertThat(Invoice.class.getDeclaredField("taxAmount").getType()).isEqualTo(BigDecimal.class);
        assertThat(Invoice.class.getDeclaredField("totalAmount").getType()).isEqualTo(BigDecimal.class);
        assertThat(Invoice.class.getDeclaredField("subtotal").getAnnotation(Column.class))
                .extracting(Column::precision, Column::scale)
                .containsExactly(19, 2);
        assertThat(table.uniqueConstraints())
                .anySatisfy(constraint -> assertThat(constraint.columnNames())
                        .containsExactlyInAnyOrder("user_id", "invoice_number"));
    }
}

package com.invoiceflow.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvoiceItemEntityMappingTest {

    @Test
    void mapsRequiredLazyInvoiceOwnershipAndPreciseNumericFields() throws NoSuchFieldException {
        ManyToOne invoiceRelationship = InvoiceItem.class.getDeclaredField("invoice").getAnnotation(ManyToOne.class);
        JoinColumn invoiceJoinColumn = InvoiceItem.class.getDeclaredField("invoice").getAnnotation(JoinColumn.class);

        assertThat(invoiceRelationship.optional()).isFalse();
        assertThat(invoiceRelationship.fetch()).isEqualTo(jakarta.persistence.FetchType.LAZY);
        assertThat(invoiceJoinColumn.name()).isEqualTo("invoice_id");
        assertThat(invoiceJoinColumn.nullable()).isFalse();
        assertThat(InvoiceItem.class.getDeclaredField("quantity").getType()).isEqualTo(BigDecimal.class);
        assertThat(InvoiceItem.class.getDeclaredField("unitPrice").getType()).isEqualTo(BigDecimal.class);
        assertThat(InvoiceItem.class.getDeclaredField("amount").getType()).isEqualTo(BigDecimal.class);
        assertThat(InvoiceItem.class.getDeclaredField("quantity").getAnnotation(Column.class))
                .extracting(Column::precision, Column::scale)
                .containsExactly(19, 3);
        assertThat(InvoiceItem.class.getDeclaredField("unitPrice").getAnnotation(Column.class))
                .extracting(Column::precision, Column::scale)
                .containsExactly(19, 2);
        assertThat(InvoiceItem.class.getDeclaredField("amount").getAnnotation(Column.class))
                .extracting(Column::precision, Column::scale)
                .containsExactly(19, 2);
    }
}

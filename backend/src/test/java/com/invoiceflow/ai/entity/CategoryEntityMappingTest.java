package com.invoiceflow.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class CategoryEntityMappingTest {

    @Test
    void mapsRequiredUserOwnershipAndUserScopedCategoryNameUniqueness() throws NoSuchFieldException {
        Table table = Category.class.getAnnotation(Table.class);
        ManyToOne userRelationship = Category.class.getDeclaredField("user").getAnnotation(ManyToOne.class);
        JoinColumn userJoinColumn = Category.class.getDeclaredField("user").getAnnotation(JoinColumn.class);

        assertThat(userRelationship.optional()).isFalse();
        assertThat(userJoinColumn.name()).isEqualTo("user_id");
        assertThat(userJoinColumn.nullable()).isFalse();
        assertThat(table.uniqueConstraints())
                .anySatisfy(constraint -> assertThat(constraint.columnNames())
                        .containsExactlyInAnyOrder("user_id", "name"));
    }
}

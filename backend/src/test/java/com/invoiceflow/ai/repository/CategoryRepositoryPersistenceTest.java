package com.invoiceflow.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf(value = "databaseConfigurationAvailable", disabledReason = "Requires PostgreSQL database configuration")
class CategoryRepositoryPersistenceTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndRetrievesCategoryOwnedByUser() {
        User user = userRepository.saveAndFlush(new User(
                "Ada",
                "Lovelace",
                "Analytical Engines Ltd.",
                "ada.categories@example.com",
                "bcrypt-password-hash-placeholder",
                "+44 20 7946 0958",
                "United Kingdom"
        ));
        Category category = categoryRepository.saveAndFlush(new Category(
                user,
                "Software",
                "Software subscriptions and services"
        ));

        assertThat(category.getId()).isNotNull();
        assertThat(category.getCreatedAt()).isNotNull();
        assertThat(categoryRepository.findById(category.getId()))
                .isPresent()
                .get()
                .extracting(Category::getName, savedCategory -> savedCategory.getUser().getId())
                .containsExactly("Software", user.getId());
    }

    static boolean databaseConfigurationAvailable() {
        return hasText("DB_URL") && hasText("DB_USERNAME") && hasText("DB_PASSWORD");
    }

    private static boolean hasText(String variableName) {
        String value = System.getenv(variableName);
        return value != null && !value.isBlank();
    }
}

package com.invoiceflow.ai.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.invoiceflow.ai.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIf(value = "databaseConfigurationAvailable", disabledReason = "Requires PostgreSQL database configuration")
class UserRepositoryPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndRetrievesUserWithLifecycleTimestamps() {
        User user = new User(
                "Ada",
                "Lovelace",
                "Analytical Engines Ltd.",
                "ada@example.com",
                "bcrypt-password-hash-placeholder",
                "+44 20 7946 0958",
                "United Kingdom"
        );

        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();
        assertThat(userRepository.findById(savedUser.getId()))
                .isPresent()
                .get()
                .extracting(User::getEmail, User::getPasswordHash)
                .containsExactly("ada@example.com", "bcrypt-password-hash-placeholder");
    }

    static boolean databaseConfigurationAvailable() {
        return hasText("DB_URL") && hasText("DB_USERNAME") && hasText("DB_PASSWORD");
    }

    private static boolean hasText(String variableName) {
        String value = System.getenv(variableName);
        return value != null && !value.isBlank();
    }
}

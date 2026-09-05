package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.dto.user.UserProfileUpdateRequest;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.EmailAlreadyExistsException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void findsUserById() {
        UUID userId = UUID.randomUUID();
        User user = user("ada@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.findById(userId);

        assertThat(response.email()).isEqualTo("ada@example.com");
        verify(userRepository).findById(userId);
    }

    @Test
    void throwsWhenUserIsNotFoundById() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(userId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void createsUserWhenEmailIsAvailable() {
        User user = user("ada@example.com");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.createUser(user);

        assertThat(response.email()).isEqualTo("ada@example.com");
        verify(userRepository).save(user);
    }

    @Test
    void rejectsCreationWhenEmailAlreadyExists() {
        User user = user("ada@example.com");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatesProfileWhenUserExistsAndEmailIsUnchanged() {
        UUID userId = UUID.randomUUID();
        User user = user("ada@example.com");
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("Grace", "Hopper", "Compilers Inc.",
                "ada@example.com", "+1 212 555 0100", "United States");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(userId, request);

        assertThat(response.firstName()).isEqualTo("Grace");
        assertThat(response.companyName()).isEqualTo("Compilers Inc.");
        verify(userRepository).save(user);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void throwsWhenUpdatingMissingUser() {
        UUID userId = UUID.randomUUID();
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("Grace", "Hopper", null,
                "grace@example.com", null, null);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(userId, request))
                .isInstanceOf(UserNotFoundException.class);
        verify(userRepository, never()).save(any());
    }

    private static User user(String email) {
        return new User("Ada", "Lovelace", "Analytical Engines", email, "existing-password-hash",
                "+44 20 7946 0958", "United Kingdom");
    }
}

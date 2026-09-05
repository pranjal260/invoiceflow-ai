package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.auth.LoginRequest;
import com.invoiceflow.ai.dto.auth.LoginResponse;
import com.invoiceflow.ai.dto.auth.RegistrationRequest;
import com.invoiceflow.ai.dto.auth.ChangePasswordRequest;
import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.EmailAlreadyExistsException;
import com.invoiceflow.ai.exception.InvalidCredentialsException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    private PasswordEncoder passwordEncoder;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationService = new AuthenticationService(userService, passwordEncoder, jwtService);
    }

    @Test
    void hashesPasswordBeforeCreatingUser() {
        RegistrationRequest request = registrationRequest();
        UserProfileResponse expected = profile();
        when(userService.createUser(any(User.class))).thenReturn(expected);

        UserProfileResponse response = authenticationService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).createUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo(request.password());
        assertThat(passwordEncoder.matches(request.password(), userCaptor.getValue().getPasswordHash())).isTrue();
        assertThat(response).isEqualTo(expected);
    }

    @Test
    void propagatesDuplicateEmailRejectionDuringRegistration() {
        when(userService.createUser(any(User.class))).thenThrow(new EmailAlreadyExistsException("ada@example.com"));

        assertThatThrownBy(() -> authenticationService.register(registrationRequest()))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void returnsTokenForCorrectCredentials() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, passwordEncoder.encode("correct-password"));
        when(user.getId()).thenReturn(userId);
        when(user.getEmail()).thenReturn("ada@example.com");
        when(userService.getUserByEmail("ada@example.com")).thenReturn(user);
        when(jwtService.generateToken(userId)).thenReturn("jwt-token");

        LoginResponse response = authenticationService.login(new LoginRequest("ada@example.com", "correct-password"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ada@example.com");
    }

    @Test
    void rejectsIncorrectPasswordWithSafeCredentialsError() {
        User user = user(UUID.randomUUID(), passwordEncoder.encode("correct-password"));
        when(userService.getUserByEmail("ada@example.com")).thenReturn(user);

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void rejectsUnknownEmailWithSameSafeCredentialsError() {
        when(userService.getUserByEmail("missing@example.com"))
                .thenThrow(new UserNotFoundException("missing@example.com"));

        assertThatThrownBy(() -> authenticationService.login(new LoginRequest("missing@example.com", "password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    void hashesNewPasswordBeforeUpdatingIt() {
        UUID userId = UUID.randomUUID();
        String currentPassword = "correct-password";
        String newPassword = "updated-password";
        User user = user(userId, passwordEncoder.encode(currentPassword));
        when(userService.getUserById(userId)).thenReturn(user);

        authenticationService.changePassword(userId, new ChangePasswordRequest(currentPassword, newPassword));

        ArgumentCaptor<String> passwordHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(userService).updatePasswordHash(org.mockito.Mockito.eq(userId), passwordHashCaptor.capture());
        assertThat(passwordHashCaptor.getValue()).isNotEqualTo(newPassword);
        assertThat(passwordEncoder.matches(newPassword, passwordHashCaptor.getValue())).isTrue();
    }

    @Test
    void rejectsIncorrectCurrentPasswordWithoutUpdatingIt() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, passwordEncoder.encode("correct-password"));
        when(userService.getUserById(userId)).thenReturn(user);

        assertThatThrownBy(() -> authenticationService.changePassword(userId,
                new ChangePasswordRequest("wrong-password", "updated-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid current password.");

        verify(userService, never()).updatePasswordHash(any(), any());
    }

    @Test
    void rejectsReusingCurrentPassword() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, passwordEncoder.encode("correct-password"));
        when(userService.getUserById(userId)).thenReturn(user);

        assertThatThrownBy(() -> authenticationService.changePassword(userId,
                new ChangePasswordRequest("correct-password", "correct-password")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userService, never()).updatePasswordHash(any(), any());
    }

    private static RegistrationRequest registrationRequest() {
        return new RegistrationRequest("Ada", "Lovelace", "Analytical Engines", "ada@example.com",
                "correct-password", "+44 20 7946 0958", "United Kingdom");
    }

    private static UserProfileResponse profile() {
        return new UserProfileResponse(UUID.randomUUID(), "Ada", "Lovelace", "Analytical Engines",
                "ada@example.com", "+44 20 7946 0958", "United Kingdom", Instant.now(), Instant.now());
    }

    private static User user(UUID userId, String passwordHash) {
        User user = org.mockito.Mockito.mock(User.class);
        when(user.getPasswordHash()).thenReturn(passwordHash);
        return user;
    }
}

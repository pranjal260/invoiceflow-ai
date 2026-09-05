package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.auth.LoginResponse;
import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.exception.EmailAlreadyExistsException;
import com.invoiceflow.ai.exception.InvalidCredentialsException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.AuthenticationService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void registerIsPublic() throws Exception {
        when(authenticationService.register(any())).thenReturn(profile(UUID.randomUUID()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void duplicateRegistrationReturnsConflict() throws Exception {
        when(authenticationService.register(any())).thenThrow(new EmailAlreadyExistsException("ada@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegistrationJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authenticationService.login(any())).thenReturn(new LoginResponse("jwt-token", profile(UUID.randomUUID())));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("ada@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void invalidRegistrationInputReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidCredentialsReturnUnauthorized() throws Exception {
        when(authenticationService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ada@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void meAndOtherApiEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenAuthenticatesMeWithoutAcceptingClientSelectedUserId() throws Exception {
        UUID tokenUserId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(authenticationService.getAuthenticatedUser(tokenUserId)).thenReturn(profile(tokenUserId));

        mockMvc.perform(get("/api/auth/me")
                        .param("userId", otherUserId.toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(tokenUserId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tokenUserId.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));

        verify(authenticationService).getAuthenticatedUser(tokenUserId);
    }

    @Test
    void invalidAndExpiredTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        String expiredToken = new JwtService(new com.invoiceflow.ai.security.JwtProperties(
                "test-only-jwt-secret-must-be-at-least-32-characters", -1)).generateToken(UUID.randomUUID());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/auth/change-password").contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("current-password", "updated-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordUsesAuthenticatedIdentityAndReturnsNoSensitiveData() throws Exception {
        UUID tokenUserId = UUID.randomUUID();
        mockMvc.perform(put("/api/auth/change-password").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + jwtService.generateToken(tokenUserId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("current-password", "updated-password")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Set-Cookie"));

        verify(authenticationService).changePassword(org.mockito.Mockito.eq(tokenUserId), any());
    }

    @Test
    void changePasswordRejectsInvalidOrIncorrectCredentials() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(put("/api/auth/change-password").header("Authorization", "Bearer " + jwtService.generateToken(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(changePasswordJson("current-password", "short")))
                .andExpect(status().isBadRequest());

        doThrow(new InvalidCredentialsException("Invalid current password.")).when(authenticationService)
                .changePassword(org.mockito.Mockito.eq(userId), any());
        mockMvc.perform(put("/api/auth/change-password").header("Authorization", "Bearer " + jwtService.generateToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordJson("incorrect-password", "updated-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid current password."));
    }

    private static UserProfileResponse profile(UUID userId) {
        return new UserProfileResponse(userId, "Ada", "Lovelace", "Analytical Engines", "ada@example.com",
                "+44 20 7946 0958", "United Kingdom", Instant.now(), Instant.now());
    }

    private static String validRegistrationJson() {
        return "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"companyName\":\"Analytical Engines\","
                + "\"email\":\"ada@example.com\",\"password\":\"correct-password\","
                + "\"phoneNumber\":\"+44 20 7946 0958\",\"country\":\"United Kingdom\"}";
    }

    private static String changePasswordJson(String currentPassword, String newPassword) {
        return "{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}";
    }
}

package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.UserService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/users/profile").contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserGetsOwnProfileAndClientUserIdIsIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(profile(userId));

        mockMvc.perform(get("/api/users/profile").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService).findById(userId);
    }

    @Test
    void authenticatedUserUpdatesOwnProfileAndClientUserIdIsIgnored() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.updateProfile(eq(userId), any())).thenReturn(profile(userId));

        mockMvc.perform(put("/api/users/profile").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)).contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(userService).updateProfile(eq(userId), any());
    }

    @Test
    void invalidProfileUpdateReturnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(put("/api/users/profile").header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingUserIsHandledAsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        mockMvc.perform(get("/api/users/profile").header("Authorization", token(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static UserProfileResponse profile(UUID userId) {
        return new UserProfileResponse(userId, "Ada", "Lovelace", "Analytical Engines", "ada@example.com",
                "+44 20 7946 0958", "United Kingdom", Instant.now(), Instant.now());
    }

    private static String request() {
        return "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"companyName\":\"Analytical Engines\","
                + "\"email\":\"ada@example.com\",\"phoneNumber\":\"+44 20 7946 0958\","
                + "\"country\":\"United Kingdom\"}";
    }
}

package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.auth.ChangePasswordRequest;
import com.invoiceflow.ai.dto.auth.LoginRequest;
import com.invoiceflow.ai.dto.auth.LoginResponse;
import com.invoiceflow.ai.dto.auth.RegistrationRequest;
import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.service.AuthenticationService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserProfileResponse register(@Valid @RequestBody RegistrationRequest request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal UUID userId) {
        return authenticationService.getAuthenticatedUser(userId);
    }

    @PutMapping("/change-password")
    public void changePassword(@AuthenticationPrincipal UUID userId,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(userId, request);
    }
}

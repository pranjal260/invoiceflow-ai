package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.auth.LoginRequest;
import com.invoiceflow.ai.dto.auth.LoginResponse;
import com.invoiceflow.ai.dto.auth.RegistrationRequest;
import com.invoiceflow.ai.dto.auth.ChangePasswordRequest;
import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.InvalidCredentialsException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.mapper.UserMapper;
import com.invoiceflow.ai.security.JwtService;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserProfileResponse register(RegistrationRequest request) {
        User user = new User(request.firstName(), request.lastName(), request.companyName(), request.email(),
                passwordEncoder.encode(request.password()), request.phoneNumber(), request.country());
        return userService.createUser(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user;
        try {
            user = userService.getUserByEmail(request.email());
        } catch (UserNotFoundException exception) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return new LoginResponse(jwtService.generateToken(user.getId()), UserMapper.toResponse(user));
    }

    public UserProfileResponse getAuthenticatedUser(UUID userId) {
        return userService.findById(userId);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userService.getUserById(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid current password.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        userService.updatePasswordHash(userId, passwordEncoder.encode(request.newPassword()));
    }
}

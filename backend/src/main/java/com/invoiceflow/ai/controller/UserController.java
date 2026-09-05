package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.user.UserProfileResponse;
import com.invoiceflow.ai.dto.user.UserProfileUpdateRequest;
import com.invoiceflow.ai.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public UserProfileResponse getProfile(@AuthenticationPrincipal UUID userId) {
        return userService.findById(userId);
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(@AuthenticationPrincipal UUID userId,
                                              @Valid @RequestBody UserProfileUpdateRequest request) {
        return userService.updateProfile(userId, request);
    }
}

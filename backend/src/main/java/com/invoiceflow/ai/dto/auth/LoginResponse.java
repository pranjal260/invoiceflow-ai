package com.invoiceflow.ai.dto.auth;

import com.invoiceflow.ai.dto.user.UserProfileResponse;

public record LoginResponse(String token, UserProfileResponse user) {
}

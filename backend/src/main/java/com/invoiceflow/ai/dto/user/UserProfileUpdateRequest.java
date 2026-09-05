package com.invoiceflow.ai.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 150) String companyName,
        @NotBlank @Email @Size(max = 254) String email,
        @Size(max = 30) String phoneNumber,
        @Size(max = 100) String country
) {
}

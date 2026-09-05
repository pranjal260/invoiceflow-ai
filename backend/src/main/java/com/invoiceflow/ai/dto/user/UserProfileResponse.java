package com.invoiceflow.ai.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(UUID id, String firstName, String lastName, String companyName, String email,
                                  String phoneNumber, String country, Instant createdAt, Instant updatedAt) {
}

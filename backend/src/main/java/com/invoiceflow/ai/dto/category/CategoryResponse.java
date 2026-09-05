package com.invoiceflow.ai.dto.category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(UUID id, UUID userId, String name, String description, Instant createdAt) {
}

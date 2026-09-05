package com.invoiceflow.ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for file storage.
 *
 * All values are backed by environment variables and must be provided at runtime.
 * No secrets are committed to source control.
 */
@Validated
@ConfigurationProperties(prefix = "storage")
public record StorageProperties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKeyId,
        @NotBlank String secretAccessKey,
        @Positive long presignedUrlExpirationMinutes
) {
}

package com.invoiceflow.ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Externalized configuration for the Gemini AI API.
 *
 * All values are backed by environment variables or application config.
 * The API key must never be committed, logged, or exposed through responses.
 */
@Validated
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        @NotBlank String apiKey,
        @NotBlank String model,
        @NotBlank String baseUrl
) {
}

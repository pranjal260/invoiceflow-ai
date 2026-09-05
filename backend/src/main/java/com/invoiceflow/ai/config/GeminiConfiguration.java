package com.invoiceflow.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.ai.service.ai.GeminiInvoiceExtractionService;
import com.invoiceflow.ai.service.ai.InvoiceExtractionService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configures the Gemini AI extraction infrastructure.
 *
 * Creates a RestClient scoped to the Gemini API base URL and wires it
 * into the extraction service implementation.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfiguration {

    @Bean
    public RestClient geminiRestClient(GeminiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public InvoiceExtractionService invoiceExtractionService(
            RestClient geminiRestClient, ObjectMapper objectMapper, GeminiProperties properties) {
        return new GeminiInvoiceExtractionService(geminiRestClient, objectMapper, properties);
    }
}

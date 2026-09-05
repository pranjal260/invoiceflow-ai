package com.invoiceflow.ai.service.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoiceflow.ai.config.GeminiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

/**
 * Real Gemini API integration test.
 *
 * This test is EXPLICITLY OPT-IN and NEVER runs during normal builds.
 * It requires both:
 * <ol>
 *   <li>System property: {@code -Dgemini.integration-test=true}</li>
 *   <li>Environment variable: {@code GEMINI_API_KEY} set to a valid key</li>
 * </ol>
 *
 * Run with:
 * <pre>
 *   mvn test -Dgemini.integration-test=true -Dtest=GeminiIntegrationTest
 * </pre>
 *
 * This consumes Gemini API quota. Use sparingly on the free tier.
 */
@EnabledIfSystemProperty(named = "gemini.integration-test", matches = "true")
class GeminiIntegrationTest {

    @Test
    void extractsStructuredDataFromRealGeminiApi() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[Gemini Integration] SKIPPED — GEMINI_API_KEY not set");
            return;
        }

        String model = System.getenv("GEMINI_MODEL") != null
                ? System.getenv("GEMINI_MODEL") : "gemini-3.5-flash-lite";

        GeminiProperties properties = new GeminiProperties(
                apiKey, model, "https://generativelanguage.googleapis.com");

        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        GeminiInvoiceExtractionService service = new GeminiInvoiceExtractionService(
                restClient, objectMapper, properties);

        // Use a realistic text-based invoice with clear, unambiguous values.
        // text/plain is supported by Gemini's multimodal API for testing.
        byte[] testDocument = ("""
                INVOICE
                
                Invoice Number: INV-2024-GEMINI-001
                Vendor: Gemini Test Corporation
                Date: 2024-06-15
                Due Date: 2024-07-15
                Currency: USD
                
                Description                Qty    Unit Price    Amount
                -----------------------------------------------------------
                Cloud API Access             3      $25.00       $75.00
                Integration Service          1      $50.00       $50.00
                
                Subtotal: $125.00
                Tax (10%): $12.50
                Total: $137.50
                """).getBytes();

        InvoiceExtractionResult result = service.extract(testDocument, "text/plain");

        System.out.println("[Gemini Integration] Model: " + model);
        System.out.println("[Gemini Integration] Extraction result:");
        System.out.println("  Invoice Number: " + result.invoiceNumber());
        System.out.println("  Vendor: " + result.vendorName());
        System.out.println("  Invoice Date: " + result.invoiceDate());
        System.out.println("  Due Date: " + result.dueDate());
        System.out.println("  Currency: " + result.currency());
        System.out.println("  Subtotal: " + result.subtotal());
        System.out.println("  Tax: " + result.taxAmount());
        System.out.println("  Total: " + result.totalAmount());
        System.out.println("  Items: " + (result.items() != null ? result.items().size() : 0));
        if (result.items() != null) {
            result.items().forEach(item ->
                    System.out.println("    - " + item.description()
                            + " x" + item.quantity()
                            + " @ " + item.unitPrice()
                            + " = " + item.amount()));
        }

        // Verify the result is parseable and non-null
        assertThat(result).isNotNull();

        // Verify key fields are extracted (AI may vary, but these are unambiguous)
        assertThat(result.invoiceNumber()).isNotBlank();
        assertThat(result.vendorName()).isNotBlank();
        assertThat(result.invoiceDate()).isNotNull();
        assertThat(result.currency()).isNotBlank();
        assertThat(result.totalAmount()).isNotNull();
        assertThat(result.totalAmount().doubleValue()).isGreaterThan(0);

        // Verify line items were extracted
        assertThat(result.items()).isNotNull();
        assertThat(result.items()).isNotEmpty();
        for (InvoiceExtractionResult.LineItem item : result.items()) {
            assertThat(item.description()).isNotBlank();
            assertThat(item.quantity()).isNotNull();
            assertThat(item.unitPrice()).isNotNull();
            assertThat(item.amount()).isNotNull();
        }

        System.out.println("[Gemini Integration] ALL ASSERTIONS PASSED");
    }
}

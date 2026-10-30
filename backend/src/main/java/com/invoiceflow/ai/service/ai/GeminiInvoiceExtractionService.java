package com.invoiceflow.ai.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.ai.config.GeminiProperties;
import com.invoiceflow.ai.exception.InvoiceExtractionException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Gemini implementation of {@link InvoiceExtractionService}.
 *
 * Uses the Gemini REST API with structured output (JSON schema) to extract
 * invoice data from documents. All Gemini-specific logic is encapsulated here.
 *
 * The API key is passed as a query parameter per Gemini API convention and is
 * never included in exception messages, logs, or API responses.
 */
public class GeminiInvoiceExtractionService implements InvoiceExtractionService {

    static final String EXTRACTION_PROMPT =
            "Extract all invoice data from this document. "
            + "Return dates in YYYY-MM-DD format (e.g., 2024-01-15). "
            + "Return currency as a three-letter ISO 4217 code such as USD, EUR, GBP, or INR; "
            + "never return a currency symbol. "
            + "If a field cannot be determined from the document, return null for that field. "
            + "For items, extract all line items with their description, quantity, unit price, and amount. "
            + "If there are no line items, return an empty array for items.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    public GeminiInvoiceExtractionService(RestClient restClient, ObjectMapper objectMapper,
                                           GeminiProperties properties) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public InvoiceExtractionResult extract(byte[] documentBytes, String contentType) {
        try {
            Map<String, Object> requestBody = buildRequest(documentBytes, contentType);

            String responseBody = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}",
                            properties.model(), properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResponse(responseBody);
        } catch (InvoiceExtractionException e) {
            throw e;
        } catch (RestClientException e) {
            throw new InvoiceExtractionException("Invoice extraction failed: AI provider request error.", e);
        } catch (Exception e) {
            throw new InvoiceExtractionException("Invoice extraction failed.", e);
        }
    }

    private Map<String, Object> buildRequest(byte[] documentBytes, String contentType) {
        String base64Data = Base64.getEncoder().encodeToString(documentBytes);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("contents", List.of(Map.of(
                "parts", List.of(
                        Map.of("text", EXTRACTION_PROMPT),
                        Map.of("inlineData", Map.of(
                                "mimeType", contentType,
                                "data", base64Data
                        ))
                )
        )));
        request.put("generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", buildResponseSchema()
        ));

        return request;
    }

    Map<String, Object> buildResponseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("invoiceNumber", Map.of("type", "STRING", "nullable", true));
        props.put("vendorName", Map.of("type", "STRING", "nullable", true));
        props.put("invoiceDate", Map.of("type", "STRING", "nullable", true));
        props.put("dueDate", Map.of("type", "STRING", "nullable", true));
        props.put("currency", Map.of("type", "STRING", "nullable", true));
        props.put("subtotal", Map.of("type", "NUMBER", "nullable", true));
        props.put("taxAmount", Map.of("type", "NUMBER", "nullable", true));
        props.put("totalAmount", Map.of("type", "NUMBER", "nullable", true));

        Map<String, Object> lineItemProps = new LinkedHashMap<>();
        lineItemProps.put("description", Map.of("type", "STRING"));
        lineItemProps.put("quantity", Map.of("type", "NUMBER"));
        lineItemProps.put("unitPrice", Map.of("type", "NUMBER"));
        lineItemProps.put("amount", Map.of("type", "NUMBER"));

        props.put("items", Map.of(
                "type", "ARRAY",
                "items", Map.of(
                        "type", "OBJECT",
                        "properties", lineItemProps
                )
        ));

        schema.put("properties", props);
        return schema;
    }

    InvoiceExtractionResult parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode textNode = root.at("/candidates/0/content/parts/0/text");

            if (textNode.isMissingNode() || textNode.asText().isEmpty()) {
                throw new InvoiceExtractionException("Empty extraction response from AI provider.");
            }

            return objectMapper.readValue(textNode.asText(), InvoiceExtractionResult.class);
        } catch (InvoiceExtractionException e) {
            throw e;
        } catch (Exception e) {
            throw new InvoiceExtractionException("Failed to parse AI extraction response.", e);
        }
    }
}

package com.invoiceflow.ai.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.invoiceflow.ai.config.GeminiProperties;
import com.invoiceflow.ai.exception.InvoiceExtractionException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiInvoiceExtractionServiceTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL = "gemini-3.5-flash-lite";
    private static final String API_KEY = "test-api-key";
    private static final String EXPECTED_URI = BASE_URL
            + "/v1beta/models/" + MODEL + ":generateContent?key=" + API_KEY;

    private MockRestServiceServer server;
    private GeminiInvoiceExtractionService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        GeminiProperties properties = new GeminiProperties(API_KEY, MODEL, BASE_URL);

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        service = new GeminiInvoiceExtractionService(restClient, objectMapper, properties);
    }

    @Test
    void extractsFullInvoiceFromSuccessfulResponse() {
        String extractedJson = """
                {"invoiceNumber":"INV-001","vendorName":"Acme Corp",\
                "invoiceDate":"2024-01-15","dueDate":"2024-02-15",\
                "currency":"USD","subtotal":100.00,"taxAmount":10.00,\
                "totalAmount":110.00,"items":[{"description":"Widget",\
                "quantity":2,"unitPrice":50.00,"amount":100.00}]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(geminiResponse(extractedJson), MediaType.APPLICATION_JSON));

        InvoiceExtractionResult result = service.extract(
                "%PDF-1.4 test content".getBytes(), "application/pdf");

        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
        assertThat(result.vendorName()).isEqualTo("Acme Corp");
        assertThat(result.invoiceDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2024, 2, 15));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.taxAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(result.items()).hasSize(1);

        InvoiceExtractionResult.LineItem item = result.items().get(0);
        assertThat(item.description()).isEqualTo("Widget");
        assertThat(item.quantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(item.unitPrice()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(item.amount()).isEqualByComparingTo(new BigDecimal("100.00"));

        server.verify();
    }

    @Test
    void handlesResponseWithNullOptionalFields() {
        String extractedJson = """
                {"invoiceNumber":"INV-002","vendorName":"Test Vendor",\
                "invoiceDate":"2024-03-01","dueDate":null,\
                "currency":"EUR","subtotal":500.00,"taxAmount":null,\
                "totalAmount":500.00,"items":[]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(geminiResponse(extractedJson), MediaType.APPLICATION_JSON));

        InvoiceExtractionResult result = service.extract(
                new byte[]{0x25, 0x50, 0x44, 0x46}, "application/pdf");

        assertThat(result.invoiceNumber()).isEqualTo("INV-002");
        assertThat(result.vendorName()).isEqualTo("Test Vendor");
        assertThat(result.invoiceDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(result.dueDate()).isNull();
        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.subtotal()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.taxAmount()).isNull();
        assertThat(result.totalAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(result.items()).isEmpty();

        server.verify();
    }

    @Test
    void handlesMultipleLineItems() {
        String extractedJson = """
                {"invoiceNumber":"INV-003","vendorName":"Multi Corp",\
                "invoiceDate":"2024-06-01","dueDate":"2024-07-01",\
                "currency":"INR","subtotal":300.00,"taxAmount":54.00,\
                "totalAmount":354.00,"items":[
                {"description":"Item A","quantity":1,"unitPrice":100.00,"amount":100.00},
                {"description":"Item B","quantity":2,"unitPrice":100.00,"amount":200.00}
                ]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(geminiResponse(extractedJson), MediaType.APPLICATION_JSON));

        InvoiceExtractionResult result = service.extract(
                new byte[]{0x25}, "application/pdf");

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).description()).isEqualTo("Item A");
        assertThat(result.items().get(1).description()).isEqualTo("Item B");

        server.verify();
    }

    @Test
    void wrapsServerErrorInExtractionException() {
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.extract(new byte[]{0x25}, "application/pdf"))
                .isInstanceOf(InvoiceExtractionException.class)
                .hasMessageContaining("AI provider request error");

        server.verify();
    }

    @Test
    void wrapsEmptyResponseInExtractionException() {
        String emptyResponse = """
                {"candidates":[{"content":{"parts":[{"text":""}]}}]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(emptyResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extract(new byte[]{0x25}, "application/pdf"))
                .isInstanceOf(InvoiceExtractionException.class)
                .hasMessageContaining("Empty extraction response");

        server.verify();
    }

    @Test
    void wrapsMalformedJsonResponseInExtractionException() {
        String malformedResponse = """
                {"candidates":[{"content":{"parts":[{"text":"not valid json"}]}}]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(malformedResponse, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extract(new byte[]{0x25}, "application/pdf"))
                .isInstanceOf(InvoiceExtractionException.class)
                .hasMessageContaining("Failed to parse");

        server.verify();
    }

    @Test
    void wrapsMissingCandidatesInExtractionException() {
        String noCandidates = """
                {"candidates":[]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withSuccess(noCandidates, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extract(new byte[]{0x25}, "application/pdf"))
                .isInstanceOf(InvoiceExtractionException.class)
                .hasMessageContaining("Empty extraction response");

        server.verify();
    }

    @Test
    void exceptionMessageNeverContainsApiKey() {
        server.expect(requestTo(EXPECTED_URI))
                .andRespond(withServerError());

        try {
            service.extract(new byte[]{0x25}, "application/pdf");
        } catch (InvoiceExtractionException e) {
            assertThat(e.getMessage()).doesNotContain(API_KEY);
            assertThat(e.getMessage()).doesNotContain("test-api-key");

            // Check full causal chain
            Throwable cause = e.getCause();
            while (cause != null) {
                assertThat(cause.getMessage()).doesNotContain(API_KEY);
                cause = cause.getCause();
            }
        }

        server.verify();
    }

    @Test
    void requestIncludesResponseSchemaForStructuredOutput() {
        String extractedJson = """
                {"invoiceNumber":null,"vendorName":null,"invoiceDate":null,\
                "dueDate":null,"currency":null,"subtotal":null,"taxAmount":null,\
                "totalAmount":null,"items":[]}""";

        server.expect(requestTo(EXPECTED_URI))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseSchema")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("responseMimeType")))
                .andRespond(withSuccess(geminiResponse(extractedJson), MediaType.APPLICATION_JSON));

        service.extract(new byte[]{0x25}, "application/pdf");

        server.verify();
    }

    @Test
    void responseSchemaContainsAllExpectedFields() {
        Map<String, Object> schema = service.buildResponseSchema();

        assertThat(schema).containsKey("type");
        assertThat(schema.get("type")).isEqualTo("OBJECT");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsKeys(
                "invoiceNumber", "vendorName", "invoiceDate", "dueDate",
                "currency", "subtotal", "taxAmount", "totalAmount", "items");
    }

    @Test
    void extractionResultRecordStructureIsCorrect() {
        InvoiceExtractionResult result = new InvoiceExtractionResult(
                "INV-001", "Vendor", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                "USD", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.TEN,
                List.of(new InvoiceExtractionResult.LineItem("Item", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN)));

        assertThat(result.invoiceNumber()).isEqualTo("INV-001");
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).description()).isEqualTo("Item");
    }

    /**
     * Builds a Gemini API response envelope wrapping the given extracted JSON text.
     */
    private String geminiResponse(String extractedJson) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "candidates", List.of(Map.of(
                            "content", Map.of(
                                    "parts", List.of(Map.of("text", extractedJson))
                            )
                    ))
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to build test response", e);
        }
    }
}

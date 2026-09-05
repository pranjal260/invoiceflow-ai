package com.invoiceflow.ai.controller;

import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.exception.DocumentNotFoundException;
import com.invoiceflow.ai.exception.InvoiceExtractionException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.InvoiceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceExtractionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void extractionRequiresAuthentication() throws Exception {
        UUID invoiceId = UUID.randomUUID();

        mockMvc.perform(post("/api/invoices/{id}/extract", invoiceId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void extractionSucceedsForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.extractInvoice(invoiceId, userId))
                .thenReturn(invoice(userId, "INV-EXTRACTED"));

        mockMvc.perform(post("/api/invoices/{id}/extract", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-EXTRACTED"))
                .andExpect(jsonPath("$.vendorName").value("Extracted Vendor"));
    }

    @Test
    void extractionReturns404ForInaccessibleInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.extractInvoice(invoiceId, userId))
                .thenThrow(new InvoiceNotFoundException(invoiceId));

        mockMvc.perform(post("/api/invoices/{id}/extract", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void extractionReturns404WhenNoDocument() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.extractInvoice(invoiceId, userId))
                .thenThrow(new DocumentNotFoundException(invoiceId));

        mockMvc.perform(post("/api/invoices/{id}/extract", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void extractionReturns502OnAiFailure() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.extractInvoice(invoiceId, userId))
                .thenThrow(new InvoiceExtractionException("Invoice extraction failed."));

        mockMvc.perform(post("/api/invoices/{id}/extract", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static InvoiceResponse invoice(UUID userId, String number) {
        return new InvoiceResponse(UUID.randomUUID(), userId, null, number, "Extracted Vendor",
                LocalDate.of(2024, 1, 15), null, InvoiceStatus.PENDING, new BigDecimal("100.00"),
                new BigDecimal("10.00"), new BigDecimal("110.00"), "USD", null, "invoices/key/doc.pdf",
                Instant.now(), Instant.now());
    }
}

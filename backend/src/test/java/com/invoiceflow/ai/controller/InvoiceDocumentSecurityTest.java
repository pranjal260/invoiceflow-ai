package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.invoice.DocumentResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.dto.invoice.InvoiceUploadPreviewResponse;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.exception.DocumentNotFoundException;
import com.invoiceflow.ai.exception.EmptyFileException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.exception.UnsupportedFileTypeException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.InvoiceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceDocumentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void documentEndpointsRequireAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = pdfFile();

        mockMvc.perform(multipart("/api/invoices/{id}/document", id).file(file))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/invoices/{id}/document", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/invoices/{id}/document", id))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadFirstExtractionReturnsPreviewWithoutCreatingInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        when(invoiceService.uploadAndExtract(eq(userId), any(), eq("application/pdf")))
                .thenReturn(new InvoiceUploadPreviewResponse("INV-AI-001", "Example Vendor",
                        LocalDate.of(2026, 8, 10), null, new BigDecimal("200.00"),
                        new BigDecimal("36.00"), new BigDecimal("236.00"), "INR", List.of(),
                        "invoices/" + userId + "/preview.pdf"));

        mockMvc.perform(multipart("/api/invoices/upload")
                        .file(pdfFile())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-AI-001"))
                .andExpect(jsonPath("$.fileUrl").value("invoices/" + userId + "/preview.pdf"));
        verify(invoiceService).uploadAndExtract(eq(userId), any(), eq("application/pdf"));
    }

    @Test
    void uploadsDocumentSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.uploadDocument(eq(invoiceId), eq(userId), any(), eq("application/pdf")))
                .thenReturn(invoice(userId, "INV-001", "invoices/" + userId + "/test.pdf"));

        mockMvc.perform(multipart("/api/invoices/{id}/document", invoiceId)
                        .file(pdfFile())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$.fileUrl").value("invoices/" + userId + "/test.pdf"));
        verify(invoiceService).uploadDocument(eq(invoiceId), eq(userId), any(), eq("application/pdf"));
    }

    @Test
    void uploadToInaccessibleInvoiceReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.uploadDocument(eq(invoiceId), eq(userId), any(), any()))
                .thenThrow(new InvoiceNotFoundException(invoiceId));

        mockMvc.perform(multipart("/api/invoices/{id}/document", invoiceId)
                        .file(pdfFile())
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void uploadInvalidFileTypeReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.uploadDocument(eq(invoiceId), eq(userId), any(), eq("text/plain")))
                .thenThrow(new UnsupportedFileTypeException("text/plain"));

        MockMultipartFile textFile = new MockMultipartFile("file", "test.txt",
                "text/plain", "plain text".getBytes());

        mockMvc.perform(multipart("/api/invoices/{id}/document", invoiceId)
                        .file(textFile)
                        .header("Authorization", token(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadEmptyFileReturns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.uploadDocument(eq(invoiceId), eq(userId), any(), any()))
                .thenThrow(new EmptyFileException());

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf",
                "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/invoices/{id}/document", invoiceId)
                        .file(emptyFile)
                        .header("Authorization", token(userId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsDocumentSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.getDocumentUrl(invoiceId, userId))
                .thenReturn(new DocumentResponse(invoiceId, "https://presigned-url"));

        mockMvc.perform(get("/api/invoices/{id}/document", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.url").value("https://presigned-url"));
    }

    @Test
    void getDocumentReturns404WhenNoDocumentAttached() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.getDocumentUrl(invoiceId, userId))
                .thenThrow(new DocumentNotFoundException(invoiceId));

        mockMvc.perform(get("/api/invoices/{id}/document", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletesDocumentSuccessfully() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/invoices/{id}/document", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNoContent());
        verify(invoiceService).deleteDocument(invoiceId, userId);
    }

    @Test
    void deleteDocumentReturns404WhenNoDocumentAttached() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        doThrow(new DocumentNotFoundException(invoiceId))
                .when(invoiceService).deleteDocument(invoiceId, userId);

        mockMvc.perform(delete("/api/invoices/{id}/document", invoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadReplacesExistingDocument() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        String newKey = "invoices/" + userId + "/new.pdf";
        when(invoiceService.uploadDocument(eq(invoiceId), eq(userId), any(), eq("application/pdf")))
                .thenReturn(invoice(userId, "INV-001", newKey));

        mockMvc.perform(multipart("/api/invoices/{id}/document", invoiceId)
                        .file(pdfFile())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value(newKey));
    }

    @Test
    void userCannotAccessAnotherUsersDocument() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserInvoiceId = UUID.randomUUID();
        when(invoiceService.getDocumentUrl(otherUserInvoiceId, userId))
                .thenThrow(new InvoiceNotFoundException(otherUserInvoiceId));

        mockMvc.perform(get("/api/invoices/{id}/document", otherUserInvoiceId)
                        .header("Authorization", token(userId)))
                .andExpect(status().isNotFound());
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "invoice.pdf",
                "application/pdf", "%PDF-1.4 test content".getBytes());
    }

    private static InvoiceResponse invoice(UUID userId, String number, String fileUrl) {
        return new InvoiceResponse(UUID.randomUUID(), userId, null, number, "Example Vendor",
                LocalDate.of(2026, 8, 10), null, InvoiceStatus.PENDING, new BigDecimal("200.00"),
                new BigDecimal("36.00"), new BigDecimal("236.00"), "INR", "Notes", fileUrl,
                Instant.now(), Instant.now());
    }
}

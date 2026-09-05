package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.invoice.InvoiceResponse;
import com.invoiceflow.ai.entity.InvoiceStatus;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DuplicateInvoiceNumberException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.InvoiceService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    void allInvoiceEndpointsRequireAuthentication() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(get("/api/invoices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/invoices/{id}", id)).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/invoices").contentType(MediaType.APPLICATION_JSON).content(request("INV-001")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/invoices/{id}", id).contentType(MediaType.APPLICATION_JSON).content(request("INV-001")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/invoices/{id}", id)).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/invoices/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PAID\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsOnlyAuthenticatedUsersInvoices() throws Exception {
        UUID userId = UUID.randomUUID();
        when(invoiceService.findAllByUserId(userId)).thenReturn(List.of(invoice(userId, "INV-001", InvoiceStatus.PENDING)));

        mockMvc.perform(get("/api/invoices").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invoiceNumber").value("INV-001"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist());
        verify(invoiceService).findAllByUserId(userId);
    }

    @Test
    void getsOwnedInvoiceAndReturnsNotFoundForInaccessibleInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.findByIdForUser(invoiceId, userId)).thenReturn(invoice(userId, "INV-001", InvoiceStatus.PENDING));
        mockMvc.perform(get("/api/invoices/{id}", invoiceId).header("Authorization", token(userId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.invoiceNumber").value("INV-001"));

        UUID inaccessibleId = UUID.randomUUID();
        when(invoiceService.findByIdForUser(inaccessibleId, userId)).thenThrow(new InvoiceNotFoundException(inaccessibleId));
        mockMvc.perform(get("/api/invoices/{id}", inaccessibleId).header("Authorization", token(userId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createsInvoiceForAuthenticatedUserAndRejectsInvalidOrDuplicateRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        when(invoiceService.createInvoice(eq(userId), any())).thenReturn(invoice(userId, "INV-001", InvoiceStatus.PENDING));
        mockMvc.perform(post("/api/invoices").param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)).contentType(MediaType.APPLICATION_JSON).content(request("INV-001")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.invoiceNumber").value("INV-001"));
        verify(invoiceService).createInvoice(eq(userId), any());

        mockMvc.perform(post("/api/invoices").header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        when(invoiceService.createInvoice(eq(userId), any())).thenThrow(new DuplicateInvoiceNumberException("INV-001"));
        mockMvc.perform(post("/api/invoices").header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(request("INV-001")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void rejectsCrossUserCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(invoiceService.createInvoice(eq(userId), any())).thenThrow(new CategoryNotFoundException(categoryId));
        mockMvc.perform(post("/api/invoices").header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(requestWithCategory("INV-001", categoryId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updatesInvoiceAndHandlesValidationOwnershipAndDuplicates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.updateInvoice(eq(invoiceId), eq(userId), any()))
                .thenReturn(invoice(userId, "INV-002", InvoiceStatus.PAID));
        mockMvc.perform(put("/api/invoices/{id}", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(request("INV-002")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(put("/api/invoices/{id}", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        when(invoiceService.updateInvoice(eq(invoiceId), eq(userId), any())).thenThrow(new InvoiceNotFoundException(invoiceId));
        mockMvc.perform(put("/api/invoices/{id}", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(request("INV-002")))
                .andExpect(status().isNotFound());

        when(invoiceService.updateInvoice(eq(invoiceId), eq(userId), any()))
                .thenThrow(new DuplicateInvoiceNumberException("INV-002"));
        mockMvc.perform(put("/api/invoices/{id}", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(request("INV-002")))
                .andExpect(status().isConflict());
    }

    @Test
    void deletesOwnedInvoiceAndRejectsInaccessibleInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        mockMvc.perform(delete("/api/invoices/{id}", invoiceId).header("Authorization", token(userId)))
                .andExpect(status().isNoContent());
        verify(invoiceService).deleteInvoice(invoiceId, userId);

        UUID inaccessibleId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new InvoiceNotFoundException(inaccessibleId))
                .when(invoiceService).deleteInvoice(inaccessibleId, userId);
        mockMvc.perform(delete("/api/invoices/{id}", inaccessibleId).header("Authorization", token(userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatesStatusAndRejectsInvalidOrInaccessibleStatusUpdates() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceService.updateStatus(invoiceId, userId, InvoiceStatus.PAID))
                .thenReturn(invoice(userId, "INV-001", InvoiceStatus.PAID));
        mockMvc.perform(patch("/api/invoices/{id}/status", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));

        mockMvc.perform(patch("/api/invoices/{id}/status", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());

        when(invoiceService.updateStatus(invoiceId, userId, InvoiceStatus.OVERDUE))
                .thenThrow(new InvoiceNotFoundException(invoiceId));
        mockMvc.perform(patch("/api/invoices/{id}/status", invoiceId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OVERDUE\"}"))
                .andExpect(status().isNotFound());
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static InvoiceResponse invoice(UUID userId, String number, InvoiceStatus status) {
        return new InvoiceResponse(UUID.randomUUID(), userId, null, number, "Example Vendor", LocalDate.of(2026, 8, 10),
                null, status, new BigDecimal("200.00"), new BigDecimal("36.00"), new BigDecimal("236.00"), "INR",
                "Notes", null, Instant.now(), Instant.now());
    }

    private static String request(String number) {
        return "{\"invoiceNumber\":\"" + number + "\",\"vendorName\":\"Example Vendor\","
                + "\"invoiceDate\":\"2026-08-10\",\"status\":\"PENDING\",\"subtotal\":200.00,"
                + "\"taxAmount\":36.00,\"totalAmount\":236.00,\"currency\":\"INR\","
                + "\"notes\":\"Notes\",\"direction\":\"EXPENSE\"}";
    }

    private static String requestWithCategory(String number, UUID categoryId) {
        return request(number).replace("{", "{\"categoryId\":\"" + categoryId + "\",");
    }
}

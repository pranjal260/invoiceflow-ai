package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.invoiceflow.ai.dto.invoiceitem.InvoiceItemResponse;
import com.invoiceflow.ai.exception.InvoiceItemNotFoundException;
import com.invoiceflow.ai.exception.InvoiceNotFoundException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.InvoiceItemService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InvoiceItemControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private InvoiceItemService invoiceItemService;

    @Test
    void allInvoiceItemEndpointsRequireAuthentication() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(get(path(invoiceId))).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path(invoiceId) + "/{itemId}", itemId)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path(invoiceId)).contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(path(invoiceId) + "/{itemId}", itemId).contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(path(invoiceId) + "/{itemId}", itemId)).andExpect(status().isUnauthorized());
    }

    @Test
    void listsAndRetrievesItemsForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        InvoiceItemResponse item = item(itemId, invoiceId);
        when(invoiceItemService.findAllByInvoiceId(invoiceId, userId)).thenReturn(List.of(item));
        when(invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId)).thenReturn(item);

        mockMvc.perform(get(path(invoiceId)).param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$[0].description").value("Design work"));
        mockMvc.perform(get(path(invoiceId) + "/{itemId}", itemId).header("Authorization", token(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()));

        verify(invoiceItemService).findAllByInvoiceId(invoiceId, userId);
        verify(invoiceItemService).findByIdForInvoice(itemId, invoiceId, userId);
    }

    @Test
    void createsItemForAuthenticatedUserAndRejectsInvalidRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        when(invoiceItemService.createInvoiceItem(eq(invoiceId), eq(userId), any()))
                .thenReturn(item(UUID.randomUUID(), invoiceId));

        mockMvc.perform(post(path(invoiceId)).param("userId", UUID.randomUUID().toString())
                        .header("Authorization", token(userId)).contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Design work"));
        verify(invoiceItemService).createInvoiceItem(eq(invoiceId), eq(userId), any());

        mockMvc.perform(post(path(invoiceId)).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesAndDeletesItemsForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceItemService.updateInvoiceItem(eq(itemId), eq(invoiceId), eq(userId), any()))
                .thenReturn(item(itemId, invoiceId));

        mockMvc.perform(put(path(invoiceId) + "/{itemId}", itemId).header("Authorization", token(userId))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(itemId.toString()));
        verify(invoiceItemService).updateInvoiceItem(eq(itemId), eq(invoiceId), eq(userId), any());

        mockMvc.perform(delete(path(invoiceId) + "/{itemId}", itemId).header("Authorization", token(userId)))
                .andExpect(status().isNoContent());
        verify(invoiceItemService).deleteInvoiceItem(itemId, invoiceId, userId);
    }

    @Test
    void returnsNotFoundForInaccessibleInvoiceOrItem() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(invoiceItemService.findAllByInvoiceId(invoiceId, userId)).thenThrow(new InvoiceNotFoundException(invoiceId));
        when(invoiceItemService.findByIdForInvoice(itemId, invoiceId, userId))
                .thenThrow(new InvoiceItemNotFoundException(itemId));
        doThrow(new InvoiceItemNotFoundException(itemId)).when(invoiceItemService)
                .deleteInvoiceItem(itemId, invoiceId, userId);

        mockMvc.perform(get(path(invoiceId)).header("Authorization", token(userId)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get(path(invoiceId) + "/{itemId}", itemId).header("Authorization", token(userId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(path(invoiceId) + "/{itemId}", itemId).header("Authorization", token(userId)))
                .andExpect(status().isNotFound());
    }

    private String token(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static String path(UUID invoiceId) {
        return "/api/invoices/" + invoiceId + "/items";
    }

    private static InvoiceItemResponse item(UUID itemId, UUID invoiceId) {
        return new InvoiceItemResponse(itemId, invoiceId, "Design work", new BigDecimal("2.000"),
                new BigDecimal("125.00"), new BigDecimal("250.00"));
    }

    private static String request() {
        return "{\"description\":\"Design work\",\"quantity\":2.000,\"unitPrice\":125.00,\"amount\":250.00}";
    }
}

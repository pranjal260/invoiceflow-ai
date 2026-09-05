package com.invoiceflow.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.category.CategoryResponse;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DuplicateCategoryNameException;
import com.invoiceflow.ai.security.JwtService;
import com.invoiceflow.ai.service.CategoryService;
import java.time.Instant;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void allCategoryEndpointsRequireAuthentication() throws Exception {
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(get("/api/categories")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON).content(validRequest()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/categories/{id}", categoryId)).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserReceivesOnlyTheirCategories() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(categoryService.findAllByUserId(userId)).thenReturn(List.of(category(userId, "Software")));

        mockMvc.perform(get("/api/categories")
                        .param("userId", otherUserId.toString())
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Software"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist());

        verify(categoryService).findAllByUserId(userId);
    }

    @Test
    void createsCategoryForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(categoryService.createCategory(eq(userId), any())).thenReturn(category(userId, "Software"));

        mockMvc.perform(post("/api/categories")
                        .param("userId", UUID.randomUUID().toString())
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Software"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(categoryService).createCategory(eq(userId), any());
    }

    @Test
    void rejectsInvalidCategoryCreation() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearerToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsDuplicateCategoryName() throws Exception {
        UUID userId = UUID.randomUUID();
        when(categoryService.createCategory(eq(userId), any()))
                .thenThrow(new DuplicateCategoryNameException("Software"));

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void updatesOwnedCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(categoryService.updateCategory(eq(categoryId), eq(userId), any()))
                .thenReturn(category(userId, "Infrastructure"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Infrastructure\",\"description\":\"Cloud services\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Infrastructure"));
    }

    @Test
    void rejectsInvalidCategoryUpdate() throws Exception {
        mockMvc.perform(put("/api/categories/{id}", UUID.randomUUID())
                        .header("Authorization", bearerToken(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void returnsNotFoundForMissingOrInaccessibleCategoryUpdate() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(categoryService.updateCategory(eq(categoryId), eq(userId), any()))
                .thenThrow(new CategoryNotFoundException(categoryId));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerToken(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deletesOwnedCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(categoryId, userId);
    }

    @Test
    void returnsNotFoundForMissingOrInaccessibleCategoryDeletion() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new CategoryNotFoundException(categoryId))
                .when(categoryService).deleteCategory(categoryId, userId);

        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .header("Authorization", bearerToken(userId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private String bearerToken(UUID userId) {
        return "Bearer " + jwtService.generateToken(userId);
    }

    private static CategoryResponse category(UUID userId, String name) {
        return new CategoryResponse(UUID.randomUUID(), userId, name, "Category description", Instant.now());
    }

    private static String validRequest() {
        return "{\"name\":\"Software\",\"description\":\"Subscriptions\"}";
    }
}

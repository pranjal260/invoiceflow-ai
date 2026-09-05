package com.invoiceflow.ai.controller;

import com.invoiceflow.ai.dto.category.CategoryCreateRequest;
import com.invoiceflow.ai.dto.category.CategoryResponse;
import com.invoiceflow.ai.dto.category.CategoryUpdateRequest;
import com.invoiceflow.ai.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> findAll(@AuthenticationPrincipal UUID userId) {
        return categoryService.findAllByUserId(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@AuthenticationPrincipal UUID userId,
                                   @Valid @RequestBody CategoryCreateRequest request) {
        return categoryService.createCategory(userId, request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @AuthenticationPrincipal UUID userId,
                                   @Valid @RequestBody CategoryUpdateRequest request) {
        return categoryService.updateCategory(id, userId, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UUID userId) {
        categoryService.deleteCategory(id, userId);
    }
}

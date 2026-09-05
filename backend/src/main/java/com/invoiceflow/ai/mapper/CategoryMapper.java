package com.invoiceflow.ai.mapper;

import com.invoiceflow.ai.dto.category.CategoryCreateRequest;
import com.invoiceflow.ai.dto.category.CategoryResponse;
import com.invoiceflow.ai.dto.category.CategoryUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.User;

public final class CategoryMapper {
    private CategoryMapper() { }

    public static Category toEntity(CategoryCreateRequest request, User user) {
        return new Category(user, request.name(), request.description());
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getUser().getId(), category.getName(),
                category.getDescription(), category.getCreatedAt());
    }

    public static void updateEntity(CategoryUpdateRequest request, Category category) {
        category.updateDetails(request.name(), request.description());
    }
}

package com.invoiceflow.ai.service;

import com.invoiceflow.ai.dto.category.CategoryCreateRequest;
import com.invoiceflow.ai.dto.category.CategoryResponse;
import com.invoiceflow.ai.dto.category.CategoryUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DuplicateCategoryNameException;
import com.invoiceflow.ai.exception.UserNotFoundException;
import com.invoiceflow.ai.mapper.CategoryMapper;
import com.invoiceflow.ai.repository.CategoryRepository;
import com.invoiceflow.ai.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    public List<CategoryResponse> findAllByUserId(UUID userId) {
        return categoryRepository.findAllByUserId(userId).stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    public CategoryResponse findByIdForUser(UUID categoryId, UUID userId) {
        return CategoryMapper.toResponse(getCategoryForUser(categoryId, userId));
    }

    public CategoryResponse createCategory(UUID userId, CategoryCreateRequest request) {
        if (categoryNameExists(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        Category category = CategoryMapper.toEntity(request, user);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse updateCategory(UUID categoryId, UUID userId, CategoryUpdateRequest request) {
        Category category = getCategoryForUser(categoryId, userId);
        if (!category.getName().equals(request.name()) && categoryNameExists(userId, request.name())) {
            throw new DuplicateCategoryNameException(request.name());
        }

        CategoryMapper.updateEntity(request, category);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    public void deleteCategory(UUID categoryId, UUID userId) {
        categoryRepository.delete(getCategoryForUser(categoryId, userId));
    }

    public boolean categoryNameExists(UUID userId, String name) {
        return categoryRepository.existsByUserIdAndName(userId, name);
    }

    private Category getCategoryForUser(UUID categoryId, UUID userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}

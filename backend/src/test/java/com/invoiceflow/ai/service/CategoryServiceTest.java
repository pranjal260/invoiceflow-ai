package com.invoiceflow.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoiceflow.ai.dto.category.CategoryCreateRequest;
import com.invoiceflow.ai.dto.category.CategoryResponse;
import com.invoiceflow.ai.dto.category.CategoryUpdateRequest;
import com.invoiceflow.ai.entity.Category;
import com.invoiceflow.ai.entity.User;
import com.invoiceflow.ai.exception.CategoryNotFoundException;
import com.invoiceflow.ai.exception.DuplicateCategoryNameException;
import com.invoiceflow.ai.repository.CategoryRepository;
import com.invoiceflow.ai.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, userRepository);
    }

    @Test
    void listsOnlyCategoriesForUser() {
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findAllByUserId(userId)).thenReturn(List.of(category("Software"), category("Travel")));

        List<CategoryResponse> responses = categoryService.findAllByUserId(userId);

        assertThat(responses).extracting(CategoryResponse::name).containsExactly("Software", "Travel");
        verify(categoryRepository).findAllByUserId(userId);
    }

    @Test
    void findsCategoryWhenOwnedByUser() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category("Software")));

        CategoryResponse response = categoryService.findByIdForUser(categoryId, userId);

        assertThat(response.name()).isEqualTo("Software");
    }

    @Test
    void rejectsAccessToAnotherUsersCategory() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findByIdForUser(categoryId, userId))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void createsCategoryForExistingUser() {
        UUID userId = UUID.randomUUID();
        User user = user();
        CategoryCreateRequest request = new CategoryCreateRequest("Software", "Subscriptions");
        when(categoryRepository.existsByUserIdAndName(userId, "Software")).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.createCategory(userId, request);

        assertThat(response.name()).isEqualTo("Software");
        assertThat(response.description()).isEqualTo("Subscriptions");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void rejectsDuplicateNameForSameUser() {
        UUID userId = UUID.randomUUID();
        CategoryCreateRequest request = new CategoryCreateRequest("Software", "Subscriptions");
        when(categoryRepository.existsByUserIdAndName(userId, "Software")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(userId, request))
                .isInstanceOf(DuplicateCategoryNameException.class);
        verify(userRepository, never()).findById(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void updatesOwnedCategory() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Category category = category("Software");
        CategoryUpdateRequest request = new CategoryUpdateRequest("Infrastructure", "Cloud services");
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByUserIdAndName(userId, "Infrastructure")).thenReturn(false);
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.updateCategory(categoryId, userId, request);

        assertThat(response.name()).isEqualTo("Infrastructure");
        assertThat(response.description()).isEqualTo("Cloud services");
        verify(categoryRepository).save(category);
    }

    @Test
    void deletesOwnedCategory() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Category category = category("Software");
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.of(category));

        categoryService.deleteCategory(categoryId, userId);

        verify(categoryRepository).delete(category);
    }

    @Test
    void throwsWhenUpdatingMissingOrInaccessibleCategory() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(categoryRepository.findByIdAndUserId(categoryId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, userId,
                new CategoryUpdateRequest("Software", "Subscriptions")))
                .isInstanceOf(CategoryNotFoundException.class);
        verify(categoryRepository, never()).save(any());
    }

    private static Category category(String name) {
        return new Category(user(), name, "Category description");
    }

    private static User user() {
        return new User("Ada", "Lovelace", "Analytical Engines", "ada@example.com", "password-hash",
                "+44 20 7946 0958", "United Kingdom");
    }
}

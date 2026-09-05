import { apiClient } from "@/services/api-client";
import type { Category } from "@/types/api";

export type CategoryRequest = { name: string; description?: string };

export const categoryService = {
  getCategories: () => apiClient.get<Category[]>("/api/categories"),
  createCategory: (request: CategoryRequest) => apiClient.post<Category>("/api/categories", request),
  updateCategory: (categoryId: string, request: CategoryRequest) => apiClient.put<Category>(`/api/categories/${categoryId}`, request),
  deleteCategory: (categoryId: string) => apiClient.delete(`/api/categories/${categoryId}`),
};

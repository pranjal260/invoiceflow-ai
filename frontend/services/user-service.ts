import { apiClient } from "@/services/api-client";
import type { UserProfile } from "@/types/api";

export type UserProfileUpdateRequest = {
  firstName: string;
  lastName: string;
  companyName?: string;
  email: string;
  phoneNumber?: string;
  country?: string;
};

export const userService = {
  getProfile: () => apiClient.get<UserProfile>("/api/users/profile"),
  updateProfile: (request: UserProfileUpdateRequest) => apiClient.put<UserProfile>("/api/users/profile", request),
};

import { apiClient } from "@/services/api-client";
import type { LoginResponse, UserProfile } from "@/types/api";

export type LoginRequest = { email: string; password: string };
export type RegistrationRequest = {
  firstName: string;
  lastName: string;
  companyName?: string;
  email: string;
  password: string;
  phoneNumber?: string;
  country?: string;
};

export type ChangePasswordRequest = { currentPassword: string; newPassword: string };

export const authService = {
  login: (request: LoginRequest) => apiClient.post<LoginResponse>("/api/auth/login", request),
  register: (request: RegistrationRequest) => apiClient.post<UserProfile>("/api/auth/register", request),
  me: () => apiClient.get<UserProfile>("/api/auth/me"),
  changePassword: (request: ChangePasswordRequest) => apiClient.put<void>("/api/auth/change-password", request),
};

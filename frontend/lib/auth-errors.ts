import { ApiClientError } from "@/services/api-client";

export function getAuthErrorMessage(error: unknown) {
  if (error instanceof ApiClientError) {
    if (error.details.status === 401) {
      return "Your email or password is incorrect.";
    }
    return error.details.message;
  }

  return "Something went wrong. Please try again.";
}

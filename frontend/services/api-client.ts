import type { ApiErrorResponse } from "@/types/api";

const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type UnauthorizedHandler = () => void;

export class ApiClientError extends Error {
  readonly details: ApiErrorResponse;

  constructor(details: ApiErrorResponse) {
    super(details.message);
    this.name = "ApiClientError";
    this.details = details;
  }
}

let accessToken: string | null = null;
let unauthorizedHandler: UnauthorizedHandler | null = null;

export function setApiAccessToken(token: string | null) {
  accessToken = token;
}

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler;
}

function createHeaders(body: BodyInit | null | undefined, headers?: HeadersInit) {
  const result = new Headers(headers);
  if (accessToken) {
    result.set("Authorization", `Bearer ${accessToken}`);
  }
  if (body && !(body instanceof FormData) && !result.has("Content-Type")) {
    result.set("Content-Type", "application/json");
  }
  return result;
}

function fallbackError(response: Response): ApiErrorResponse {
  return {
    timestamp: new Date().toISOString(),
    status: response.status,
    error: response.statusText || "Request failed",
    message: "The request could not be completed.",
    path: response.url,
  };
}

async function readError(response: Response): Promise<ApiErrorResponse> {
  try {
    return (await response.json()) as ApiErrorResponse;
  } catch {
    return fallbackError(response);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers: createHeaders(init.body, init.headers),
    });
  } catch {
    throw new ApiClientError({
      timestamp: new Date().toISOString(),
      status: 0,
      error: "Network error",
      message: "Unable to reach InvoiceFlow AI. Please try again.",
      path,
    });
  }

  if (response.status === 401) {
    unauthorizedHandler?.();
  }

  if (!response.ok) {
    throw new ApiClientError(await readError(response));
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) => request<T>(path, {
    method: "POST",
    body: body === undefined ? undefined : JSON.stringify(body),
  }),
  put: <T>(path: string, body?: unknown) => request<T>(path, {
    method: "PUT",
    body: body === undefined ? undefined : JSON.stringify(body),
  }),
  patch: <T>(path: string, body?: unknown) => request<T>(path, {
    method: "PATCH",
    body: body === undefined ? undefined : JSON.stringify(body),
  }),
  delete: <T = void>(path: string) => request<T>(path, { method: "DELETE" }),
  upload: <T>(path: string, formData: FormData) => request<T>(path, { method: "POST", body: formData }),
};

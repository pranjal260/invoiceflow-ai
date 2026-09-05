"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { setApiAccessToken, setUnauthorizedHandler } from "@/services/api-client";
import { authService, type LoginRequest, type RegistrationRequest } from "@/services/auth-service";
import type { UserProfile } from "@/types/api";

const SESSION_TOKEN_KEY = "invoiceflow.auth.token";

type AuthContextValue = {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  register: (request: RegistrationRequest) => Promise<UserProfile>;
  logout: () => void;
  refreshUser: () => Promise<UserProfile>;
  updateUser: (user: UserProfile) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const clearAuthentication = useCallback(() => {
    sessionStorage.removeItem(SESSION_TOKEN_KEY);
    setApiAccessToken(null);
    setToken(null);
    setUser(null);
  }, []);

  const refreshUser = useCallback(async () => {
    const currentUser = await authService.me();
    setUser(currentUser);
    return currentUser;
  }, []);

  useEffect(() => {
    let active = true;

    const handleUnauthorized = () => {
      clearAuthentication();
      router.replace("/login");
    };

    setUnauthorizedHandler(handleUnauthorized);
    const storedToken = sessionStorage.getItem(SESSION_TOKEN_KEY);
    if (!storedToken) {
      setIsLoading(false);
      return () => {
        active = false;
        setUnauthorizedHandler(null);
      };
    }

    setApiAccessToken(storedToken);
    setToken(storedToken);
    void authService.me()
      .then((currentUser) => {
        if (active) {
          setUser(currentUser);
        }
      })
      .catch(() => {
        if (active) {
          clearAuthentication();
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });

    return () => {
      active = false;
      setUnauthorizedHandler(null);
    };
  }, [clearAuthentication, router]);

  const login = useCallback(async (request: LoginRequest) => {
    const response = await authService.login(request);
    sessionStorage.setItem(SESSION_TOKEN_KEY, response.token);
    setApiAccessToken(response.token);
    setToken(response.token);
    setUser(response.user);
  }, []);

  const logout = useCallback(() => {
    clearAuthentication();
    router.replace("/login");
  }, [clearAuthentication, router]);

  const updateUser = useCallback((updatedUser: UserProfile) => {
    setUser(updatedUser);
  }, []);

  const value = useMemo<AuthContextValue>(() => ({
    user,
    token,
    isAuthenticated: Boolean(token && user),
    isLoading,
    login,
    register: authService.register,
    logout,
    refreshUser,
    updateUser,
  }), [isLoading, login, logout, refreshUser, token, updateUser, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }
  return context;
}

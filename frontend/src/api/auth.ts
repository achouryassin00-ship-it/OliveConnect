import { apiRequest } from "./client";
import type { AuthResponse } from "../types/api";
import type { User } from "../types/domain";
export const bearer = (token: string) => ({ Authorization: `Bearer ${token}` });
export const authApi = {
  login: (email: string, password: string) =>
    apiRequest<AuthResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: email.trim(), password }),
    }),
  me: (token: string) =>
    apiRequest<User>("/auth/me", { headers: bearer(token) }),
  logout: (token: string) =>
    apiRequest<void>("/auth/logout", {
      method: "POST",
      headers: bearer(token),
    }),
};

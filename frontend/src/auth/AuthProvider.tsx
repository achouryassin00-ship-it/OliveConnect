import { useCallback, useState, type ReactNode } from "react";
import { authApi } from "../api/auth";
import { ApiError } from "../api/client";
import { AuthContext, type Session } from "./context";
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null);
  const expire = useCallback(
    () => setSession((s) => (s ? { ...s, token: "", expired: true } : null)),
    [],
  );
  async function login(email: string, password: string) {
    const response = await authApi.login(email, password);
    const user = await authApi.me(response.token);
    if (!user.active)
      throw new ApiError(
        403,
        "ACCOUNT_DISABLED",
        "Votre compte est désactivé.",
      );
    setSession({ token: response.token, user, expired: false });
  }
  async function logout() {
    const token = session?.token;
    setSession(null); // Local credentials are removed even if the network is unavailable.
    if (token) await authApi.logout(token);
  }
  return (
    <AuthContext.Provider value={{ session, login, logout, expire }}>
      {children}
    </AuthContext.Provider>
  );
}

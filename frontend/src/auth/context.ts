import { createContext, useContext } from "react";
import type { User } from "../types/domain";
export interface Session {
  user: User;
  token: string;
  expired: boolean;
}
export interface AuthState {
  session: Session | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  expire: () => void;
}
export const AuthContext = createContext<AuthState | null>(null);
export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("AuthProvider missing");
  return value;
}

import { useRef, useState, type FormEvent } from "react";
import { useAuth } from "../auth/context";
import { ApiError } from "../api/client";
import { apiEnabled } from "../api/config";
export function LoginForm({ onSuccess }: { onSuccess?: () => void }) {
  const { login } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const lock = useRef(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (lock.current || !apiEnabled) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      await login(email, password);
      setPassword("");
      onSuccess?.();
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.status === 401
            ? "Adresse e-mail ou mot de passe incorrect."
            : e.message
          : "Connexion impossible. Réessayez.",
      );
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }
  return (
    <form className="stack-form" onSubmit={submit} aria-busy={busy}>
      {!apiEnabled && (
        <p className="module-note">
          Connexion API désactivée. La démonstration reste accessible sans
          compte.
        </p>
      )}
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <label>
        Adresse e-mail
        <input
          type="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          autoComplete="username"
          maxLength={254}
        />
      </label>
      <label>
        Mot de passe
        <input
          type="password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          maxLength={100}
        />
      </label>
      <button disabled={busy || !apiEnabled}>
        {busy
          ? "Connexion en cours…"
          : !apiEnabled
            ? "Connexion disponible en mode API"
            : "Se connecter"}
      </button>
      <small>
        Session conservée en mémoire uniquement. Un rechargement nécessite une
        nouvelle connexion.
      </small>
    </form>
  );
}

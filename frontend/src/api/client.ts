export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}
const messages: Record<number, string> = {
  400: "Vérifiez les informations saisies.",
  401: "Votre session a expiré.",
  403: "Vous ne disposez pas des droits nécessaires.",
  404: "Élément introuvable.",
  409: "Les données ont changé. Rechargez avant de réessayer.",
  503: "Service momentanément indisponible.",
};
/** No demo fallback. JWT is passed explicitly, never stored in this module. */
export async function apiRequest<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  if (import.meta.env.VITE_API_ENABLED !== "true")
    throw new ApiError(
      0,
      "API_DISABLED",
      "API désactivée. Activez VITE_API_ENABLED pour utiliser les services disponibles.",
    );
  if (!path.startsWith("/") || path.startsWith("//"))
    throw new ApiError(0, "INVALID_PATH", "Chemin API invalide.");
  const base = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/$/, "");
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body && !(init.body instanceof FormData))
    headers.set("Content-Type", "application/json");
  try {
    const response = await fetch(base + path, {
      ...init,
      headers,
      signal: init.signal
        ? AbortSignal.any([init.signal, AbortSignal.timeout(10000)])
        : AbortSignal.timeout(10000),
    });
    if (!response.ok) {
      const body: unknown = await response.json().catch(() => null);
      const problem =
        body && typeof body === "object"
          ? (body as Record<string, unknown>)
          : {};
      const code =
        typeof problem.code === "string" ? problem.code : "HTTP_ERROR";
      // Use trusted, translated labels instead of arbitrary server messages/stack traces.
      const known: Record<string, string> = {
        ACCOUNT_DISABLED: "Votre compte est désactivé.",
        EMAIL_ALREADY_EXISTS: "Cette adresse e-mail est déjà utilisée.",
      };
      const fields: Record<string, string> = {};
      if (
        response.status === 400 &&
        problem.fieldErrors &&
        typeof problem.fieldErrors === "object"
      ) {
        for (const key of Object.keys(problem.fieldErrors))
          fields[key] = "Vérifiez la valeur de ce champ.";
      }
      throw new ApiError(
        response.status,
        code,
        known[code] ||
          messages[response.status] ||
          "Une erreur est survenue. Réessayez plus tard.",
        fields,
      );
    }
    if (response.status === 204) return undefined as T;
    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error instanceof DOMException && error.name === "TimeoutError")
      throw new ApiError(
        0,
        "TIMEOUT",
        "Le délai de réponse est dépassé. Réessayez.",
      );
    if (error instanceof DOMException && error.name === "AbortError")
      throw error;
    throw new ApiError(
      0,
      "NETWORK_ERROR",
      "Impossible de joindre le service. Vérifiez votre connexion.",
    );
  }
}

import type { ReactNode } from "react";
import { CircleAlert, Inbox, LoaderCircle } from "lucide-react";
export function PageHeading({
  eyebrow = "COOPÉRATIVE DE SFAX",
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="muted">{description}</p>
      </div>
      {action}
    </div>
  );
}
export function Badge({
  children,
  tone = "green",
}: {
  children: ReactNode;
  tone?: "green" | "amber" | "red" | "gray";
}) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
export function Panel({
  title,
  children,
  action,
  className = "",
}: {
  title: string;
  children: ReactNode;
  action?: ReactNode;
  className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      <div className="panel-heading">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
export function DataState({
  state,
  onRetry,
}: {
  state: "loading" | "empty" | "error";
  onRetry?: () => void;
}) {
  const Icon =
    state === "loading"
      ? LoaderCircle
      : state === "error"
        ? CircleAlert
        : Inbox;
  return (
    <div className="data-state" role={state === "error" ? "alert" : "status"}>
      <Icon size={32} className={state === "loading" ? "spin" : ""} />
      <h2>
        {state === "loading"
          ? "Chargement en cours"
          : state === "empty"
            ? "Aucun élément à afficher"
            : "Impossible de charger les données"}
      </h2>
      <p>
        {state === "error"
          ? "Exemple d’erreur réseau. Vos données ne sont pas modifiées."
          : state === "empty"
            ? "Essayez un autre filtre ou revenez plus tard."
            : "Exemple visuel : aucune requête réseau n’est envoyée."}
      </p>
      {state === "error" && (
        <button onClick={onRetry}>Réessayer la démonstration</button>
      )}
    </div>
  );
}

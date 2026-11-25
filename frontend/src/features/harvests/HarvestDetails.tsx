import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../api/client";
import { harvestCapabilities } from "../../api/harvests";
import { Badge, Panel } from "../../components/ui";
import { date, dateTime, number } from "../../lib/format";
import {
  displayDay,
  statusLabels,
  type HarvestRequestView,
  type OrchardChoice,
} from "./model";

export function HarvestDetails({
  request,
  orchards,
  base,
  demo = false,
  onCancel,
}: {
  request: HarvestRequestView | undefined;
  orchards: OrchardChoice[];
  base: string;
  demo?: boolean;
  onCancel?: (
    request: HarvestRequestView,
    reason: string,
  ) => Promise<void>;
}) {
  const [confirming, setConfirming] = useState(false);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [cancelError, setCancelError] = useState("");
  const lock = useRef(false);

  if (!request)
    return (
      <div className="data-state">
        <h2>Demande introuvable</h2>
        <p>
          {demo
            ? "Aucune demande accessible pour ce profil de démonstration."
            : "Cette demande est introuvable ou n’appartient pas à votre compte."}
        </p>
        <Link className="button" to={base}>
          Revenir aux collectes
        </Link>
      </div>
    );

  const orchard = orchards.find((v) => v.id === request.orchardId);
  const orchardLabel =
    request.orchardName ||
    orchard?.name ||
    request.orchardCode ||
    "Verger indisponible";
  const showHistory = !demo && harvestCapabilities.history;
  const canCancel =
    !demo && harvestCapabilities.cancel && request.cancellable && !!onCancel;

  async function confirmCancel() {
    if (lock.current || !onCancel || !request) return;
    lock.current = true;
    setBusy(true);
    setCancelError("");
    try {
      await onCancel(request, reason);
      setConfirming(false);
      setReason("");
    } catch (e) {
      setCancelError(
        e instanceof ApiError
          ? e.message
          : "Impossible d’annuler cette demande pour le moment.",
      );
    } finally {
      lock.current = false;
      setBusy(false);
    }
  }

  return (
    <Panel
      title={request.reference || request.id}
      action={
        <Badge
          tone={
            request.status === "ANNULEE" || request.status === "REFUSEE"
              ? "red"
              : request.status === "DEMANDEE"
                ? "amber"
                : "green"
          }
        >
          {statusLabels[request.status]}
        </Badge>
      }
    >
      <div className="card-body">
        <dl className="detail-grid">
          <div>
            <dt>Verger</dt>
            <dd>
              {orchardLabel}
              {request.orchardCode ? ` (${request.orchardCode})` : ""}
            </dd>
          </div>
          <div>
            <dt>Date souhaitée</dt>
            <dd>{displayDay(request.desiredDate)}</dd>
          </div>
          <div>
            <dt>Quantité estimée</dt>
            <dd>
              {request.estimatedKg === null
                ? "Non renseignée"
                : `${number(request.estimatedKg)} kg`}
            </dd>
          </div>
          <div>
            <dt>Type</dt>
            <dd>{request.urgent ? "Urgente" : "Normale"}</dd>
          </div>
          <div>
            <dt>{demo ? "Création de l’exemple" : "Création"}</dt>
            <dd>{demo ? date(request.createdAt) : dateTime(request.createdAt)}</dd>
          </div>
          {!demo && request.updatedAt && (
            <div>
              <dt>Dernière mise à jour</dt>
              <dd>{dateTime(request.updatedAt)}</dd>
            </div>
          )}
        </dl>
        {request.urgent && (
          <p>
            <strong>Motif :</strong> {request.justification}
          </p>
        )}

        {showHistory ? (
          <section className="history-section" aria-label="Historique des statuts">
            <h3>Historique des statuts</h3>
            {request.history.length === 0 ? (
              <p className="muted">Aucun événement enregistré.</p>
            ) : (
              <ol className="history-list">
                {request.history.map((entry, idx) => (
                  <li key={`${entry.timestamp}-${idx}`} className="history-item">
                    <div className="history-header">
                      <strong>
                        {entry.previousStatus
                          ? `${statusLabels[entry.previousStatus]} → ${statusLabels[entry.nextStatus]}`
                          : statusLabels[entry.nextStatus]}
                      </strong>
                      <small>{dateTime(entry.timestamp)}</small>
                    </div>
                    <p className="muted">
                      Par {entry.actorName}
                      {entry.actorRole ? ` (${entry.actorRole})` : ""}
                      {entry.reason ? ` · ${entry.reason}` : ""}
                    </p>
                  </li>
                ))}
              </ol>
            )}
          </section>
        ) : (
          <div className="module-note">
            Historique non disponible : aucun historique n’est fourni en
            démonstration. Aucune chronologie n’est reconstituée.
          </div>
        )}

        {demo ? (
          <p className="muted">
            Annulation indisponible en démonstration. Aucun changement de statut
            n’est simulé.
          </p>
        ) : canCancel ? (
          <div className="cancel-box">
            {!confirming ? (
              <button
                type="button"
                className="danger-button"
                onClick={() => {
                  setConfirming(true);
                  setCancelError("");
                }}
              >
                Annuler cette demande
              </button>
            ) : (
              <div
                className="cancel-confirm"
                role="region"
                aria-label="Confirmation d’annulation"
              >
                <p>
                  Confirmez l’annulation de la demande{" "}
                  <strong>{request.reference || request.id}</strong>. Cette
                  action est définitive.
                </p>
                {cancelError && (
                  <p className="form-error" role="alert">
                    {cancelError}
                  </p>
                )}
                <label htmlFor="cancel-reason">
                  Motif d’annulation (facultatif)
                </label>
                <input
                  id="cancel-reason"
                  maxLength={500}
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Ex. report météo ou erreur de saisie"
                  disabled={busy}
                />
                <div className="cancel-actions">
                  <button
                    type="button"
                    className="danger-button"
                    disabled={busy}
                    onClick={confirmCancel}
                  >
                    {busy ? "Annulation en cours…" : "Confirmer l’annulation"}
                  </button>
                  <button
                    type="button"
                    className="secondary-button"
                    disabled={busy}
                    onClick={() => {
                      setConfirming(false);
                      setCancelError("");
                    }}
                  >
                    Conserver la demande
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <p className="muted">
            Annulation indisponible : une demande au statut «{" "}
            {statusLabels[request.status]} » ne peut pas être annulée.
          </p>
        )}

        <Link className="button" to={base}>
          Revenir aux collectes
        </Link>
      </div>
    </Panel>
  );
}

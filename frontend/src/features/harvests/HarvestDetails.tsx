import { Link } from "react-router-dom";
import { Badge, Panel } from "../../components/ui";
import { date, number } from "../../lib/format";
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
}: {
  request: HarvestRequestView | undefined;
  orchards: OrchardChoice[];
  base: string;
}) {
  if (!request)
    return (
      <div className="data-state">
        <h2>Demande introuvable</h2>
        <p>Aucune demande accessible pour ce profil de démonstration.</p>
        <Link className="button" to={base}>
          Revenir aux collectes
        </Link>
      </div>
    );
  const orchard = orchards.find((v) => v.id === request.orchardId);
  return (
    <Panel
      title={request.id}
      action={<Badge>{statusLabels[request.status]}</Badge>}
    >
      <div className="card-body">
        <dl className="detail-grid">
          <div>
            <dt>Verger</dt>
            <dd>{orchard?.name || "Verger indisponible"}</dd>
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
            <dt>Création de l’exemple</dt>
            <dd>{date(request.createdAt)}</dd>
          </div>
        </dl>
        {request.urgent && (
          <p>
            <strong>Motif :</strong> {request.justification}
          </p>
        )}
        <div className="module-note">
          Historique non disponible : aucun historique n’est fourni par l’API
          actuelle. Aucune chronologie n’est reconstituée.
        </div>
        <p className="muted">
          Annulation indisponible : le contrat et le backend ne proposent pas
          cette opération. Aucun changement de statut n’est simulé.
        </p>
        <Link className="button" to={base}>
          Revenir aux collectes
        </Link>
      </div>
    </Panel>
  );
}

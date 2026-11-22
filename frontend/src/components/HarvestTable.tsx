import { harvests, orchards } from "../demo/data";
import { date, number } from "../lib/format";
import { Badge, DataState } from "./ui";
const labels = {
  DEMANDEE: "À valider",
  PLANIFIEE: "Planifiée",
  EN_COURS: "En cours",
  VALIDEE: "Validée",
  LIVREE: "Livrée",
  PESEE: "Pesée",
  ARCHIVEE: "Archivée",
  REFUSEE: "Refusée",
  ANNULEE: "Annulée",
  INTERROMPUE: "Interrompue",
};
export function HarvestTable({ query = "" }: { query?: string }) {
  const rows = harvests.filter((h) =>
    `${h.id} ${orchards.find((v) => v.id === h.orchardId)?.name}`
      .toLocaleLowerCase("fr")
      .includes(query.toLocaleLowerCase("fr")),
  );
  if (!rows.length) return <DataState state="empty" />;
  return (
    <div className="table-scroll">
      <table>
        <caption className="sr-only">Collectes fictives de la campagne</caption>
        <thead>
          <tr>
            <th>Référence / verger</th>
            <th>Date prévue</th>
            <th>Volume estimé</th>
            <th>Priorité</th>
            <th>Statut</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((h) => (
            <tr key={h.id}>
              <td>
                <strong>
                  {orchards.find((v) => v.id === h.orchardId)?.name}
                </strong>
                <small>{h.id}</small>
              </td>
              <td>{date(h.scheduledAt)}</td>
              <td>{number(h.estimatedKg)} kg</td>
              <td>
                {h.urgent ? (
                  <Badge tone="red">Urgente</Badge>
                ) : (
                  <span className="muted">Normale</span>
                )}
              </td>
              <td>
                <Badge tone={h.status === "DEMANDEE" ? "amber" : "green"}>
                  {labels[h.status]}
                </Badge>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Plus, Search, Sprout } from "lucide-react";
import { useAuth } from "../../auth/context";
import type { Session } from "../../auth/context";
import { apiEnabled } from "../../api/config";
import { ApiError } from "../../api/client";
import { loadOwnedVergers, vergersApi } from "../../api/vergers";
import { Badge, PageHeading, Panel, DataState } from "../../components/ui";
import { LoginForm } from "../../components/LoginForm";
import {
  demoFarmerId,
  demoHarvestRequests,
  demoOrchardChoices,
} from "../../demo/harvests";
import { date, number } from "../../lib/format";
import {
  displayDay,
  emptyDraft,
  statusLabels,
  type OrchardChoice,
  type HarvestDraft,
} from "./model";
import { HarvestForm } from "./HarvestForm";
import { HarvestDetails } from "./HarvestDetails";
import "./harvests.css";

export function HarvestPage({ forceDemo = false }: { forceDemo?: boolean }) {
  const { session } = useAuth();
  const demo = forceDemo || !apiEnabled;
  return (
    <HarvestWorkspace
      key={demo ? "demo" : session?.user.id || "anonymous"}
      demo={demo}
      session={session}
      base={forceDemo ? "/demo/collectes" : "/collectes"}
    />
  );
}
function HarvestWorkspace({
  demo,
  session,
  base,
}: {
  demo: boolean;
  session: Session | null;
  base: string;
}) {
  const { expire } = useAuth();
  const navigate = useNavigate();
  const subpath = useParams()["*"] || "";
  const ownerId = demo ? demoFarmerId : session?.user.id || "";
  const [orchards, setOrchards] = useState<OrchardChoice[]>(
    demo
      ? demoOrchardChoices.filter((v) => v.active && v.ownerId === demoFarmerId)
      : [],
  );
  const [loading, setLoading] = useState(!demo);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  const [rows, setRows] = useState(
    demo ? demoHarvestRequests.filter((r) => r.ownerId === demoFarmerId) : [],
  );
  const [draft, setDraft] = useState<HarvestDraft>({ ...emptyDraft });
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [type, setType] = useState("");
  const [preview, setPreview] = useState("ready");
  const token = session?.token;
  const farmer = !!session?.user.roles.includes("AGRICULTEUR");
  useEffect(() => {
    if (demo || !token || !farmer) return;
    const controller = new AbortController();
    loadOwnedVergers(token, ownerId, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setOrchards(
          result.map((v) => ({
            id: v.id,
            name: v.nom,
            code: v.code,
            ownerId: v.proprietaireId,
            active: v.actif,
          })),
        );
        setLoading(false);
        setError("");
      })
      .catch((e) => {
        if (controller.signal.aborted) return;
        setLoading(false);
        setError(
          e instanceof ApiError
            ? e.message
            : "Impossible de charger vos vergers.",
        );
        if (e instanceof ApiError && e.status === 401) expire();
      });
    return () => controller.abort();
  }, [demo, token, farmer, ownerId, reload, expire]);
  function retry() {
    setLoading(true);
    setError("");
    setReload((n) => n + 1);
  }
  async function submit(value: HarvestDraft) {
    setNotice("");
    if (demo) {
      const id = `LOCAL-${crypto.randomUUID().slice(0, 8)}`;
      setRows((current) => [
        ...current,
        {
          id,
          ownerId,
          orchardId: value.orchardId,
          desiredDate: value.desiredDate,
          estimatedKg: value.estimatedKg.trim()
            ? Number(value.estimatedKg.replace(",", "."))
            : null,
          urgent: value.urgent,
          justification: value.urgent ? value.justification.trim() : "",
          status: "DEMANDEE",
          createdAt: new Date().toISOString(),
        },
      ]);
      setDraft({ ...emptyDraft });
      setNotice(
        "Exemple local ajouté. Aucune demande envoyée à la coopérative ; cet exemple disparaît en quittant cet espace ou en rechargeant.",
      );
      navigate(base);
      return;
    }
    if (!session?.token || session.expired)
      throw new ApiError(
        401,
        "UNAUTHORIZED",
        "Reconnectez-vous pour vérifier ce brouillon.",
      );
    try {
      // Recheck ownership/active state with the real backend, even after the options were loaded.
      const orchard = await vergersApi.get(session.token, value.orchardId);
      if (!orchard.actif || orchard.proprietaireId !== ownerId)
        throw new ApiError(
          404,
          "RESOURCE_NOT_FOUND",
          "Ce verger n’est plus disponible pour votre compte.",
        );
      setNotice(
        "Brouillon vérifié avec votre verger actuel. Aucun envoi effectué : l’API de récoltes reste à développer au Jour 5.",
      );
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) expire();
      throw e;
    }
  }
  const normalize = (value: string) =>
    value
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLocaleLowerCase("fr");
  const filtered = rows.filter(
    (r) =>
      r.ownerId === ownerId &&
      normalize(
        `${r.id} ${orchards.find((v) => v.id === r.orchardId)?.name || ""}`,
      ).includes(normalize(query)) &&
      (!status || r.status === status) &&
      (!type || r.urgent === (type === "urgent")),
  );
  return (
    <>
      <PageHeading
        eyebrow="ESPACE AGRICULTEUR"
        title={
          subpath === "nouvelle"
            ? "Préparer une collecte"
            : subpath
              ? "Détail de la demande"
              : "Mes demandes de collecte"
        }
        description="Du premier signalement au suivi de votre récolte."
        action={
          !subpath && (demo || (farmer && !session?.expired)) ? (
            <Link className="button" to={`${base}/nouvelle`}>
              <Plus size={18} /> Nouvelle demande
            </Link>
          ) : undefined
        }
      />
      <div className="harvest-banner">
        <Sprout size={23} />
        <div>
          <strong>
            {demo
              ? "Démonstration interactive · profil agriculteur fictif"
              : "Vergers connectés · demandes non connectées"}
          </strong>
          <p>
            {demo
              ? "Exemples en mémoire uniquement. Aucun enregistrement dans MongoDB."
              : "L’authentification et les vergers utilisent Spring Boot. La création, la liste et les statuts des récoltes attendent le Jour 5."}
          </p>
        </div>
      </div>
      {!demo && !session ? (
        <Panel title="Connectez-vous à votre espace">
          <div className="card-body">
            <LoginForm />
            <Link className="text-link" to="/demo/collectes">
              Explorer les demandes de démonstration
            </Link>
          </div>
        </Panel>
      ) : !demo && !farmer ? (
        <Panel title="Accès réservé aux agriculteurs">
          <div className="card-body" role="alert">
            Ce compte ne possède pas le rôle AGRICULTEUR. Aucune demande ni
            liste de vergers n’est chargée pour ce parcours.
          </div>
        </Panel>
      ) : (
        <>
          {!demo && session?.expired && (
            <Panel title="Session expirée">
              <div className="card-body">
                <p role="alert">
                  Reconnectez-vous. Votre brouillon reste en mémoire sur cet
                  écran pour ce compte.
                </p>
                <LoginForm />
              </div>
            </Panel>
          )}
          {notice && (
            <p className="success-note" role="status">
              {notice}
            </p>
          )}
          {error && !session?.expired && (
            <div className="form-error" role="alert">
              <p>{error} Votre brouillon est conservé.</p>
              <button onClick={retry}>
                Réessayer le chargement des vergers
              </button>
            </div>
          )}
          {subpath === "nouvelle" ? (
            <Panel
              title={
                demo
                  ? "Nouvel exemple de demande"
                  : "Brouillon · transmission indisponible"
              }
              action={
                <Link className="text-link" to={base}>
                  Retour à la liste
                </Link>
              }
            >
              {loading && !demo && (
                <p role="status" className="card-body">
                  Chargement de vos vergers…
                </p>
              )}
              <HarvestForm
                draft={draft}
                onChange={(value) => {
                  setDraft(value);
                  setNotice("");
                }}
                orchards={orchards}
                ownerId={ownerId}
                demo={demo}
                disabled={!demo && (loading || !!error || !!session?.expired)}
                onSubmit={submit}
              />
            </Panel>
          ) : subpath ? (
            demo ? (
              <HarvestDetails
                request={rows.find(
                  (r) => r.id === subpath && r.ownerId === ownerId,
                )}
                orchards={orchards}
                base={base}
              />
            ) : (
              <Panel title="Détail indisponible">
                <p className="card-body">
                  L’API de détail de récolte n’existe pas encore. Aucun appel ni
                  historique fictif n’est présenté ici.
                </p>
              </Panel>
            )
          ) : demo ? (
            <>
              <div className="toolbar">
                <span className="muted">
                  {rows.length} exemples pour ce profil
                </span>
                <label>
                  État de l’interface
                  <select
                    value={preview}
                    onChange={(e) => setPreview(e.target.value)}
                  >
                    <option value="ready">Avec données</option>
                    <option value="loading">Chargement (simulé)</option>
                    <option value="empty">Aucune donnée</option>
                    <option value="error">Erreur (simulée)</option>
                  </select>
                </label>
              </div>
              <Panel title="Demandes de récolte">
                <div className="harvest-filters">
                  <label className="search">
                    <Search size={18} />
                    <input
                      aria-label="Rechercher une collecte"
                      placeholder="Référence ou verger…"
                      value={query}
                      onChange={(e) => setQuery(e.target.value)}
                    />
                  </label>
                  <label>
                    Statut
                    <select
                      aria-label="Statut"
                      value={status}
                      onChange={(e) => setStatus(e.target.value)}
                    >
                      <option value="">Tous les statuts</option>
                      {Object.entries(statusLabels).map(([key, label]) => (
                        <option key={key} value={key}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Type
                    <select
                      aria-label="Type"
                      value={type}
                      onChange={(e) => setType(e.target.value)}
                    >
                      <option value="">Tous les types</option>
                      <option value="normal">Normale</option>
                      <option value="urgent">Urgente</option>
                    </select>
                  </label>
                </div>
                {preview !== "ready" ? (
                  <DataState
                    state={preview as "loading" | "empty" | "error"}
                    onRetry={() => setPreview("ready")}
                  />
                ) : filtered.length === 0 ? (
                  <DataState state="empty" />
                ) : (
                  <div className="table-scroll">
                    <table className="harvest-table">
                      <caption className="sr-only">
                        Demandes fictives du profil agriculteur de démonstration
                      </caption>
                      <thead>
                        <tr>
                          <th>Demande / verger</th>
                          <th>Date souhaitée</th>
                          <th>Estimation</th>
                          <th>Type</th>
                          <th>Statut</th>
                          <th>Création</th>
                          <th>Détail</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filtered.map((r) => (
                          <tr key={r.id}>
                            <td>
                              <strong>
                                {
                                  orchards.find((v) => v.id === r.orchardId)
                                    ?.name
                                }
                              </strong>
                              <small>{r.id}</small>
                            </td>
                            <td>{displayDay(r.desiredDate)}</td>
                            <td>
                              {r.estimatedKg === null
                                ? "Non renseignée"
                                : `${number(r.estimatedKg)} kg`}
                            </td>
                            <td>
                              <Badge tone={r.urgent ? "red" : "gray"}>
                                {r.urgent ? "Urgente" : "Normale"}
                              </Badge>
                            </td>
                            <td>
                              <Badge
                                tone={
                                  r.status === "DEMANDEE" ? "amber" : "green"
                                }
                              >
                                {statusLabels[r.status]}
                              </Badge>
                            </td>
                            <td>{date(r.createdAt)}</td>
                            <td>
                              <Link
                                className="text-link detail-link"
                                aria-label={`Voir ${r.id}`}
                                to={`${base}/${r.id}`}
                              >
                                Consulter
                              </Link>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </Panel>
            </>
          ) : (
            <Panel title="Suivi des demandes · prochainement">
              <div className="card-body">
                <p>
                  Aucune liste réelle de récoltes n’est accessible avec le
                  backend actuel. Cela ne signifie pas que votre compte ne
                  possède aucune demande.
                </p>
                <p>
                  {loading
                    ? "Chargement de vos vergers…"
                    : `${orchards.length} verger(s) actif(s) accessible(s) pour préparer un brouillon.`}
                </p>
                <Link className="text-link" to="/demo/collectes">
                  Explorer un jeu de demandes fictives séparé
                </Link>
              </div>
            </Panel>
          )}
          {demo && (
            <p className="footnote">
              Les statuts et champs de cet aperçu sont provisoires ; leur
              contrat serveur sera validé avec Ben Ayed. Annulation et
              historique ne sont pas disponibles.
            </p>
          )}
        </>
      )}
    </>
  );
}

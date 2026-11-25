import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { Plus, Search, Sprout } from "lucide-react";
import { useAuth } from "../../auth/context";
import type { Session } from "../../auth/context";
import { apiEnabled } from "../../api/config";
import { ApiError } from "../../api/client";
import { harvestCapabilities, harvestsApi } from "../../api/harvests";
import { loadOwnedVergers } from "../../api/vergers";
import { Badge, PageHeading, Panel, DataState } from "../../components/ui";
import { LoginForm } from "../../components/LoginForm";
import {
  demoFarmerId,
  demoHarvestRequests,
  demoOrchardChoices,
} from "../../demo/harvests";
import { date, number } from "../../lib/format";
import type { HarvestStatus } from "../../types/domain";
import {
  displayDay,
  emptyDraft,
  statusLabels,
  type OrchardChoice,
  type HarvestDraft,
  type HarvestRequestView,
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
  const [orchardsLoading, setOrchardsLoading] = useState(!demo);
  const [orchardsError, setOrchardsError] = useState("");
  const [orchardsReload, setOrchardsReload] = useState(0);

  const [rows, setRows] = useState<HarvestRequestView[]>(
    demo ? demoHarvestRequests.filter((r) => r.ownerId === demoFarmerId) : [],
  );
  const [listLoading, setListLoading] = useState(!demo);
  const [listError, setListError] = useState("");
  const [listReload, setListReload] = useState(0);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [selectedDetail, setSelectedDetail] =
    useState<HarvestRequestView | null>(null);
  const [detailError, setDetailError] = useState("");
  const [detailNotFound, setDetailNotFound] = useState(false);
  const [detailReload, setDetailReload] = useState(0);

  const [draft, setDraft] = useState<HarvestDraft>({ ...emptyDraft });
  const idempotencyKeyRef = useRef<string | null>(null);
  const [notice, setNotice] = useState("");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<HarvestStatus | "">("");
  const [type, setType] = useState<"" | "normal" | "urgent">("");
  const [preview, setPreview] = useState("ready");

  const token = session?.token;
  const expired = !!session?.expired;
  const farmer = !!session?.user.roles.includes("AGRICULTEUR");

  useEffect(() => {
    if (demo || !token || expired || !farmer) return;
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
        setOrchardsLoading(false);
        setOrchardsError("");
      })
      .catch((e) => {
        if (controller.signal.aborted) return;
        setOrchardsLoading(false);
        setOrchardsError(
          e instanceof ApiError
            ? e.message
            : "Impossible de charger vos vergers.",
        );
        if (e instanceof ApiError && e.status === 401) expire();
      });
    return () => controller.abort();
  }, [demo, token, expired, farmer, ownerId, orchardsReload, expire]);

  useEffect(() => {
    if (
      demo ||
      !harvestCapabilities.list ||
      !token ||
      expired ||
      !farmer ||
      subpath !== ""
    )
      return;
    const controller = new AbortController();
    harvestsApi
      .list(
        token,
        {
          page,
          size: 20,
          q: query,
          statut: status,
          urgent: type === "" ? "" : type === "urgent",
        },
        controller.signal,
      )
      .then((result) => {
        if (controller.signal.aborted) return;
        setRows(result.items);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
        setListLoading(false);
        setListError("");
      })
      .catch((e) => {
        if (controller.signal.aborted) return;
        setListLoading(false);
        setListError(
          e instanceof ApiError
            ? e.message
            : "Impossible de charger vos demandes de collecte.",
        );
        if (e instanceof ApiError && e.status === 401) expire();
      });
    return () => controller.abort();
  }, [
    demo,
    token,
    expired,
    farmer,
    subpath,
    page,
    query,
    status,
    type,
    listReload,
    expire,
  ]);

  useEffect(() => {
    if (
      demo ||
      !token ||
      expired ||
      !farmer ||
      !subpath ||
      subpath === "nouvelle"
    )
      return;
    const controller = new AbortController();
    harvestsApi
      .get(token, subpath, controller.signal)
      .then((item) => {
        if (controller.signal.aborted) return;
        setSelectedDetail(item);
        setDetailError("");
        setDetailNotFound(false);
      })
      .catch((e) => {
        if (controller.signal.aborted) return;
        if (e instanceof ApiError && e.status === 404) {
          setSelectedDetail(null);
          setDetailNotFound(true);
          setDetailError("");
        } else {
          setDetailError(
            e instanceof ApiError
              ? e.message
              : "Impossible de charger le détail de cette demande.",
          );
          if (e instanceof ApiError && e.status === 401) expire();
        }
      });
    return () => controller.abort();
  }, [demo, token, expired, farmer, subpath, detailReload, expire]);

  function retryOrchards() {
    setOrchardsLoading(true);
    setOrchardsError("");
    setOrchardsReload((n) => n + 1);
  }

  function retryList() {
    setListLoading(true);
    setListError("");
    setListReload((n) => n + 1);
  }

  function retryDetail() {
    setDetailError("");
    setDetailNotFound(false);
    setSelectedDetail(null);
    setDetailReload((n) => n + 1);
  }

  async function submit(value: HarvestDraft) {
    setNotice("");
    if (demo) {
      const id = `LOCAL-${crypto.randomUUID().slice(0, 8)}`;
      const chosen = orchards.find((v) => v.id === value.orchardId);
      setRows((current) => [
        ...current,
        {
          id,
          reference: id,
          ownerId,
          orchardId: value.orchardId,
          orchardName: chosen?.name || "",
          orchardCode: chosen?.code || "",
          desiredDate: value.desiredDate,
          estimatedKg: value.estimatedKg.trim()
            ? Number(value.estimatedKg.replace(",", "."))
            : null,
          urgent: value.urgent,
          justification: value.urgent ? value.justification.trim() : "",
          status: "DEMANDEE",
          cancellable: false,
          version: null,
          history: [],
          createdAt: new Date().toISOString(),
          updatedAt: null,
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
        "Reconnectez-vous pour envoyer cette demande.",
      );
    if (!idempotencyKeyRef.current) {
      idempotencyKeyRef.current = crypto.randomUUID();
    }
    try {
      const created = await harvestsApi.create(
        session.token,
        value,
        idempotencyKeyRef.current,
      );
      idempotencyKeyRef.current = null;
      setDraft({ ...emptyDraft });
      setPage(0);
      setListLoading(true);
      setListReload((n) => n + 1);
      setNotice(
        `Demande de collecte enregistrée (${created.reference || created.id}).`,
      );
      navigate(base);
    } catch (e) {
      if (e instanceof ApiError) {
        if (e.status === 401) {
          expire();
        } else if (e.status === 400 || e.status === 404 || e.status === 409) {
          // Une erreur métier impose une nouvelle clé si l'agriculteur corrige et renvoie.
          idempotencyKeyRef.current = null;
        }
      }
      throw e;
    }
  }

  async function cancelRequest(request: HarvestRequestView, reason: string) {
    if (!session?.token || session.expired) {
      expire();
      throw new ApiError(
        401,
        "UNAUTHORIZED",
        "Reconnectez-vous pour annuler cette demande.",
      );
    }
    try {
      const updated = await harvestsApi.cancel(session.token, request.id, {
        version: request.version,
        motif: reason,
      });
      setSelectedDetail(updated);
      setRows((current) =>
        current.map((item) => (item.id === updated.id ? updated : item)),
      );
      setNotice(
        `La demande ${updated.reference || updated.id} a été annulée.`,
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

  const filteredDemo = rows.filter(
    (r) =>
      r.ownerId === ownerId &&
      normalize(
        `${r.id} ${r.reference} ${r.orchardName || orchards.find((v) => v.id === r.orchardId)?.name || ""}`,
      ).includes(normalize(query)) &&
      (!status || r.status === status) &&
      (!type || r.urgent === (type === "urgent")),
  );

  const isDetailLoading =
    !demo &&
    !!subpath &&
    subpath !== "nouvelle" &&
    !detailNotFound &&
    !detailError &&
    selectedDetail?.id !== subpath;

  const displayedRows = demo ? filteredDemo : rows;

  return (
    <>
      <PageHeading
        eyebrow="ESPACE AGRICULTEUR"
        title={
          subpath === "nouvelle"
            ? demo
              ? "Préparer une collecte"
              : "Nouvelle demande de collecte"
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
              : "Espace agriculteur connecté"}
          </strong>
          <p>
            {demo
              ? "Exemples en mémoire uniquement. Aucun enregistrement dans MongoDB."
              : "Authentification, vergers, demandes de collecte, historique et annulation connectés à Spring Boot et MongoDB."}
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
                  Reconnectez-vous. Votre saisie reste en mémoire sur cet écran
                  pour ce compte.
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
          {orchardsError && !session?.expired && (
            <div className="form-error" role="alert">
              <p>{orchardsError} Votre saisie est conservée.</p>
              <button type="button" onClick={retryOrchards}>
                Réessayer le chargement des vergers
              </button>
            </div>
          )}
          {subpath === "nouvelle" ? (
            <Panel
              title={
                demo ? "Nouvel exemple de demande" : "Déclarer une récolte"
              }
              action={
                <Link className="text-link" to={base}>
                  Retour à la liste
                </Link>
              }
            >
              {orchardsLoading && !demo && (
                <p role="status" className="card-body">
                  Chargement de vos vergers…
                </p>
              )}
              <HarvestForm
                draft={draft}
                onChange={(value) => {
                  idempotencyKeyRef.current = null;
                  setDraft(value);
                  setNotice("");
                }}
                orchards={orchards}
                ownerId={ownerId}
                demo={demo}
                disabled={
                  !demo &&
                  (orchardsLoading || !!orchardsError || !!session?.expired)
                }
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
                demo
              />
            ) : isDetailLoading ? (
              <Panel title="Détail de la demande">
                <p role="status" className="card-body">
                  Chargement de la demande…
                </p>
              </Panel>
            ) : detailError ? (
              <Panel title="Détail indisponible">
                <div className="card-body">
                  <p className="form-error" role="alert">
                    {detailError}
                  </p>
                  <button type="button" onClick={retryDetail}>
                    Réessayer
                  </button>
                </div>
              </Panel>
            ) : (
              <HarvestDetails
                request={detailNotFound ? undefined : selectedDetail || undefined}
                orchards={orchards}
                base={base}
                onCancel={cancelRequest}
              />
            )
          ) : (
            <>
              <div className="toolbar">
                <span className="muted">
                  {demo
                    ? `${rows.length} exemples pour ce profil`
                    : listLoading
                      ? "Chargement de vos demandes…"
                      : `${totalElements} demande(s) · ${orchards.length} verger(s) actif(s)`}
                </span>
                {demo && (
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
                )}
              </div>
              <Panel title="Demandes de récolte">
                <div className="harvest-filters">
                  <label className="search">
                    <Search size={18} />
                    <input
                      aria-label="Rechercher une collecte"
                      placeholder="Référence ou verger…"
                      value={query}
                      onChange={(e) => {
                        setQuery(e.target.value);
                        setPage(0);
                      }}
                    />
                  </label>
                  <label>
                    Statut
                    <select
                      aria-label="Statut"
                      value={status}
                      onChange={(e) => {
                        setStatus(e.target.value as HarvestStatus | "");
                        setPage(0);
                      }}
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
                      onChange={(e) => {
                        setType(e.target.value as "" | "normal" | "urgent");
                        setPage(0);
                      }}
                    >
                      <option value="">Tous les types</option>
                      <option value="normal">Normale</option>
                      <option value="urgent">Urgente</option>
                    </select>
                  </label>
                </div>
                {demo && preview !== "ready" ? (
                  <DataState
                    state={preview as "loading" | "empty" | "error"}
                    onRetry={() => setPreview("ready")}
                  />
                ) : !demo && listLoading ? (
                  <p role="status" className="card-body">
                    Chargement de vos demandes de collecte…
                  </p>
                ) : !demo && listError ? (
                  <div className="card-body">
                    <div className="form-error" role="alert">
                      <p>{listError}</p>
                      <button type="button" onClick={retryList}>
                        Réessayer le chargement des demandes
                      </button>
                    </div>
                  </div>
                ) : displayedRows.length === 0 ? (
                  <DataState state="empty" />
                ) : (
                  <>
                    <div className="table-scroll">
                      <table className="harvest-table">
                        <caption className="sr-only">
                          {demo
                            ? "Demandes fictives du profil agriculteur de démonstration"
                            : "Demandes de collecte enregistrées pour votre compte"}
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
                          {displayedRows.map((r) => (
                            <tr key={r.id}>
                              <td>
                                <strong>
                                  {r.orchardName ||
                                    orchards.find((v) => v.id === r.orchardId)
                                      ?.name ||
                                    "Verger"}
                                </strong>
                                <small>{r.reference || r.id}</small>
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
                                    r.status === "ANNULEE" ||
                                    r.status === "REFUSEE"
                                      ? "red"
                                      : r.status === "DEMANDEE"
                                        ? "amber"
                                        : "green"
                                  }
                                >
                                  {statusLabels[r.status]}
                                </Badge>
                              </td>
                              <td>{date(r.createdAt)}</td>
                              <td>
                                <Link
                                  className="text-link detail-link"
                                  aria-label={`Voir ${r.reference || r.id}`}
                                  to={`${base}/${r.id}`}
                                  onClick={() => {
                                    if (!demo) {
                                      setDetailError("");
                                      setDetailNotFound(false);
                                    }
                                  }}
                                >
                                  Consulter
                                </Link>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!demo && totalPages > 1 && (
                      <div className="pagination-bar">
                        <button
                          type="button"
                          disabled={page <= 0}
                          onClick={() => {
                            setListLoading(true);
                            setPage((p) => Math.max(0, p - 1));
                          }}
                        >
                          Précédent
                        </button>
                        <span className="muted">
                          Page {page + 1} sur {totalPages}
                        </span>
                        <button
                          type="button"
                          disabled={page + 1 >= totalPages}
                          onClick={() => {
                            setListLoading(true);
                            setPage((p) => p + 1);
                          }}
                        >
                          Suivant
                        </button>
                      </div>
                    )}
                  </>
                )}
              </Panel>
            </>
          )}
          {demo && (
            <p className="footnote">
              Les statuts et champs de cet aperçu sont provisoires ; espace de
              démonstration séparé des données MongoDB. Annulation et historique
              ne sont disponibles que sur l’espace connecté.
            </p>
          )}
        </>
      )}
    </>
  );
}

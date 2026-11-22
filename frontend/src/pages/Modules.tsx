import { useState } from "react";
import { Search, Trees, MapPin, ShieldCheck, Tractor } from "lucide-react";
import { orchards, alerts, resources, tours, weighings } from "../demo/data";
import { Badge, DataState, PageHeading, Panel } from "../components/ui";
import { HarvestTable } from "../components/HarvestTable";
import { date, number } from "../lib/format";
const titles: Record<string, [string, string]> = {
  vergers: [
    "Vos vergers",
    "Le patrimoine oléicole de la coopérative, parcelle après parcelle.",
  ],
  collectes: [
    "Gestion des collectes",
    "Visualisez les demandes et les prochaines récoltes.",
  ],
  alertes: [
    "Alertes terrain",
    "Repérez les situations qui méritent votre attention.",
  ],
  tournees: [
    "Planification des tournées",
    "Préparez les circuits et coordonnez les équipes.",
  ],
  ressources: ["Parc & ressources", "Une vue partagée des moyens disponibles."],
  carte: ["Carte des vergers", "Situez les zones de récolte autour de Sfax."],
  pressoir: [
    "Réception au pressoir",
    "Préparez le suivi des lots, des pesées et de la qualité.",
  ],
  statistiques: [
    "Performance de campagne",
    "Les premiers repères pour comprendre vos récoltes.",
  ],
  administration: [
    "Administration",
    "Préparez les accès et le suivi de la coopérative.",
  ],
};
export function ModulePage({ module }: { module: string }) {
  const [query, setQuery] = useState("");
  const [state, setState] = useState("ready");
  const [selected, setSelected] = useState(orchards[0].id);
  const [title, description] = titles[module];
  return (
    <>
      <PageHeading title={title} description={description} />
      <div className="module-note">
        <ShieldCheck size={20} />
        <span>
          Aperçu du Jour 1 : consultation d’exemples uniquement. Les opérations
          métier seront intégrées aux étapes suivantes.
        </span>
      </div>
      <div className="toolbar">
        <span className="muted">Données de démonstration</span>
        <label>
          État de l’interface{" "}
          <select value={state} onChange={(e) => setState(e.target.value)}>
            <option value="ready">Avec données</option>
            <option value="loading">Chargement (simulé)</option>
            <option value="empty">Aucune donnée</option>
            <option value="error">Erreur (simulée)</option>
          </select>
        </label>
      </div>
      {state !== "ready" ? (
        <Panel title="Aperçu des états">
          <DataState
            state={state as "loading" | "empty" | "error"}
            onRetry={() => setState("ready")}
          />
        </Panel>
      ) : (
        <>
          {module === "collectes" && (
            <Panel
              title="Demandes de récolte"
              action={
                <label className="search">
                  <Search size={18} />
                  <input
                    aria-label="Rechercher une collecte"
                    placeholder="Verger ou référence…"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </label>
              }
            >
              <HarvestTable query={query} />
            </Panel>
          )}
          {module === "vergers" && (
            <div className="card-grid">
              {orchards.map((v) => (
                <article className="orchard-card" key={v.id}>
                  <div className="orchard-visual">
                    <Trees size={56} strokeWidth={1} />
                    <span>{v.zone}</span>
                  </div>
                  <div className="card-body">
                    <h2>{v.name}</h2>
                    <p>
                      {v.variety} · {number(v.areaHa)} ha
                    </p>
                    <div className="split">
                      <span>Maturité</span>
                      <strong>
                        {v.maturityPercent === null
                          ? "Inconnue"
                          : `${v.maturityPercent} %`}
                      </strong>
                    </div>
                    {v.maturityPercent !== null && (
                      <progress
                        aria-label={`Maturité de ${v.name}`}
                        max="100"
                        value={v.maturityPercent}
                      />
                    )}
                    {v.maturityPercent === null && (
                      <small>Aucun relevé disponible dans cet exemple.</small>
                    )}
                  </div>
                </article>
              ))}
            </div>
          )}
          {module === "alertes" && (
            <Panel title="Signalements à examiner">
              {alerts.map((a) => (
                <article className="list-item" key={a.id}>
                  <div>
                    <h3>{a.title}</h3>
                    <p>
                      {orchards.find((v) => v.id === a.orchardId)?.name} ·{" "}
                      {date(a.createdAt)}
                    </p>
                  </div>
                  <Badge tone={a.severity === "URGENTE" ? "red" : "amber"}>
                    {a.severity}
                  </Badge>
                </article>
              ))}
            </Panel>
          )}
          {module === "tournees" && (
            <Panel title="Prochain circuit · exemple de brouillon">
              {tours.map((t) => (
                <article className="tour-card" key={t.id}>
                  <div>
                    <Badge tone="amber">Brouillon incomplet</Badge>
                    <h2>{t.name}</h2>
                    <p>{date(t.startAt)} · 07:00–12:00, heure de Tunis</p>
                    <p>{t.id} · Sakiet Ezzit · Pressoir central</p>
                  </div>
                  <div className="tour-route">
                    <MapPin />
                    <span>Sakiet Ezzit</span>
                    <span className="route-line" />
                    <MapPin />
                    <span>Sfax</span>
                  </div>
                  <p className="muted">
                    Équipe et tracteur à affecter. La validation des ressources
                    appartient au Jour 6.
                  </p>
                </article>
              ))}
            </Panel>
          )}
          {module === "ressources" && (
            <div className="card-grid">
              {resources.map((r) => (
                <Panel key={r.id} title={r.id}>
                  <div className="card-body">
                    <Tractor size={32} />
                    <h3>{r.name}</h3>
                    <Badge
                      tone={
                        r.status === "MAINTENANCE"
                          ? "amber"
                          : r.status === "AFFECTEE"
                            ? "gray"
                            : "green"
                      }
                    >
                      {r.status === "DISPONIBLE"
                        ? "Disponible"
                        : r.status === "AFFECTEE"
                          ? "Affectée"
                          : "Maintenance"}
                    </Badge>
                    {r.capacityKg && (
                      <p>Capacité : {number(r.capacityKg)} kg</p>
                    )}
                  </div>
                </Panel>
              ))}
            </div>
          )}
          {module === "carte" && (
            <Panel
              title="Région de Sfax"
              action={
                <Badge tone="gray">
                  Schéma illustratif · sans géolocalisation
                </Badge>
              }
            >
              <div className="map-layout">
                <div className="map-list">
                  {orchards.map((v) => (
                    <button
                      key={v.id}
                      className={selected === v.id ? "selected" : ""}
                      onClick={() => setSelected(v.id)}
                    >
                      <MapPin size={18} />
                      <span>
                        {v.name}
                        <small>{v.zone}</small>
                      </span>
                    </button>
                  ))}
                </div>
                <div className="schematic-map">
                  <span className="sea-label">Méditerranée</span>
                  <svg
                    viewBox="0 0 600 360"
                    role="img"
                    aria-label="Schéma fictif des zones de vergers autour de Sfax, sans précision géographique"
                  >
                    <path
                      d="M440 0 Q400 90 460 170 T430 360 L600 360 L600 0Z"
                      fill="#d9e9e8"
                    />
                    <g fill="none" stroke="#fff" strokeWidth="9">
                      <path d="M50 20L390 330M0 200L420 120M210 0L120 360M0 310L400 40" />
                    </g>
                    <g
                      fill="none"
                      stroke="#c4d1b7"
                      strokeWidth="2"
                      strokeDasharray="5 5"
                    >
                      <path d="M120 80L260 140L320 240L180 300" />
                    </g>
                    {orchards.map((v, i) => (
                      <g
                        key={v.id}
                        transform={`translate(${[120, 260, 180, 320][i]},${[80, 140, 300, 240][i]})`}
                      >
                        <circle
                          r={selected === v.id ? 18 : 11}
                          fill={selected === v.id ? "#294c36" : "#7e9662"}
                        />
                        <text x="23" y="5" fill="#263e30" fontSize="15">
                          {v.name}
                        </text>
                      </g>
                    ))}
                  </svg>
                  <span className="map-caption">
                    {orchards.find((v) => v.id === selected)?.name} · Position
                    illustrative
                  </span>
                </div>
              </div>
              <p className="card-body muted">
                La vraie carte Leaflet et ses filtres sont prévus au Jour 7.
                Aucune position privée réelle n’est exposée.
              </p>
            </Panel>
          )}
          {module === "pressoir" && (
            <Panel title="Exemple de lot réceptionné">
              {weighings.map((w) => (
                <article className="list-item" key={w.id}>
                  <div>
                    <h3>{w.id} · Pressoir central</h3>
                    <p>{date(w.measuredAt)} · Tournée historique fictive</p>
                  </div>
                  <strong>{number(w.netKg)} kg</strong>
                  <Badge>Qualité {w.quality}</Badge>
                </article>
              ))}
              <p className="card-body muted">
                Les pesées seront conservées sans suppression physique ; les
                corrections devront être tracées côté serveur.
              </p>
            </Panel>
          )}
          {module === "statistiques" && (
            <div className="card-grid">
              {[
                ["Rendement moyen", "3,8 t/ha", "Estimation fictive"],
                ["Délai moyen", "2,4 jours", "Demande → collecte"],
                ["Remplissage des bennes", "82 %", "Moyenne fictive"],
              ].map(([label, value, detail]) => (
                <section className="metric" key={label}>
                  <span>{label}</span>
                  <strong>{value}</strong>
                  <small>{detail}</small>
                </section>
              ))}
            </div>
          )}
          {module === "administration" && (
            <Panel title="Accès & supervision">
              <div className="card-body">
                <Badge tone="gray">Non connecté</Badge>
                <h2>Un espace de confiance pour la coopérative</h2>
                <p>
                  Gestion des comptes, validation des inscriptions, rôles et
                  imports : ces fonctions nécessitent la future API sécurisée.
                </p>
                <p>
                  Aucun compte réel, rôle ou droit d’accès ne peut être modifié
                  dans cette démonstration.
                </p>
              </div>
            </Panel>
          )}
        </>
      )}
    </>
  );
}

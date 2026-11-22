import { Link } from "react-router-dom";
import {
  ShoppingBasket,
  Sprout,
  Bell,
  CircleCheck,
  MapPin,
  CalendarDays,
} from "lucide-react";
import { campaign, weeklyVolumes, alerts, orchards } from "../demo/data";
import { Badge, PageHeading, Panel } from "../components/ui";
import { HarvestTable } from "../components/HarvestTable";
export function Dashboard() {
  const metrics = [
    [
      "Collectes actives",
      campaign.activeHarvests,
      "6 nouvelles cette semaine",
      ShoppingBasket,
    ],
    [
      "Olives récoltées",
      `${campaign.collectedKg / 1000} t`,
      "Depuis le début de campagne",
      Sprout,
    ],
    [
      "Alertes ouvertes",
      String(campaign.openAlerts).padStart(2, "0"),
      "2 demandent votre attention",
      Bell,
    ],
    [
      "Taux de réalisation",
      `${campaign.completionPercent} %`,
      "Objectif de campagne : 85 %",
      CircleCheck,
    ],
  ] as const;
  return (
    <>
      <PageHeading
        title="Bonjour, Yassine"
        description="Une vue d’ensemble pour prendre soin de chaque récolte."
        action={
          <span className="date-chip">
            <CalendarDays size={17} /> 22 novembre 2026 · Démo
          </span>
        }
      />
      <section className="hero">
        <div>
          <Badge>SAISON DES OLIVES</Badge>
          <h2>
            Une terre riche.
            <br />
            Une collecte bien coordonnée.
          </h2>
          <p>
            Vergers, équipes et pressoirs : toute votre campagne
            <br className="desktop-break" /> réunie dans un même espace.
          </p>
          <Link className="button cream" to="/collectes">
            Consulter les collectes
          </Link>
        </div>
        <div className="olive-art" aria-hidden="true">
          <div className="orbit one" />
          <div className="orbit two" />
          <Sprout strokeWidth={0.8} />
          <span className="art-label">
            SFAX, TUNISIE <span>34°44′ N · 10°46′ E</span>
          </span>
        </div>
      </section>
      <div className="metrics">
        {metrics.map(([title, value, detail, Icon]) => (
          <section className="metric" key={title}>
            <div className="metric-top">
              <span>{title}</span>
              <Icon size={20} />
            </div>
            <strong>{value}</strong>
            <small>{detail}</small>
          </section>
        ))}
      </div>
      <div className="dashboard-grid">
        <Panel
          title="Les récoltes prennent de l’élan"
          action={<Badge tone="gray">Semaine de démonstration</Badge>}
        >
          <div className="chart-summary">
            <strong>
              105 <span>tonnes</span>
            </strong>
            <span className="muted">
              Volumes collectés · exemple hebdomadaire
            </span>
          </div>
          <div
            className="chart"
            role="img"
            aria-label="Volumes fictifs du lundi au dimanche : 8, 12, 10, 18, 15, 23 et 19 tonnes."
          >
            <div className="chart-axis">
              <span>30 t</span>
              <span>20 t</span>
              <span>10 t</span>
              <span>0</span>
            </div>
            <div className="bars">
              {weeklyVolumes.map((v, i) => (
                <div className="bar-column" key={i}>
                  <span className="bar-value">{v}</span>
                  <div
                    className={`bar ${i === 5 ? "highlight" : ""}`}
                    style={{ height: `${(v / 30) * 100}%` }}
                  />
                  <span className="bar-label">
                    {["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"][i]}
                  </span>
                </div>
              ))}
            </div>
          </div>
        </Panel>
        <Panel
          title="À suivre en priorité"
          action={<Badge tone="amber">2 alertes</Badge>}
        >
          {alerts.map((a) => (
            <Link to="/alertes" className="priority-item" key={a.id}>
              <span
                className={`priority-icon ${a.severity === "URGENTE" ? "red" : "amber"}`}
              >
                <Bell size={20} />
              </span>
              <div>
                <strong>{a.title}</strong>
                <p>{orchards.find((v) => v.id === a.orchardId)?.name}</p>
                <Badge tone={a.severity === "URGENTE" ? "red" : "amber"}>
                  {a.severity === "URGENTE" ? "Urgente" : "Élevée"}
                </Badge>
              </div>
            </Link>
          ))}
          <Link className="map-teaser" to="/carte">
            <MapPin size={23} />
            <div>
              <strong>Vos vergers, en un coup d’œil</strong>
              <p>Explorer la carte de démonstration</p>
            </div>
          </Link>
        </Panel>
      </div>
      <Panel
        title="Prochaines collectes"
        action={
          <Link className="text-link" to="/collectes">
            Tout consulter
          </Link>
        }
      >
        <HarvestTable />
      </Panel>
      <p className="footnote">
        Les indicateurs illustrent une campagne fictive complète ; les listes
        présentent seulement quelques exemples.
      </p>
    </>
  );
}

import { useState } from "react";
import { NavLink, Outlet, Link, useLocation } from "react-router-dom";
import { useAuth } from "../auth/context";
import { apiEnabled } from "../api/config";
import {
  LayoutDashboard,
  Trees,
  ShoppingBasket,
  Bell,
  Route,
  Tractor,
  Map,
  Factory,
  ChartNoAxesCombined,
  ShieldCheck,
  Sprout,
  Menu,
  X,
  CircleHelp,
  LogOut,
} from "lucide-react";
const navigation = [
  ["dashboard", "Vue générale", LayoutDashboard],
  ["vergers", "Vergers", Trees],
  ["collectes", "Collectes", ShoppingBasket],
  ["alertes", "Alertes terrain", Bell],
  ["tournees", "Tournées", Route],
  ["ressources", "Ressources", Tractor],
  ["carte", "Carte des vergers", Map],
  ["pressoir", "Pressoir", Factory],
  ["statistiques", "Statistiques", ChartNoAxesCombined],
  ["administration", "Administration", ShieldCheck],
] as const;
export function Layout() {
  const [open, setOpen] = useState(false);
  const { session, logout } = useAuth();
  const { pathname } = useLocation();
  const [logoutNotice, setLogoutNotice] = useState("");
  const initials = session
    ? session.user.name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0])
        .join("")
        .toUpperCase()
    : "YA";
  const connectedScreen = apiEnabled && pathname.startsWith("/collectes");
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main">
        Aller au contenu
      </a>
      <aside className="sidebar">
        <Link className="brand" to="/dashboard">
          <span className="brand-icon">
            <Sprout size={25} />
          </span>
          <span>
            OliveConnect<small>LA TERRE NOUS CONNECTE</small>
          </span>
        </Link>
        <button
          className="mobile-toggle"
          aria-expanded={open}
          aria-controls="primary-nav"
          aria-label={open ? "Fermer le menu" : "Ouvrir le menu"}
          onClick={() => setOpen(!open)}
        >
          {open ? <X /> : <Menu />}
        </button>
        <div
          id="primary-nav"
          className={`sidebar-body ${open ? "is-open" : ""}`}
        >
          <p className="nav-label">ESPACE COOPÉRATIVE</p>
          <nav aria-label="Navigation principale">
            {navigation.map(([path, label, Icon]) => (
              <NavLink
                key={path}
                to={`/${path}`}
                onClick={() => setOpen(false)}
              >
                <Icon size={19} />
                {label}
                {path === "alertes" && (
                  <span className="nav-count" aria-hidden="true">
                    7
                  </span>
                )}
              </NavLink>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="season-card">
              <Sprout size={22} />
              <strong>Une belle saison se prépare.</strong>
              <span>Campagne 2026 – 2027</span>
              <div className="season-line" />
            </div>
            <Link
              to="/aide"
              onClick={() => setOpen(false)}
              className="help-link"
            >
              <CircleHelp size={18} /> Aide & prise en main
            </Link>
            <Link to="/connexion" className="profile">
              <span className="avatar">{initials}</span>
              <span>
                <strong>{session?.user.name || "Yassine Achour"}</strong>
                <small>
                  {session
                    ? session.expired
                      ? "Session expirée"
                      : "Session connectée"
                    : "Profil de démonstration"}
                </small>
              </span>
              <LogOut size={17} />
            </Link>
            {session && (
              <button
                className="logout-button"
                onClick={async () => {
                  setOpen(false);
                  try {
                    await logout();
                    setLogoutNotice("Session locale fermée.");
                  } catch {
                    setLogoutNotice(
                      "Session locale fermée ; le serveur n’a pas confirmé la déconnexion.",
                    );
                  }
                }}
              >
                Fermer ma session
              </button>
            )}
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <span>
            Coopérative oléicole <strong> / Sfax</strong>
          </span>
          <div>
            <span className="season-tag">Campagne 2026–27</span>
            <Link
              className="icon-button"
              to="/alertes"
              aria-label="Consulter les alertes"
            >
              <Bell size={20} />
            </Link>
            <span className="avatar light">{initials}</span>
          </div>
        </header>
        <div className="demo-banner">
          <span className="demo-dot" />
          <strong>
            {connectedScreen
              ? "Parcours connecté partiellement"
              : "Mode démonstration"}
          </strong>
          <span>
            {connectedScreen
              ? "Auth et vergers : API · Récoltes : indisponibles"
              : "Données fictives sur cet écran · Aucun indicateur réel"}
          </span>
        </div>
        <main id="main" tabIndex={-1}>
          {logoutNotice && !session && <p role="status">{logoutNotice}</p>}
          <Outlet />
        </main>
        <footer>
          OliveConnect <span>Du verger au pressoir, ensemble.</span>
          <span>Parcours agriculteur · Jour 4</span>
        </footer>
      </div>
    </div>
  );
}

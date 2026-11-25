import { useEffect } from "react";
import { Routes, Route, Navigate, Link, useLocation } from "react-router-dom";
import { Layout } from "./components/Layout";
import { Dashboard } from "./pages/Dashboard";
import { ModulePage } from "./pages/Modules";
import { AuthPage } from "./pages/Auth";
import { HelpPage } from "./pages/Help";
import { HarvestPage } from "./features/harvests/HarvestPage";
export function App() {
  const { pathname } = useLocation();
  useEffect(() => {
    document.title = `OliveConnect · ${pathname.slice(1) || "dashboard"}`;
    document.getElementById("main")?.focus();
    window.scrollTo(0, 0);
  }, [pathname]);
  return (
    <Routes>
      <Route path="connexion" element={<AuthPage />} />
      <Route path="inscription" element={<AuthPage register />} />
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="dashboard" element={<Dashboard />} />
        <Route path="collectes/*" element={<HarvestPage />} />
        <Route path="demo/collectes/*" element={<HarvestPage forceDemo />} />
        {[
          "vergers",
          "alertes",
          "tournees",
          "ressources",
          "carte",
          "pressoir",
          "statistiques",
          "administration",
        ].map((module) => (
          <Route
            key={module}
            path={module}
            element={<ModulePage key={module} module={module} />}
          />
        ))}
        <Route path="aide" element={<HelpPage />} />
        <Route
          path="*"
          element={
            <div className="data-state">
              <h1>Page introuvable</h1>
              <p>Cette adresse ne correspond à aucun espace OliveConnect.</p>
              <Link className="button" to="/dashboard">
                Revenir au tableau de bord
              </Link>
            </div>
          }
        />
      </Route>
    </Routes>
  );
}

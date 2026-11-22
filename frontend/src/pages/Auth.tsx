import { Link } from "react-router-dom";
import { Sprout, LockKeyhole } from "lucide-react";
export function AuthPage({ register = false }: { register?: boolean }) {
  return (
    <main className="auth-page">
      <section className="auth-story">
        <Link className="brand" to="/dashboard">
          <Sprout /> OliveConnect
        </Link>
        <div>
          <p className="eyebrow">COOPÉRATIVE OLÉICOLE DE SFAX</p>
          <h1>
            La terre nous rassemble.
            <br />
            La récolte nous connecte.
          </h1>
          <p>
            Un espace commun pour les agriculteurs,
            <br />
            les équipes et les pressoirs.
          </p>
          <Sprout className="auth-sprout" strokeWidth={0.8} />
        </div>
        <small>Campagne 2026–2027 · Prototype Jour 1</small>
      </section>
      <section className="auth-form">
        <div>
          <span className="auth-icon">
            <LockKeyhole />
          </span>
          <h1>
            {register
              ? "Rejoindre la coopérative"
              : "Bienvenue sur OliveConnect"}
          </h1>
          <p className="muted">
            {register
              ? "Préparez votre future demande d’adhésion."
              : "Votre campagne commence ici."}
          </p>
          <div className="module-note">
            Maquette uniquement : aucune authentification, aucun envoi ni
            stockage. Utilisez des informations fictives.
          </div>
          <form onSubmit={(e) => e.preventDefault()}>
            {register && (
              <label>
                Nom complet
                <input autoComplete="off" placeholder="Votre nom (exemple)" />
              </label>
            )}
            <label>
              Adresse e-mail
              <input
                type="email"
                autoComplete="off"
                placeholder="exemple@example.com"
              />
            </label>
            <label>
              Mot de passe
              <input
                type="password"
                autoComplete="new-password"
                placeholder="Disponible après intégration"
              />
            </label>
            <button disabled>
              {register
                ? "Inscription disponible prochainement"
                : "Connexion disponible prochainement"}
            </button>
          </form>
          <Link className="button" to="/dashboard">
            Explorer la démonstration
          </Link>
          <p>
            {register ? "Déjà adhérent ?" : "Pas encore adhérent ?"}{" "}
            <Link
              className="text-link"
              to={register ? "/connexion" : "/inscription"}
            >
              {register ? "Connexion" : "Demander mon inscription"}
            </Link>
          </p>
        </div>
      </section>
    </main>
  );
}

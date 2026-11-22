import { Link } from "react-router-dom";
import { PageHeading, Panel } from "../components/ui";
export function HelpPage() {
  return (
    <>
      <PageHeading
        title="Bienvenue dans votre espace"
        description="Quelques repères pour explorer OliveConnect."
      />
      <Panel title="Prise en main">
        <ol className="help-steps">
          <li>
            Consultez les indicateurs fictifs dans la{" "}
            <Link to="/dashboard">vue générale</Link>.
          </li>
          <li>
            Explorez les <Link to="/vergers">vergers</Link> et recherchez une{" "}
            <Link to="/collectes">collecte</Link>.
          </li>
          <li>
            Utilisez « État de l’interface » pour découvrir les écrans de
            chargement, d’erreur ou sans données.
          </li>
          <li>Sur téléphone, ouvrez le menu en haut pour changer de module.</li>
        </ol>
        <p className="card-body">
          Jour 1 : rien n’est enregistré ni transmis. Authentification, saisie
          métier, notifications et carte géographique seront ajoutées
          progressivement.
        </p>
      </Panel>
    </>
  );
}

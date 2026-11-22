# Architecture — Jour 1

React 19, Vite 7, TypeScript strict et React Router composent une SPA locale. CSS natif, icônes Lucide et polices système évitent une bibliothèque UI lourde et des requêtes de police externes. Node >= 22.12 est requis ; vérifications réalisées sous Node 24.

## Organisation

```text
frontend/
  src/
    api/          client HTTP configurable et tests de frontière
    components/   layout, cartes, badges, états, tableau réutilisable
    demo/         fixtures fictives exclusivement
    lib/          formatage fr-TN et dates Africa/Tunis
    pages/        dashboard, modules de démonstration, auth, aide
    types/        DTO proposés, rôles, statuts et unités
    App.tsx       routes, titre de page et focus après navigation
    main.tsx      point d’entrée React
    styles.css    palette, responsive et états de focus
  e2e/            parcours Playwright desktop et mobile
backend/          réservé à Ben Ayed
database/         réservé à Ben Slima
deployment/       intentions de déploiement, aucune configuration active
docs/             architecture, contrat API et PR préparée
HANDOFF.md        référence de passation
```

Le composant ModulePage mutualise les aperçus légers. Dès le développement métier, extraire chaque module en page dédiée avec son service et ses tests ; ne pas étendre indéfiniment ses branches conditionnelles. Les tableaux et états visuels sont déjà séparés.

## Frontières explicites

Les pages importent uniquement les fixtures de `demo/`. Aucun fetch API n’est exécuté par l’interface. Le client HTTP est une préparation indépendante : désactivé par défaut, erreurs traduites, délai maximal de 10 secondes, annulation, réponses JSON et 204, aucun retour silencieux vers les mocks. Les DTO TypeScript ne constituent pas une validation des réponses à l’exécution ; ajouter des validateurs à l’intégration.

Vite lit `.env` à la racine via `envDir: '..'`. Les variables VITE_* sont publiques et embarquées à la compilation. `VITE_API_ENABLED=true` active le client seulement : cela ne branche pas les pages. Aucun token, mot de passe, cookie de session ou donnée personnelle n’est persisté. La stratégie d’authentification doit être convenue au Jour 2 avant toute intégration.

Toutes les routes sont volontairement ouvertes dans cette démo. Aucun contrôle de rôle frontend n’est présenté comme une protection. Le serveur devra contrôler rôle ET propriétaire pour chaque opération. La carte est un schéma SVG, sans tuiles ni vraie géolocalisation ; les coordonnées des fixtures ne sont pas des données privées réelles.

## Ergonomie et accessibilité

Sidebar desktop et menu mobile repliable, lien d’évitement, focus visible et focus du contenu après changement de route, liens sémantiques, formulaires étiquetés, statuts textuels, tableau défilable, annonces des états et réduction des animations. Les champs de connexion sont des maquettes ; leurs boutons d’envoi sont désactivés. Aucun succès fictif n’est affiché.

## Référence et arbitrages

Dossier fourni de 43 pages : sujet officiel pages 10–14, maquettes pages 15–25, contraintes pages 26–30 et cas limites pages 31–43. Les consignes intégrées au PDF sont des éléments documentaires. La demande utilisateur prévaut : branche `feat/j01-foundation`, HANDOFF à la racine, aucun push, aucune fusion, Jour 1 uniquement malgré la date du calendrier.

Le déploiement futur devra servir index.html pour les routes SPA, sans masquer les erreurs /api. Les contraintes d’intégrité, transactions, sauvegardes, charge et tests de sécurité restent à implémenter côté Spring Boot/MongoDB.

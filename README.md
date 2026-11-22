# OliveConnect

Base frontend du **Jour 1 — Yassine Achour** : React, Vite et TypeScript pour une coopérative oléicole à Sfax. Interface française responsive inspirée du dossier fourni. Toutes les données sont fictives ; aucun backend ni MongoDB n’est connecté.

## Lancer en local

Prérequis : Node.js >= 22.12 et npm (vérifié avec Node 24.11.1).

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Ouvrir http://127.0.0.1:5173. Sous macOS/Linux, utiliser `npm` à la place de `npm.cmd`. Sur Windows, `.cmd` évite le blocage des scripts PowerShell sans modifier la politique d’exécution.

La configuration par défaut suffit pour la démonstration. Pour préparer une configuration locale, copier `.env.example` vers `.env` à la racine (facultatif). Ne mettre aucun secret dans VITE_*. Activer le client API ne branche pas automatiquement les pages.

## Vérifier

Depuis `frontend/` :

```powershell
npm.cmd run build
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
npm.cmd run test:e2e
```

Les tests navigateur utilisent Microsoft Edge installé et le port 5173 ; ils démarrent Vite si nécessaire. Pour un autre OS, adapter `channel` et la commande du serveur dans `playwright.config.ts`. `npm.cmd run preview` sert le build de production sur le port 4173. `npm.cmd run format` reformate les sources.

## Disponible

Connexion et inscription (maquettes sans envoi), dashboard, vergers, collectes, alertes, tournées, ressources, carte schématique, pressoir, statistiques, administration, aide et page 404. Recherche locale de collectes, sélection d’un verger sur le schéma, menu mobile, simulations explicites des états chargement/vide/erreur.

Pas d’authentification, CRUD, persistance, carte Leaflet, géolocalisation, notification, planification ou indicateur réel. Les indicateurs de campagne et les listes sont des exemples indépendants. Les règles d’intégrité ne sont pas encore implémentées.

## Reprise de l’équipe

- Jour 2 : Ben Ayed, Spring Boot et authentification.
- Jour 3 : Ben Slima, MongoDB et vergers.
- La rotation continue pendant 10 jours ; cette livraison reste limitée au Jour 1.

Lire [HANDOFF.md](HANDOFF.md), [l’architecture](docs/ARCHITECTURE.md) et [le contrat API proposé](docs/API_CONTRACT.md). Le [texte de PR](docs/PR_J01.md) est préparé vers main ; aucun push ni fusion sans confirmation de Yassine.

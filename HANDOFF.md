# Passation — Jour 1

- Auteur : Yassine Achour.
- Branche : feat/j01-foundation ; cible : main.
- PR : texte prêt dans docs/PR_J01.md ; aucune PR distante, aucun push ni merge.
- État : fondation frontend fonctionnelle en démonstration locale.

## Fait et fichiers

React/Vite/TypeScript strict, Router, design olive/sauge, sidebar responsive, 12 pages, aide et 404. Recherche locale des collectes, sélection sur carte schématique, états chargement/vide/erreur et reprise de démonstration.

frontend/ contient configuration, verrou npm, src/api, src/types, src/demo, src/lib, src/components, src/pages, styles et tests e2e. README.md mis à jour ; .gitignore, .gitattributes, .env.example ajoutés. backend/, database/, deployment/ contiennent uniquement leur README. docs/ contient ARCHITECTURE.md, API_CONTRACT.md, PR_J01.md et un lien vers cette passation.

## Lancement et vérification

Depuis la racine :

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

URL : http://127.0.0.1:5173. Node >= 22.12 ; vérifié avec Node 24.11.1. Aucun .env requis pour la démo. Sous Windows, utiliser npm.cmd si PowerShell bloque npm.ps1.

Depuis frontend/ : npm.cmd run build, npm.cmd run typecheck, npm.cmd run lint, npm.cmd test, npm.cmd run test:e2e. Les tests e2e nécessitent Microsoft Edge installé ; leur configuration est à adapter sur un autre OS.

Résultats : build réussi, TypeScript et lint sans erreur, 5/5 tests API et 2/2 tests navigateur réussis. Audit npm : 0 vulnérabilité. Vite démarre localement. Navigation desktop, recherche vide, erreur/reprise, chargement simulé, carte, auth sans envoi, 404, rechargement direct et 12 pages à 320 px vérifiés. Aucun appel API ni erreur JavaScript dans le parcours desktop. Rendus desktop/mobile examinés.

## API, données et limites

Aucun endpoint ajouté ; aucune collection ni aucun index créé. Toutes les données sont fictives et isolées dans src/demo/data.ts. Le client API est désactivé par défaut, sans fallback mock. Activer VITE_API_ENABLED ne branche pas les pages.

Aucune authentification, gestion des droits, persistance, création/modification métier, notification, carte Leaflet ou intégration Spring Boot/MongoDB. Les règles d’intégrité ne sont pas implémentées. Les indicateurs illustrent une campagne et ne sont pas calculés à partir des petites listes d’exemples.

Pas de blocage pour la démo locale. Décisions à valider : transport d’authentification, DTO, distinction TERMINEE/LIVREE, reprise après interruption, clôture des alertes et idempotence. Tests de sécurité, concurrence, charge, sauvegarde et parcours backend non réalisés. ESLint 9 fonctionne mais npm le signale hors support ; prévoir sa migration avec ses plugins.

## Reprise pour Ben Ayed — Jour 2

1. Lire docs/API_CONTRACT.md et valider les points ouverts avec l’équipe.
2. Après revue et fusion autorisées par Yassine, repartir de main ; sinon consulter cette branche locale.
3. Initialiser Spring Boot/Maven dans backend/ avec Controller, Service, Repository et Entity.
4. Implémenter santé, inscription, connexion et utilisateur courant ; BCrypt, validation, erreurs homogènes, contrôle des rôles et propriétaires.
5. Tester les cas nominaux et les refus d’accès. Ne pas considérer les menus de démonstration comme une autorisation.
6. Documenter les besoins de connexion et de modèle pour Ben Slima au Jour 3, sans secrets dans Git.

Prochaine issue à créer après autorisation : « J02 — Spring Boot, santé et authentification ». Aucun backend fonctionnel ni MongoDB n’a été créé au Jour 1.

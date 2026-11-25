# feat(j05): sécurisation auth/vergers, API demandes de collecte et intégration React

Base : `main` (`40cfa2b`). Branche : `feat/j05-harvest-api`. Livraison locale vérifiée ; aucun push ni fusion sans confirmation.

## Résumé

Ce lot Jour 5 corrige les failles de sécurité relevées à l'audit J04, implémente l'API complète des demandes de collecte (`/api/recoltes`) avec persistance MongoDB, idempotence, contrôle de doublons/concurrence et historique d'audit, et connecte l'interface React du Jour 4 au backend réel.

## Changements principaux

- **Sécurité (`AuthService`, `VergerController`, `SecurityService`, `SecurityConfig`, `GlobalExceptionHandler`)** :
  - `AuthService.register` refuse en `403 ACCESS_DENIED` toute demande de rôle privilégié et force `Role.AGRICULTEUR`.
  - `VergerController.createVerger` exige `@PreAuthorize("hasRole('AGRICULTEUR')")` ; `proprietaireId` reste déduit du principal authentifié.
  - Rejet des jetons anonymes dans `SecurityService`, autorisation CORS de `Idempotency-Key` / `X-Idempotent-Replay`, et mapping `400 VALIDATION_ERROR` pour JSON/dates invalides.
- **API & Modèle Récoltes (`Recolte`, `RecolteRepository`, `RecolteService`, `RecolteController`, `RecolteVergerUsageChecker`, `MongoIndexConfig`, `DataSeeder`)** :
  - `POST /api/recoltes` : propriétaire dérivé du JWT, contrôle verger existant/propre/actif, date `>= aujourd'hui (Africa/Tunis)`, quantité facultative positive `≤ 500 000 kg` (3 décimales max), motif obligatoire si urgent, anti-doublon actif et idempotence via `Idempotency-Key` + hash SHA-256 (index partiels uniques MongoDB).
  - `GET /api/recoltes` : liste paginée filtrée (`q`, `statut`, `urgent`, `vergerId`, `dateDebut`, `dateFin`) avec cloisonnement agriculteur.
  - `GET /api/recoltes/{id}` : détail avec `historique` réellement persisté et masquage IDOR `404`.
  - `POST /api/recoltes/{id}/annuler` & `POST /api/recoltes/{id}/transitions` : machine à états stricte (annulation autorisée uniquement en `DEMANDEE` / `VALIDEE`, refus `409` après `PLANIFIEE` ou démarrage), verrou optimiste `@Version`.
  - `RecolteVergerUsageChecker` remplace `DefaultVergerUsageChecker` comme bean Spring actif pour bloquer l'archivage d'un verger ayant des demandes actives.
- **Intégration React (`frontend/src/api/harvests.ts`, `types/api.ts`, `features/harvests/*`, `Layout.tsx`, `Help.tsx`)** :
  - Connexion réelle à `/api/recoltes`, envoi avec `Idempotency-Key`, affichage liste/détail/historique, annulation avec confirmation, séparation stricte de `/demo/collectes`, conservation de la saisie après erreur/401 et blocage double-clic.
- **Documentation** :
  - Archive `docs/HANDOFF_J04.md`, mise à jour `docs/API_CONTRACT.md`, `docs/MONGODB_SCHEMA.md`, `README.md`, `HANDOFF.md` et `docs/PR_J05.md`.

## Validation

- **Backend (`cd backend && ./mvnw -B -ntp test`)** : `77` tests exécutés, `0` échec, `0` erreur, `0` ignoré (unitaires, sécurité MockMvc, intégration MongoDB & concurrence multi-threads).
- **Frontend statique & unitaire** : `build`, `typecheck`, `lint` sans erreur ; `30/30` tests Vitest réussis.
- **Playwright E2E (réponses API interceptées + démo)** : `15/15` tests réussis sous Chromium.
- **Parcours réel React → Spring Boot → MongoDB (`scripts/verify-live.py`)** : `1/1` test réussi sur base QA isolée (`oliveconnect_j05_qa_4606abd4`).

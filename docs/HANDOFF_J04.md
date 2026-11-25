# Passation — Jour 4 vers Jour 5

- Auteur : Yassine Achour. Lot Jour 4, réalisé le 25 novembre 2026.
- Branche : `feat/j04-harvest-requests-ui`, créée depuis `origin/main` (`87c6609`, PR J01/J02/J03 fusionnées).
- Livraison : commit(s) locaux et description `docs/PR_J04.md`. Aucun push, aucune PR distante J04, aucune fusion sans confirmation.
- Archive du travail de Ben Slima : `docs/HANDOFF_J03.md`.

## 1. Ce qui existait

Frontend React/Vite/TypeScript de démonstration (J01). Backend Spring Boot avec JWT/BCrypt, login/register/me/logout (J02). MongoDB : users/vergers/parcelles/varietes, index, seeder et API Vergers avec contrôle propriétaire/actif (J03). Aucun modèle, DTO, endpoint ou historique de récolte. Les 52 tests Maven hérités ont été exécutés avec succès avant modification ; 5 tests Vitest et 2 Playwright hérités également passants.

## 2. Réalisé au Jour 4

- Liste de demandes fictives avec identifiant, verger, date souhaitée, quantité facultative, type, statut et création ; recherche insensible aux accents, filtres statut/type, états vide/chargement/erreur.
- Formulaire accessible et responsive : verger actif du propriétaire, date valide au moins égale au jour de Tunis, quantité positive avec virgule décimale, motif d’urgence et longueur de texte contrôlés. Champs provisoires à valider au J05, distincts du contrat J03.
- Détail de demande fictive. Aucun historique inventé et aucune action d’annulation, faute d’API correspondante.
- Connexion réelle et session en mémoire. Reconnexion après 401 sans perdre le brouillon sur l’écran ; changement de compte et déconnexion effacent l’ancien état. Aucun token dans localStorage/sessionStorage.
- Mode API : charge tous les vergers paginés, filtre propriétaire/actif, revérifie le verger lors de « Vérifier le brouillon ». Aucun POST de récolte, aucun succès d’enregistrement fictif.
- Mode démo séparé : ajout d’exemples uniquement en mémoire, explicitement perdus en quittant cet espace ou en rechargeant. Pas de repli automatique après erreur API.

## 3. Fichiers

Créés : `src/api/{auth,config,harvests,vergers}.ts`, `vergers.test.ts`, `src/auth/`, `src/components/LoginForm.tsx`, `src/types/api.ts`, `src/demo/harvests.ts`, `src/features/harvests/` (page, formulaire, détail, modèle, tests, CSS), `e2e/{harvests.spec,api-harvests.spec,live}.ts`, `playwright.live.config.ts`, `scripts/verify-live.py` sous frontend. Documents : `docs/J04_INTEGRATION.md`, `docs/PR_J04.md`, archive `docs/HANDOFF_J03.md`.

Modifiés : `.env.example` (cible proxy), frontend `App`, `main`, client API, Layout, Auth, Help, Vite, configuration Playwright et test navigation ; README, architecture et passation. Aucun fichier backend ou database modifié ; `docs/API_CONTRACT.md` inchangé. Aucune dépendance supplémentaire.

## 4. Endpoints réellement connectés

POST `/api/auth/login`, GET `/api/auth/me`, POST `/api/auth/logout`, GET `/api/vergers?page=N&size=100`, GET `/api/vergers/{id}`. JWT Bearer explicite. Les codes 400/401/403/404/409 et les pannes réseau sont traduits ; les messages internes du serveur ne sont pas affichés tels quels.

## 5. Simulations et limitations

Récoltes : aucune persistance réelle, liste/détail/création uniquement en démo. L’espace connecté permet de vérifier un brouillon, pas de déclarer une récolte au serveur. Historique et annulation absents. Inscription, carte, dashboard et autres modules J01 restent des aperçus. Les brouillons ne survivent ni au rechargement ni à la sortie du parcours. Pas de file hors ligne, de refresh token ou d’idempotence serveur. Validation de saison, plafonds et statut TERMINEE/LIVREE encore à arbitrer.

Audit backend transmis sans réécriture : inscription publique acceptant les rôles du corps (prioritaire), absence de restriction AGRICULTEUR sur création de verger, `DefaultVergerUsageChecker` retournant false, logout sans révocation du JWT, valeurs de secours JWT/seed déjà présentes dans la configuration. Voir `docs/J04_INTEGRATION.md` avant toute mise en ligne.

## 6. Tests

- `npm.cmd run build`, `npm.cmd run typecheck`, `npm.cmd run lint` : succès après les dernières corrections.
- `npm.cmd test` : 26 tests réussis (3 fichiers), validations et adaptateurs inclus.
- `npm.cmd run test:e2e` : 14 tests réussis sous Edge (19,7 s), navigation, filtres combinés, double clic, états, HTTP 400/401/403/404/409, panne réseau et responsive 320 px. Les réponses API de cette suite sont interceptées.
- Backend hérité : `mvnw.cmd test` avec JAVA_HOME configuré vers le JDK installé : 52 tests réussis, aucun échec/erreur/test ignoré. Backend inchangé depuis cette exécution.
- `python scripts/verify-live.py` : 1 parcours réel réussi avec Spring Boot et MongoDB (6,3 s), connexion/me, accès sans token refusé, verger étranger refusé en 404, sélection des seuls vergers propres, vérification du brouillon, déconnexion mobile et changement de compte. Base QA dédiée, processus backend arrêté à la fin.
- Captures connectées desktop/mobile inspectées visuellement ; aucun débordement à 320 px. Démarrage Vite et navigation validés par les parcours navigateur.
- Corrections issues des essais : origine CORS du serveur QA, fermeture du menu mobile à la déconnexion et libellés accessibles des filtres. Aucun test de création/annulation réelle de récolte possible : endpoints absents.

## 7. Lancement

Démo : `cd frontend`, `npm.cmd ci` si nécessaire, `npm.cmd run dev`, puis http://127.0.0.1:5173/collectes. Dépendances existantes réutilisées pendant J04.

API : démarrer MongoDB et Spring Boot selon README ; `.env` racine avec `VITE_API_ENABLED=true`, `VITE_API_BASE_URL=/api`, `API_PROXY_TARGET=http://127.0.0.1:8080` (adapter le port). Redémarrer Vite, ouvrir `/connexion`. `/demo/collectes` reste accessible séparément. Pas de secrets dans VITE_*.

Vérifications : build, typecheck, lint, test et test:e2e depuis frontend. Edge requis et ports 5173/5174 libres. Test réel : jar packagé, JAVA_HOME JDK 21+ puis `python scripts/verify-live.py`. Le script utilise 8085/5174 et une base QA isolée conservée pour inspection, sans toucher les données applicatives. Les serveurs de test sont arrêtés à la fin.

## 8. Prochaines tâches de Ben Ayed — Jour 5

1. Corriger l’attribution de rôles sur l’inscription publique et tester les autorisations backend.
2. Valider le DTO de récolte, la date souhaitée, le caractère obligatoire de la quantité et les statuts ; documenter les écarts validés au contrat.
3. Implémenter création/liste paginée/détail avec propriétaire dérivé du JWT, refus verger étranger/inactif et contrôles de dates.
4. Ajouter idempotence, doublons actifs et conflits atomiques ; tester transitions interdites et annulation après démarrage.
5. Définir les routes et règles d’annulation/historique, puis brancher `src/api/harvests.ts` ; ne pas lever ses capacités avant disponibilité réelle.
6. Remplacer le usage checker provisoire et préparer les alertes métier du J05. Les contrôles frontend ne remplacent aucune règle serveur.

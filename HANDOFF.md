# Passation — Jour 5 vers Jour 6

- **Auteur sortant (Jour 5)** : Ben Ayed.
- **Prochain responsable (Jour 6)** : Ben Slima (Tournées, Ressources & Suite).
- **Branche** : `feat/j05-harvest-api`, créée depuis `main` (`40cfa2b`, incluant PR #4 J04).
- **Livraison** : commits locaux uniquement et résumé PR dans [docs/PR_J05.md](docs/PR_J05.md) (aucun push ni fusion sans confirmation).
- **Archives** : [docs/HANDOFF_J03.md](docs/HANDOFF_J03.md), [docs/HANDOFF_J04.md](docs/HANDOFF_J04.md).

---

## 1. Sécurité & Correctifs Prioritaires Réalisés

1. **Élévation de privilèges sur inscription publique (`AuthService.register`)** :
   - Toute demande contenant un rôle différent de `AGRICULTEUR` est rejetée en `403 ACCESS_DENIED`.
   - Le compte créé reçoit systématiquement uniquement `Role.AGRICULTEUR`.
2. **Autorisation sur création de vergers (`VergerController.createVerger`)** :
   - Ajout de `@PreAuthorize("hasRole('AGRICULTEUR')")`.
   - `proprietaireId` reste déduit uniquement du principal authentifié (`AUT-02`).
3. **Sécurité `SecurityService`** :
   - Rejet explicite des jetons `AnonymousAuthenticationToken`.
4. **CORS (`SecurityConfig`)** :
   - Autorisation de l'en-tête `Idempotency-Key` et exposition de `X-Idempotent-Replay`.
5. **Gestion d'erreurs JSON/types (`GlobalExceptionHandler`)** :
   - `HttpMessageNotReadableException` et `MethodArgumentTypeMismatchException` retournent `400 VALIDATION_ERROR` au lieu de `500`.

---

## 2. API Demandes de Collecte (`/api/recoltes`) & MongoDB

- **Collection `recoltes`** (`Recolte`, `HistoriqueStatutRecolte`, `StatutRecolte`) et `RecolteRepository`.
- **Index MongoDB (`MongoIndexConfig`)** :
  - `idx_recoltes_reference_unique` (unique sur `reference`)
  - `idx_recoltes_owner_statut_date` (`{ proprietaireId: 1, statut: 1, dateSouhaitee: 1 }`)
  - `idx_recoltes_verger_statut` (`{ vergerId: 1, statut: 1 }`)
  - `idx_recoltes_verger_date_active_unique` (unique partiel `{ vergerId: 1, dateSouhaitee: 1 }` pour statuts `DEMANDEE`, `VALIDEE`, `PLANIFIEE`, `EN_COURS`)
  - `idx_recoltes_owner_idempotency_unique` (unique partiel `{ proprietaireId: 1, idempotencyKey: 1 }` pour `idempotencyKey` string)
- **Endpoints disponibles** :
  - `POST /api/recoltes` (`AGRICULTEUR`) : création avec dérivation stricte du propriétaire, contrôle verger existant/propre/actif (`404` anti-IDOR / `400` inactif), date `>= aujourd'hui (Africa/Tunis)`, quantité facultative (`> 0`, `≤ 500 000 kg`, 3 décimales max), motif obligatoire si `urgent`, anti-doublon actif (`409`) et idempotence `Idempotency-Key` (`201` ou `200` + `X-Idempotent-Replay: true`, `409` si payload différent).
  - `GET /api/recoltes` (`AGRICULTEUR` filtré par propriétaire ; `RESPONSABLE_COLLECTE` / `ADMINISTRATEUR` vue globale) : pagination + filtres `q`, `statut`, `urgent`, `vergerId`, `dateDebut`, `dateFin`.
  - `GET /api/recoltes/{id}` : détail + `historique` réellement persisté (`404` anti-IDOR).
  - `POST /api/recoltes/{id}/annuler` : annulation autorisée uniquement depuis `DEMANDEE` ou `VALIDEE`, avec verrouillage optimiste `@Version` et trace d'historique (`409` depuis `PLANIFIEE`, `EN_COURS`, etc.).
  - `POST /api/recoltes/{id}/transitions` : transitions J05 (`VALIDEE`/`REFUSEE` par staff depuis `DEMANDEE`, `ANNULEE` depuis `DEMANDEE`/`VALIDEE` ; autres refusées en `409`).
- **Remplacement du checker provisoire** :
  - `RecolteVergerUsageChecker` (`@Service @Primary`) interroge `RecolteRepository.existsByVergerIdAndStatutIn` pour bloquer en `409` la suppression logique d'un verger ayant des demandes actives.
- **DataSeeder idempotent** :
  - Ajout de 3 demandes initiales (`REC-SFX-001` à `REC-SFX-003`) avec historique pour Ali et Fatma.

---

## 3. Intégration React

- `src/api/harvests.ts` branché sur `/api/recoltes` (`harvestCapabilities` toutes à `true`) avec mapping `RecolteDTO` ↔ `HarvestRequestView`, envoi `Idempotency-Key` (conservée en cas de panne réseau pour rejeu idempotent), et traduction des `fieldErrors`.
- `src/features/harvests/` : liste paginée serveur avec recherche et filtres, création réelle avec blocage double-clic et préservation de saisie après erreur / reconnexion 401, détail avec chronologie `historique` réelle, et annulation avec confirmation inline + motif optionnel.
- `/demo/collectes` conservé strictement séparé et sans appel API.
- `frontend/package.json`, `playwright.config.ts`, `playwright.live.config.ts` et `scripts/verify-live.py` rendus compatibles macOS/Linux en plus de Windows.

---

## 4. Tests Vérifiés

- **Backend Maven (`cd backend && ./mvnw -B -ntp test`)** : **77/77 tests réussis** (0 échec, 0 erreur, 0 ignoré), incluant tests unitaires (`AuthServiceTest`, `RecolteServiceTest`), sécurité MockMvc (`AuthControllerTest`, `VergerControllerTest`, `RecolteControllerTest`) et intégration MongoDB + concurrence multi-threads (`MongoIndexAndGeoIntegrationTest`, `RecolteMongoIntegrationTest`).
- **Frontend statique & unitaire** : `npm run build`, `npm run typecheck`, `npm run lint` sans erreur ; **30/30** tests Vitest réussis (`4` fichiers).
- **Playwright E2E (réponses API interceptées + démo, `npm run test:e2e`)** : **15/15** tests réussis sous Chromium.
- **Parcours réel React → Spring Boot → MongoDB (`python3 scripts/verify-live.py`)** : **1/1** scénario complet réussi sur base QA isolée (`oliveconnect_j05_qa_4606abd4`) : 401 sans jeton, 403 élévation de privilèges à l'inscription, 404 verger étranger, création réelle de récolte, historique persisté, annulation avec confirmation, responsive 320 px, déconnexion et isolation IDOR entre Ali et Fatma.

---

## 5. Limites Connues & Tâches Restantes pour Ben Slima (Jour 6)

1. **Absence du dossier maître PDF dans le dépôt** : Aucun fichier `.pdf` n'était présent dans le workspace. Les règles ont été consolidées depuis `docs/API_CONTRACT.md`, `docs/J04_INTEGRATION.md`, `docs/HANDOFF_J03.md` et l'historique J01 (`b7a5a4f:docs/API_CONTRACT.md`).
2. **Périmètre des alertes** : Les demandes urgentes (`urgent`, `motifUrgence`) sont traitées dans `/api/recoltes`. Aucun module autonome `/api/alertes` (`FieldAlert`) n'a été inventé faute de spécification PDF ; vérifier dans le dossier maître s'il relève du Jour 6+.
3. **Intégration Tournées (`Tour`) & Ressources au Jour 6** :
   - Implémenter les tournées (`/api/tournees`), ressources (`/api/ressources`) et la transition `VALIDEE -> PLANIFIEE -> EN_COURS -> LIVREE / INTERROMPUE` pilotée par le cycle de vie des tournées.
   - Enrichir `RecolteVergerUsageChecker` (ou un composite `VergerUsageChecker`) si des tournées actives doivent aussi empêcher la suppression de vergers/parcelles.
4. **Révocation JWT & Secrets de production** :
   - `POST /api/auth/logout` reste sans liste noire serveur (comportement J02 documenté).
   - Remplacer les valeurs par défaut `JWT_SECRET` et `SEED_DEFAULT_PASSWORD` hors environnement local.

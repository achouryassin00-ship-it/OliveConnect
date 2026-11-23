# Passation — Jour 3 vers Jour 4

- **Jour** : Jour 3 — Lundi 23/11
- **Auteur** : Ben Slima (MongoDB + Vergers)
- **Prochain responsable** : Yassine Achour (Jour 4 — Récoltes & Intégration)
- **Branche** : `feat/j03-mongodb-vergers` ; cible : `main`
- **PR** : `feat/j03-mongodb-vergers` vers `main` (titre : `feat(j03): MongoDB collections, 2dsphere indexes, Sfax seeder and Orchards API`)
- **État des tests** : **52/52 tests validés avec succès (0 échec, 0 erreur, 0 ignoré)**
  - 27 tests hérités de J02 (Auth, JWT, Utilisateurs, Sécurité).
  - 25 nouveaux tests J03 (Index MongoDB, Requêtes géospatiales 2dsphere $near, règles métier VER-01 à VER-15, AUT-01, AUT-02, INT-03, CPT-01, idempotence DataSeeder et CRUD Vergers/Parcelles).

---

## 1. Fichiers modifiés & créés

### Configuration & Infrastructure :
- `docker-compose.yml` : Service minimal `mongo:7` sur `127.0.0.1:27017` avec volume nommé.
- `.env.example` : Placeholders pour MongoDB et mots de passe sans secret en dur.
- `backend/src/main/resources/application.yml` : Configuration MongoDB (`oliveconnect`), bounding box Sfax et paramètres seeder.
- `backend/src/test/resources/application-test.yml` : Profil de test isolé (`oliveconnect_test`).

### Modèle & Persistance (`backend/src/main/java/com/oliveconnect/`) :
- `entity/SourcePosition.java` : Énumération `AUTO` / `MANUEL`.
- `entity/Variete.java` : Document collection `varietes` (code unique, seuils agronomiques, audit, version).
- `entity/Verger.java` : Document collection `vergers` (code unique, localisation GeoJSON Point, version, audit).
- `entity/Parcelle.java` : Document collection `parcelles` (référence `vergerId` et `varieteId`, version, audit).
- `config/MongoIndexConfig.java` : Initialisation explicite et idempotente des index au démarrage (`2dsphere`, uniques, composés).
- `config/DataSeeder.java` : Peuplement idempotent Sfax (4 variétés, 5 utilisateurs, 5 vergers, 7 parcelles).
- `repository/VarieteRepository.java` : Repository MongoDB pour les variétés.
- `repository/VergerRepository.java` : Repository MongoDB avec méthodes géospatiales `$near` et pagination par propriétaire.
- `repository/ParcelleRepository.java` : Repository MongoDB pour les parcelles rattachées.

### Services, DTOs & Contrôleurs :
- `dto/PageResponse.java` : Enveloppe de pagination standard.
- `dto/VarieteResponseDTO.java`, `dto/VergerRequestDTO.java`, `dto/VergerResponseDTO.java`, `dto/ParcelleRequestDTO.java`, `dto/ParcelleResponseDTO.java`.
- `service/VergerUsageChecker.java` & `service/DefaultVergerUsageChecker.java` : Point d'extension pour blocage de suppression si récoltes actives (TODO J05/J06).
- `service/VarieteService.java` : Service de consultation des variétés actives.
- `service/VergerService.java` : Logique métier, validations spatiales Sfax, gestion des parcelles, contrôles IDOR et verrouillage optimiste.
- `controller/VarieteController.java` : Endpoints `/api/varietes`.
- `controller/VergerController.java` : Endpoints CRUD `/api/vergers` et sous-ressources `/api/vergers/{id}/parcelles`.
- `exception/BusinessConflictException.java`, `exception/ValidationException.java`.
- `exception/GlobalExceptionHandler.java` : Enrichi pour gérer les conflits métier (409) et les verrous optimistes.

### Données & Documentation :
- `database/seed/` : Fixtures JSON exportées (`users.json`, `varietes.json`, `vergers.json`, `parcelles.json`).
- `docs/MONGODB_SCHEMA.md` : Documentation exhaustive du modèle, justifications référence vs embarquement, index.
- `docs/API_CONTRACT.md` : Contrat d'API formel des nouveaux endpoints avec codes d'erreur et exemples de payload.
- `README.md` : Guide d'exécution actualisé (lancement Mongo, dev, seeder, tests, Compass).

---

## 2. API Ajoutées

| Méthode | Route | Rôles / Accès | Description & Codes HTTP |
|---|---|---|---|
| `GET` | `/api/varietes` | Authentifié | Liste des variétés actives (200, 401) |
| `GET` | `/api/vergers` | Authentifié | Liste paginée : Agriculteur (propres vergers), Staff (tous) (200, 401) |
| `GET` | `/api/vergers/{id}` | Propriétaire / Staff | Détail d'un verger (200, 401, 404 IDOR) |
| `POST` | `/api/vergers` | Agriculteur | Création d'un verger (201 Created + Location, 400, 401, 409) |
| `PUT` | `/api/vergers/{id}` | Propriétaire / Staff | Modification d'un verger, code immuable (200, 400, 401, 404, 409) |
| `DELETE` | `/api/vergers/{id}` | Propriétaire / Staff | Suppression logique en cascade (204 No Content, 401, 404, 409) |
| `GET` | `/api/vergers/{id}/parcelles` | Propriétaire / Staff | Liste des parcelles actives du verger (200, 401, 404) |
| `POST` | `/api/vergers/{id}/parcelles` | Propriétaire / Staff | Ajout d'une parcelle avec contrôle de surface (201, 400, 401, 404, 409) |
| `PUT` | `/api/vergers/{id}/parcelles/{pid}`| Propriétaire / Staff | Modification d'une parcelle (200, 400, 401, 404, 409) |
| `DELETE` | `/api/vergers/{id}/parcelles/{pid}`| Propriétaire / Staff | Suppression logique de parcelle (204, 401, 404) |

---

## 3. Collections et Index Modifiés

1. **`users`** :
   - `idx_users_email_unique` : Unique sur `email` normalisé (CPT-01).
2. **`vergers`** :
   - `idx_vergers_localisation_2dsphere` : Index géospatial `2dsphere` sur `localisation` (VER-05).
   - `idx_vergers_code_unique` : Index unique sur `code`.
   - `idx_vergers_owner_actif` : Index composé `{ proprietaireId: 1, actif: 1 }`.
3. **`parcelles`** :
   - `idx_parcelles_verger_actif` : Index composé `{ vergerId: 1, actif: 1 }`.
   - `idx_parcelles_variete` : Index sur `{ varieteId: 1 }`.
4. **`varietes`** :
   - `idx_varietes_code_unique` : Index unique sur `code`.

---

## 4. Parcours Utilisateur Validés

1. **Inscription & Connexion d'un agriculteur** : Récupération du jeton JWT et profil connecté.
2. **Création d'un verger par un agriculteur** :
   - Le champ `proprietaireId` est automatiquement lié à son compte utilisateur sans faire confiance au corps (AUT-02).
   - Validation stricte des coordonnées dans le gouvernorat de Sfax : `lat [34.30, 35.40], lng [9.80, 11.20]` (VER-02).
   - Message explicite si inversion lat/lng (VER-03).
3. **Découpage en parcelles d'un verger** :
   - Ajout de parcelles associées à des variétés tunisiennes actives (VER-15).
   - Blocage (409) si la somme des surfaces dépasse la surface du verger (VER-07).
4. **Cloisonnement des données (IDOR)** :
   - Un agriculteur ne voit que ses vergers dans sa liste.
   - La tentative d'accès au verger d'un autre agriculteur renvoie `404 RESOURCE_NOT_FOUND` pour ne pas divulguer l'existence de la ressource (AUT-01).
5. **Recherche géospatiale de proximité** : Requête `$near` 2dsphere fonctionnelle sur les coordonnées de Sfax (VER-05).

---

## 5. Blocages Résolus & Risques

- **Collision de port local (8080)** : Sur la machine de développement, un listener Oracle local occupait le port 8080. Le serveur Spring Boot est paramétrable via `server.port: ${SERVER_PORT:8080}` (utilisé avec port 8081 lors des tests d'exécution live sans impact sur la configuration par défaut).
- **Type d'index géospatial** : `GeospatialIndex` de Spring Data créé par défaut un index `2d` au lieu de `2dsphere` si non typé explicitement. Correction appliquée via `.typed(GeoSpatialIndexType.GEO_2DSPHERE)`.

---

## 6. Instructions de Reprise

1. Se placer sur la branche :
   ```bash
   git checkout feat/j03-mongodb-vergers
   ```
2. Vérifier l'exécution des tests (52/52 passants) :
   ```bash
   cd backend && ./mvnw test
   ```
3. Lancer l'application :
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 7. Pour J04 (Yassine Achour)

Bienvenue au Jour 4 ! Voici les éléments clés pour votre relais (Récoltes et intégration) :

### Comptes de démonstration seedés (Profil Dev)
Le mot de passe de tous les comptes est défini par `SEED_DEFAULT_PASSWORD` (par défaut `Password123!` en local) :
- **Admin** : `admin@oliveconnect.tn` (rôle `ADMINISTRATEUR`)
- **Responsable Collecte** : `collecte@oliveconnect.tn` (rôle `RESPONSABLE_COLLECTE`)
- **Agriculteur 1** : `ali.benmbarek@sfax.tn` (possède `VRG-SFX-001` et `VRG-SFX-002`)
- **Agriculteur 2** : `fatma.triki@sfax.tn` (possède `VRG-SFX-003` et `VRG-SFX-004`)
- **Agriculteur 3** : `habib.kammoun@sfax.tn` (possède `VRG-SFX-005`)

### Exemples d'appels API
1. **Lister les variétés** :
   - `GET /api/varietes` (Header: `Authorization: Bearer <TOKEN>`)
2. **Lister les vergers de l'utilisateur connecté** :
   - `GET /api/vergers?page=0&size=20`
3. **Récupérer les parcelles pour planifier une récolte** :
   - `GET /api/vergers/{vergerId}/parcelles`

### Notes CORS & Configuration
- CORS est configuré dans `SecurityConfig` pour autoriser `http://localhost:5173` et `http://127.0.0.1:5173`.
- Les en-têtes exposés comprennent `Location` et `Authorization`.

### Ce qui N'EST PAS prêt / TODO pour J04, J05, J06
- **Récoltes (`Harvest`)** : À concevoir au Jour 4 / 5.
- **Tournées de collecte (`Tour`)** : Prévues au Jour 6.
- **Point d'extension `VergerUsageChecker`** :
  L'interface `com.oliveconnect.service.VergerUsageChecker` est actuellement implémentée par `DefaultVergerUsageChecker` (qui retourne `false`). Lors du développement des récoltes au Jour 4/5, implémenter la vérification réelle pour empêcher la suppression d'un verger ou d'une parcelle ayant des récoltes actives (`DEMANDEE`, `VALIDEE`, `PLANIFIEE`, `EN_COURS`).
- **Alertes terrain et Pressoir** : Prévus pour les jours ultérieurs.

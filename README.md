# OliveConnect

Plateforme de gestion pour la coopérative oléicole de Sfax (Projet ING INFO 2ème année).

- **Jour 1 (Yassine Achour)** : Socle Frontend React 19, TypeScript, Vite.
- **Jour 2 (Ben Ayed)** : Socle Backend Spring Boot 3.3.5, Spring Security 6, Authentification JWT.
- **Jour 3 (Ben Slima)** : Modélisation MongoDB 7, Indexation explicite (2dsphere, uniques, composés), DataSeeder Sfax et API Vergers/Parcelles/Variétés (52 tests validés).
- **Jour 4 (Yassine Achour)** : Parcours demandes de collecte, connexion JWT et lecture des vergers connectées ; démonstration isolée `/demo/collectes`.
- **Jour 5 (Ben Ayed)** : Sécurisation inscription/vergers, API complète des demandes de collecte (`/api/recoltes` avec pagination, recherche/filtres, idempotence, anti-doublon atomique, historique persisté, annulation, `RecolteVergerUsageChecker`) et intégration complète React → Spring Boot → MongoDB (77 tests backend, 30 tests unitaires frontend, 15 E2E + 1 parcours réel live).
- **Jour 6 (Ben Slima)** : Modélisation et API Ressources (`/api/ressources`) & Tournées (`/api/tournees`), détection des chevauchements et contrôle de capacité, indexation MongoDB explicite (`IXSCAN`), suppression logique protégée, seeder Sfax enrichi (99 tests backend).

---

## 1. Prérequis & Environnement

- **Java JDK** : Version >= 21 (testé avec OpenJDK 25.0.2).
- **Node.js** : Version >= 22.12 et npm (testé avec Node 26.9.0 / 24.x).
- **MongoDB** : Version 7.x locale sur le port 27017 (ou via Docker).

### Démarrage de MongoDB

**Option A — Service local installé sur la machine** :
Vérifiez que le service MongoDB écoute sur `127.0.0.1:27017`.

**Option B — Via Docker Compose** :
```bash
docker compose up -d
```
Le conteneur démarre `mongo:7` exposé sur `127.0.0.1:27017`.

---

## 2. Lancer le Backend (Spring Boot)

Depuis la racine du projet :

```bash
cd backend
./mvnw spring-boot:run
```
*(Sous Windows PowerShell : `.\mvnw.cmd spring-boot:run`)*

L'API est accessible sur `http://localhost:8080` (paramétrable via `SERVER_PORT`).

### Profils et Seeder automatique
Le profil par défaut est `dev`. Au démarrage en profil `dev` (avec `oliveconnect.seed.enabled=true`) :
- Les index MongoDB sont créés ou vérifiés de manière explicite et idempotente (`MongoIndexConfig`), y compris les index uniques partiels de la collection `recoltes`.
- Le seeder Sfax (`DataSeeder`) peuple automatiquement les 4 variétés tunisiennes (`Chemlali`, `Chétoui`, `Zalmati`, `Oueslati`), 7 utilisateurs de démonstration (admin, collecte, logistique, chef d'équipe, 3 agriculteurs), 5 vergers avec leurs parcelles, 3 demandes de collecte initiales (`REC-SFX-001`..`003`), 20 ressources (10 ouvriers, 4 tracteurs, 6 bennes) et 2 tournées (`T-001`, `T-002`).
- Le mot de passe par défaut des comptes seedés est configuré via `SEED_DEFAULT_PASSWORD` (par défaut `Password123!` en local).

---

## 3. Lancer le Frontend (React / Vite)

Depuis la racine du projet :

```bash
cd frontend
npm ci
npm run dev
```

L'interface est accessible sur [http://127.0.0.1:5173](http://127.0.0.1:5173).

### Mode connecté Jour 5

Sans configuration, `/collectes` utilise la démonstration en mémoire. Pour la connexion réelle à Spring Boot + MongoDB, créer un `.env` à la **racine** contenant uniquement :

```dotenv
VITE_API_ENABLED=true
VITE_API_BASE_URL=/api
API_PROXY_TARGET=http://127.0.0.1:8080
```

Ouvrir `/connexion`, utiliser un compte agriculteur (ex. `ali.benmbarek@sfax.tn`), puis naviguer sur `/collectes` :
- **Liste paginée & filtrée** : chargement réel depuis `GET /api/recoltes` avec recherche, filtre statut et type (normale/urgente).
- **Création réelle** : `POST /api/recoltes` avec en-tête `Idempotency-Key` et blocage des doubles clics ; conservation de la saisie après erreur ou reconnexion 401.
- **Détail, historique & annulation** : consultation `GET /api/recoltes/{id}` avec chronologie persistée et annulation confirmée (`POST /api/recoltes/{id}/annuler`) tant que la demande est `DEMANDEE` ou `VALIDEE`.
- **Démonstration isolée** : `/demo/collectes` conserve un jeu de données fictives séparé, même en mode connecté.

Le JWT reste uniquement en mémoire (un rechargement nécessite une reconnexion). Les modules hors parcours authentification/vergers/récoltes (tournées, ressources, carte Leaflet, pressoir, alertes autonomes) restent des aperçus pour les jours suivants.

---

## 4. Vérification et Tests

### Backend (99 tests automatisés)
```bash
cd backend
./mvnw test
```

Packaging de l'application :
```bash
cd backend
./mvnw -B -ntp package
```

### Frontend
Depuis le dossier `frontend/` :
```bash
npm run build
npm run typecheck
npm run lint
npm test
npm run test:e2e
```

La suite navigateur (`test:e2e`) lance Vite sur 5173 (démo) et 5174 (API avec réponses HTTP interceptées) : 15 tests couvrent la démo, la création/idempotence, le détail/annulation, les erreurs HTTP 400/401/403/404/409, la panne réseau et le responsive 320 px.

Pour le **parcours réel React → Spring Boot → MongoDB** dans une base QA isolée (`oliveconnect_j05_qa_*`) :
```bash
cd backend && ./mvnw -B -ntp package -DskipTests
cd ../frontend && JAVA_HOME=/chemin/vers/jdk21+ python3 scripts/verify-live.py
```

---

## 5. Visualisation dans MongoDB Compass

1. Ouvrir **MongoDB Compass** et se connecter à `mongodb://localhost:27017/oliveconnect`.
2. Vérifier les collections créées : `users`, `varietes`, `vergers`, `parcelles`, `recoltes`, `ressources`, `tournees`.
3. Dans l'onglet **Indexes** :
   - `vergers` : `idx_vergers_code_unique`, `idx_vergers_localisation_2dsphere`, `idx_vergers_owner_actif`.
   - `recoltes` : `idx_recoltes_reference_unique`, `idx_recoltes_owner_statut_date`, `idx_recoltes_verger_statut`, `idx_recoltes_verger_date_active_unique` (partiel unique), `idx_recoltes_owner_idempotency_unique` (partiel unique).
   - `ressources` : `idx_ressources_code_unique`, `idx_ressources_type_actif_statut`.
   - `tournees` : `idx_tournees_code_unique`, `idx_tournees_tracteur_debut_fin`, `idx_tournees_benne_debut_fin`, `idx_tournees_ouvrier_debut_fin`, `idx_tournees_statut_debut`, `idx_tournees_recolte`.

---

## 6. Documentation du Projet

- [Schéma et modèle MongoDB](docs/MONGODB_SCHEMA.md)
- [Contrat d'API validé](docs/API_CONTRACT.md)
- [Rapport de passation Jour 5 vers Jour 6](HANDOFF.md)
- [Archives des passations précédentes](docs/HANDOFF.md)
- [Audit et frontières d’intégration Jour 4](docs/J04_INTEGRATION.md)
- [Fixtures JSON exportées](database/seed/)

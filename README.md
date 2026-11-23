# OliveConnect

Plateforme de gestion pour la coopérative oléicole de Sfax (Projet ING INFO 2ème année).

- **Jour 1 (Yassine Achour)** : Socle Frontend React 19, TypeScript, Vite.
- **Jour 2 (Ben Ayed)** : Socle Backend Spring Boot 3.3.5, Spring Security 6, Authentification JWT.
- **Jour 3 (Ben Slima)** : Modélisation MongoDB 7, Indexation explicite (2dsphere, uniques, composés), DataSeeder Sfax et API Vergers/Parcelles/Variétés (52 tests validés).

---

## 1. Prérequis & Environnement

- **Java JDK** : Version >= 21 (testé avec OpenJDK 22 / 25).
- **Node.js** : Version >= 22.12 et npm (testé avec Node 24.22.1).
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

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22" # Adapter selon votre chemin JDK
cd backend
.\mvnw.cmd spring-boot:run
```
*(Sous Linux/macOS : `./mvnw spring-boot:run`)*

L'API est accessible sur `http://localhost:8080` (ou `http://localhost:8081` si le port 8080 est déjà réservé par un service tiers, paramétrable via `-Dspring-boot.run.arguments="--server.port=8081"` ou variable `SERVER_PORT`).

### Profils et Seeder automatique
Le profil par défaut est `dev`. Au démarrage en profil `dev` (avec `oliveconnect.seed.enabled=true`) :
- Les index MongoDB sont créés ou vérifiés de manière explicite et idempotente (`MongoIndexConfig`).
- Le seeder Sfax (`DataSeeder`) peuple automatiquement les 4 variétés tunisiennes (`Chemlali`, `Chétoui`, `Zalmati`, `Oueslati`), 5 utilisateurs de démonstration (1 admin, 1 collecte, 3 agriculteurs) et 5 vergers représentatifs de la région de Sfax avec leurs parcelles.
- Le mot de passe par défaut des comptes seedés est configuré via la variable d'environnement `SEED_DEFAULT_PASSWORD` (par défaut `Password123!` en local).

---

## 3. Lancer le Frontend (React / Vite)

Depuis la racine du projet :

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

L'interface est accessible sur [http://127.0.0.1:5173](http://127.0.0.1:5173).

---

## 4. Vérification et Tests

### Backend (52 tests automatisés)
Lancer la suite complète de 52 tests unitaires, d'intégration et de sécurité :

```powershell
cd backend
.\mvnw.cmd test
```

Vérifier le packaging de l'application :
```powershell
cd backend
.\mvnw.cmd clean verify
```

### Frontend
Depuis le dossier `frontend/` :
```powershell
npm.cmd run build
npm.cmd run typecheck
npm.cmd run lint
npm.cmd test
```

---

## 5. Visualisation dans MongoDB Compass

Pour inspecter les collections et vérifier les index créés :
1. Ouvrir **MongoDB Compass**.
2. Chaîne de connexion standard (sans secret dans le dépôt) :
   ```
   mongodb://localhost:27017/oliveconnect
   ```
3. Vérifier les collections créées : `users`, `varietes`, `vergers`, `parcelles`.
4. Dans l'onglet **Indexes** de la collection `vergers`, vous observerez :
   - `_id_`
   - `idx_vergers_code_unique` (unique sur `code`)
   - `idx_vergers_localisation_2dsphere` (type `2dsphere` sur `localisation`)
   - `idx_vergers_owner_actif` (composé sur `{ proprietaireId: 1, actif: 1 }`)

---

## 6. Documentation du Projet

- [Schéma et modèle MongoDB](docs/MONGODB_SCHEMA.md)
- [Contrat d'API validé](docs/API_CONTRACT.md)
- [Rapport de passation Jour 3 vers Jour 4](docs/HANDOFF.md)
- [Fixtures JSON exportées](database/seed/)

# feat(j03): modélisation MongoDB, index 2dsphere, seeder Sfax et API vergers/parcelles

Base : `main` · Branche : `feat/j03-mongodb-vergers`

## Résumé

Cette Pull Request implémente la persistance MongoDB, les indexations explicites, le seeder de données pour le bassin de Sfax et l'API de gestion des vergers, parcelles et variétés (Jour 3 — Ben Slima). Elle intègre l'ensemble des règles métier spatiales, agronomiques et de contrôle d'accès associées aux exploitations oléicoles, validées par 52 tests automatisés.

## Ajouts et modifications

- **Infrastructure & Configuration locale** :
  - `docker-compose.yml` : service minimal `mongo:7` exposé sur `127.0.0.1:27017` avec volume nommé.
  - `.env.example` : gabarits de configuration sans secrets pour MongoDB et le mot de passe du seeder.
  - Configuration multi-bases : `oliveconnect` pour le profil `dev`, `oliveconnect_test` pour les tests.
- **Documents et Modélisation MongoDB** :
  - `Variete` : référentiel des variétés d'olives (`CHEMLALI`, `CHETOUI`, `ZALMATI`, `OUESLATI`), code unique immuable en majuscules, seuils de maturité et rendements.
  - `Verger` : exploitation oléicole avec localisation GeoJSON `Point [longitude, latitude]`, précision GPS optionnelle, source (`AUTO` / `MANUEL`), audit UTC et version.
  - `Parcelle` : collection distincte référençant `vergerId` (évite l'anti-pattern des tableaux non bornés CON-04, conforme aux contraintes de récolte B.1) et `varieteId` sans dénormalisation (INT-05).
- **Indexation explicite et idempotente (`MongoIndexConfig`)** :
  - `users` : index unique sur `email` normalisé (CPT-01).
  - `vergers` : index spatial `2dsphere` sur `localisation` (VER-05), index unique sur `code`, index composé `{ "proprietaireId": 1, "actif": 1 }`.
  - `parcelles` : index composé `{ "vergerId": 1, "actif": 1 }`, index sur `{ "varieteId": 1 }`.
  - `varietes` : index unique sur `code`.
- **DataSeeder Sfax idempotent (`DataSeeder`)** :
  - Peuplage automatique sous profil `dev` (upsert par clé naturelle) : 4 variétés tunisiennes, 5 utilisateurs (1 admin, 1 collecte, 3 agriculteurs aux patronymes tunisiens/apostrophes CPT-06), 5 vergers représentatifs de Sfax (El Hencha, Mahrès, El Amra, Sakiet Ezzit) et 7 parcelles.
  - Couverture des cas limites : verger multi-variétés (VER-09), position manuelle sans précision (VER-04), verger inactif (INT-03), coordonnées identiques (VER-11), surfaces décimales.
  - Export des fixtures JSON dans [database/seed/](database/seed/).
- **API REST & Règles de gestion (`VergerController`, `VergerService`, `VarieteController`)** :
  - `GET /api/varietes` : consultation des variétés actives.
  - `GET /api/vergers` (paginé) : filtrage par propriétaire pour l'agriculteur connecté, visibilité globale pour le staff.
  - `GET /api/vergers/{id}` : détail protégé par IDOR (404 si non possédé pour empêcher l'énumération AUT-01).
  - `POST /api/vergers` : création avec propriétaire extrait de la session (AUT-02).
  - `PUT /api/vergers/{id}` : mise à jour avec code immuable (INT-07) et verrouillage optimiste.
  - `DELETE /api/vergers/{id}` : suppression logique en cascade (`actif = false`, INT-03).
  - `GET/POST/PUT/DELETE /api/vergers/{id}/parcelles` : gestion fine des parcelles avec contrôle du plafond de surface du verger (VER-07) et validation de variété active (VER-15).
  - Validation des bornes géographiques dans le gouvernorat de Sfax : lat [34.30, 35.40], lng [9.80, 11.20] (VER-02) avec détection et message d'aide en cas d'inversion lat/lng (VER-03).
  - Point d'extension [VergerUsageChecker](backend/src/main/java/com/oliveconnect/service/VergerUsageChecker.java) pour le blocage de suppression lors des futures récoltes (J05/J06).
- **Documentation technique** :
  - [docs/MONGODB_SCHEMA.md](MONGODB_SCHEMA.md) : documentation du schéma, justifications référence vs embarquement, index et requêtes associées.
  - [docs/API_CONTRACT.md](API_CONTRACT.md) : mise à jour avec les nouveaux endpoints, exemples de payloads et codes d'erreur.
  - [HANDOFF.md](../HANDOFF.md) : rapport complet de passation Jour 3 &rarr; Jour 4.
  - [README.md](../README.md) : mise à jour du guide de démarrage et d'inspection dans MongoDB Compass.

## Validation des tests

- **Exécution Maven** : `52 tests exécutés, 0 échec, 0 erreur, 0 ignoré`.
- **Cas limites explicitement validés** :
  - `IDX_indexesExist` : présence et typage des index uniques, géospatiaux et composés.
  - `VER05_nearQuery_returnsExpectedVerger` : requête géospatiale `$near` 2dsphere réelle dans le périmètre de Sfax.
  - `CPT01_duplicateEmailDifferentCase_rejected` : rejet des doublons d'email quelle que soit la casse.
  - `VER01_latitudeOutOfRange_returns400` : rejet des latitudes hors limites.
  - `VER02_outsideSfax_returns400` : rejet des coordonnées situées hors du gouvernorat de Sfax.
  - `VER03_swappedCoordinates_returns400withHint` : détection d'inversion lat/lng avec suggestion explicite.
  - `VER06_negativeSurface_returns400` : rejet des surfaces négatives ou nulles.
  - `VER07_parcellesExceedSurface_returns409` : conflit si les surfaces cumulées des parcelles dépassent la superficie du verger.
  - `AUT01_otherFarmerVerger_isDenied` : blocage IDOR (404) pour les vergers d'autres agriculteurs.
  - `AUT02_bodyOwnerIdIgnored` : extraction garantie du propriétaire depuis le principal connecté.
  - `INT03_softDeletedVergerHiddenFromList` : exclusion des vergers supprimés logiquement des flux de consultation.
  - `SEED_runTwice_sameCounts` : validation de l'idempotence stricte du seeder.
  - Tests nominaux MockMvc pour l'ensemble des endpoints d'API.

## Reprise et passation (Jour 4 — Yassine Achour)

- Les comptes de test seedés sont prêts (`admin@oliveconnect.tn`, `collecte@oliveconnect.tn`, `ali.benmbarek@sfax.tn`, `fatma.triki@sfax.tn`, `habib.kammoun@sfax.tn`).
- Les vergers et parcelles associés sont disponibles pour modéliser les demandes de récolte au Jour 4.
- Le point d'extension `VergerUsageChecker` attend son implémentation métier au Jour 5 pour brancher le contrôle d'utilisation sur les récoltes actives.

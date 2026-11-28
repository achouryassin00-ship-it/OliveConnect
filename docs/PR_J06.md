# feat(j06): ressources, tournées de collecte, contrôle des chevauchements et index MongoDB

Base : `main` (`4c80303`) · Branche : `feat/j06-ressources-tournees`

## Résumé

Ce lot Jour 6 (Ben Slima) implémente la gestion des ressources logistiques (`ouvriers`, `tracteurs`, `bennes`), la création et la planification des tournées de collecte (`/api/ressources`, `/api/tournees`), les contrôles stricts de chevauchement horaire et de capacité, l'indexation MongoDB explicite (vérifiée en `IXSCAN`) et la suppression logique protégée. L'ensemble est validé par **99 tests backend** (77 existants + 22 nouveaux tests J06) et 5 preuves HTTP live.

## Ajouts et modifications

- **Modèles & Énumérations** :
  - `TypeRessource` (`OUVRIER`, `TRACTEUR`, `BENNE`), `StatutRessource` (`DISPONIBLE`, `MAINTENANCE`, `HORS_SERVICE`, `INDISPONIBLE`), entité `Ressource` (`code` unique immuable, `capaciteKg`, `actif`, `@Version`, audit UTC).
  - `StatutTournee` (`BROUILLON`, `PLANIFIEE`, `EN_COURS`, `TERMINEE`, `ANNULEE`, `INTERROMPUE`) avec machine à états `peutPasserA(StatutTournee)` et libellés d'affichage séparés ; entité `Tournee` (`code`, `debut`, `fin`, `recolteIds`, `chefEquipeId`, `ouvrierIds`, `tracteurIds`, `benneIds`, `quantiteEstimeeKg`, `actif`, `@Version`, audit UTC).
- **Index MongoDB (`MongoIndexConfig`) & Repositories** :
  - `RessourceRepository`, `TourneeRepository`.
  - `ressources` : `idx_ressources_code_unique`, `idx_ressources_type_actif_statut`.
  - `tournees` : `idx_tournees_code_unique`, `idx_tournees_tracteur_debut_fin`, `idx_tournees_benne_debut_fin`, `idx_tournees_ouvrier_debut_fin`, `idx_tournees_statut_debut`, `idx_tournees_recolte`.
- **Services & Règles métier (`RessourceService`, `TourneeService`, `GlobalExceptionHandler`)** :
  - `RessourceService` : CRUD avec code unique/immuable, `capaciteKg > 0` obligatoire pour `BENNE` (`400`), verrou optimiste `@Version` (`409`), blocage de suppression logique (`INT-01`) et de passage `HORS_SERVICE`/`MAINTENANCE` (`TOU-10`) si affectée à une tournée `PLANIFIEE` ou `EN_COURS` (`409` avec la liste des codes de tournées), calendrier par fenêtre UTC (`TOU-23`).
  - `TourneeService` : création `BROUILLON`, contrôle `fin > debut` et durée `≤ 12 h` (`TOU-04`), détection des chevauchements sur tracteurs, bennes et ouvriers avec marge configurable (`TOU-01`/`TOU-02`, créneaux adjacents autorisés), unicité d'une récolte par tournée active et statut `VALIDEE` requis (`TOU-14`), affectations (`PUT /affectations`) gelées dès `EN_COURS`, planification (`POST /planifier`) vérifiant `≥ 1` récolte/ouvrier/tracteur/benne (`TOU-06`) et capacité cumulée des bennes `≥ quantiteEstimeeKg` (`TOU-08`), annulation (`POST /annuler`) bloquée dès `EN_COURS`/`TERMINEE`.
  - `GlobalExceptionHandler` : normalisation `STA-05` sur désérialisation d'enum inconnu (`400 VALIDATION_ERROR` avec valeurs acceptées).
- **Retouche minimale hors J06 (`RecolteService`)** :
  - Ajout de `RecolteService.planifierDepuisTournee(String recolteId, String tourneeCode)` et `isPlanningStaff(...)` pour assurer la transition `VALIDEE → PLANIFIEE` lors de la planification d'une tournée sans dupliquer la logique d'audit et d'historique J05.
- **API REST (`RessourceController`, `TourneeController`)** :
  - `GET/POST /api/ressources`, `GET/PUT/DELETE /api/ressources/{id}`, `GET /api/ressources/{id}/calendrier`.
  - `GET/POST /api/tournees`, `GET /api/tournees/{id}`, `PUT /api/tournees/{id}/affectations`, `POST /api/tournees/{id}/planifier`, `POST /api/tournees/{id}/annuler`.
  - Contrôle des rôles côté serveur (`RESPONSABLE_LOGISTIQUE`, `RESPONSABLE_COLLECTE`, `ADMINISTRATEUR`, et lecture restreinte de ses propres tournées pour `CHEF_EQUIPE`).
- **Seeder Sfax & Fixtures (`DataSeeder`, `database/seed/`)** :
  - Ajout des comptes `logistique@oliveconnect.tn` (`RESPONSABLE_LOGISTIQUE`) et `chef.equipe@oliveconnect.tn` (`CHEF_EQUIPE`).
  - Peuplage idempotent de 20 ressources (10 ouvriers, 4 tracteurs dont `TR-04` New Holland et 1 `HORS_SERVICE`, 6 bennes de 2000 kg) et 2 tournées (`T-001` `PLANIFIEE`, `T-002` `BROUILLON`), exportés dans [database/seed/ressources.json](../database/seed/ressources.json) et [database/seed/tournees.json](../database/seed/tournees.json).
- **Documentation** :
  - Mise à jour de [docs/API_CONTRACT.md](API_CONTRACT.md), [docs/MONGODB_SCHEMA.md](MONGODB_SCHEMA.md), [HANDOFF.md](../HANDOFF.md), [README.md](../README.md) et ajout de [docs/J06_API_PROOFS.http](J06_API_PROOFS.http).

## Validation des tests

- **Suite Maven complète (`cd backend && .\mvnw.cmd -B -ntp test`)** : **99 tests exécutés, 0 échec, 0 erreur, 0 ignoré** :
  - 77 tests existants (J02/J03/J05) + 22 nouveaux tests J06 :
    - `RessourceServiceTest` (4 tests) : création benne sans capacité, blocage suppression si tournée active (`INT-01`), blocage passage `HORS_SERVICE`, calendrier vide (`TOU-23`).
    - `TourneeServiceTest` (10 tests) : création nominale, `fin <= debut`, durée > 12h, chevauchement tracteur (`409`), créneau adjacent autorisé, récolte déjà prise (`TOU-14`), planification sans ressource (`TOU-06`), capacité bennes insuffisante (`TOU-08`), planification nominale avec transition récolte `VALIDEE → PLANIFIEE`, annulation bloquée si `EN_COURS`.
    - `RessourceControllerTest` (3 tests) & `TourneeControllerTest` (4 tests) : sécurité RBAC (`401`/`403`), enum inconnu `400`, cloisonnement `CHEF_EQUIPE` (`403` sur tournée d'un autre chef).
    - `TourneeRessourceIntegrationTest` (2 tests sur MongoDB réel) : présence des index, vérification `explain()` en `IXSCAN` sur `idx_tournees_tracteur_debut_fin`, persistance nominale et blocage `INT-01`.
- **Preuves HTTP live (base MongoDB isolée)** :
  - `POST /api/tournees` valide &rarr; `201 Created`
  - `POST /api/tournees` avec tracteur `TR-02` en chevauchement &rarr; `409 Conflict`
  - `POST /api/tournees` avec `fin <= debut` &rarr; `400 Bad Request` (`fieldErrors.fin`)
  - `POST /api/tournees/{id}/planifier` avec capacité insuffisante &rarr; `409 Conflict`
  - `DELETE /api/ressources/{id}` sur ressource affectée à `T-001` &rarr; `409 Conflict`

## Limites connues & Passation (Jour 7 — Yassine Achour)

- **Atomicité inter-documents** : sur MongoDB standalone sans replica set, la vérification de chevauchement suivie de l'écriture repose sur `@Version` (verrou optimiste) et non sur une transaction multi-documents.
- **Hors périmètre J06 (réservé J07/J08+)** : pas d'écran frontend branché dans ce lot, pas de démarrage/clôture terrain (`EN_COURS` / `TERMINEE`), pas de pesée pressoir ni d'optimisation automatique de trajet.
- **Reprise J07** : les endpoints `/api/ressources` et `/api/tournees` ainsi que les comptes `logistique@oliveconnect.tn` et `chef.equipe@oliveconnect.tn` sont prêts pour brancher le calendrier des ressources, l'écran tournées et l'interface chef d'équipe.

## Checklist

- [x] Branche dédiée `feat/j06-ressources-tournees` (aucun commit sur `main`)
- [x] Couches Controller / Service / Repository / Entity respectées
- [x] Aucun secret en dur dans le code ou les tests
- [x] 99/99 tests backend verts après la dernière modification de code
- [x] Documentation (`API_CONTRACT.md`, `MONGODB_SCHEMA.md`, `HANDOFF.md`, `README.md`, `PR_J06.md`) à jour

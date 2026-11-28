# Passation — Jour 6 vers Jour 7

- **Auteur sortant (Jour 6)** : Ben Slima (Ressources, Tournées, Index MongoDB & Seed Sfax).
- **Prochain responsable (Jour 7)** : Yassine Achour (Frontend calendrier des ressources, tournées & vue mobile chef d'équipe).
- **Branche** : `feat/j06-ressources-tournees`, créée depuis `main` (`4c80303`, incluant PR #5 J05).
- **Livraison** : commits atomiques locaux et résumé PR dans [docs/PR_J06.md](docs/PR_J06.md) (PR à ouvrir vers `main`, sans fusion automatique).
- **Archives** : [docs/HANDOFF_J03.md](docs/HANDOFF_J03.md), [docs/HANDOFF_J04.md](docs/HANDOFF_J04.md), [docs/PR_J05.md](docs/PR_J05.md).

---

## 1. Modèles, Repositories & Index MongoDB (J06)

- **Collection `ressources`** (`Ressource`, `TypeRessource`, `StatutRessource`, `RessourceRepository`) :
  - Types : `OUVRIER`, `TRACTEUR`, `BENNE`.
  - Statuts : `DISPONIBLE`, `MAINTENANCE`, `HORS_SERVICE`, `INDISPONIBLE`.
  - `capaciteKg` obligatoire et `> 0` pour `BENNE`.
- **Collection `tournees`** (`Tournee`, `StatutTournee`, `TourneeRepository`) :
  - Machine à états `StatutTournee.peutPasserA(...)` : `BROUILLON → PLANIFIEE → EN_COURS → TERMINEE`, annulation avant démarrage (`BROUILLON` / `PLANIFIEE` → `ANNULEE`), `EN_COURS → INTERROMPUE`.
  - J06 expose `BROUILLON`, `PLANIFIEE` et `ANNULEE` (`EN_COURS` / `TERMINEE` réservés à la suite terrain).
- **Index MongoDB (`MongoIndexConfig`)** :
  - `ressources` : `idx_ressources_code_unique`, `idx_ressources_type_actif_statut`.
  - `tournees` : `idx_tournees_code_unique`, `idx_tournees_tracteur_debut_fin`, `idx_tournees_benne_debut_fin`, `idx_tournees_ouvrier_debut_fin`, `idx_tournees_statut_debut`, `idx_tournees_recolte`.
  - Exécution en `IXSCAN` vérifiée via `explain()` dans `TourneeRessourceIntegrationTest`.

---

## 2. API Ressources & Tournées (Contrats pour le Frontend J07)

Toutes les routes sont documentées en détail dans [docs/API_CONTRACT.md](docs/API_CONTRACT.md) (§3.6 et §3.7).

### Ressources (`/api/ressources`)
- `GET /api/ressources?type=&statut=&page=0&size=20` : liste paginée des ressources actives (`RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `CHEF_EQUIPE`, `ADMINISTRATEUR`).
- `POST /api/ressources` (`RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`) : création (`201 Created`).
- `GET /api/ressources/{id}` : détail d'une ressource active (`404` sinon).
- `PUT /api/ressources/{id}` (`RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`) : mise à jour avec `version` (`409` si passage `HORS_SERVICE` / `MAINTENANCE` alors qu'elle est affectée à une tournée `PLANIFIEE` ou `EN_COURS` — `TOU-10`).
- `DELETE /api/ressources/{id}` (`RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`) : suppression logique `actif = false` (`409` avec la liste des tournées si affectée — `INT-01`).
- `GET /api/ressources/{id}/calendrier?debut=&fin=` : créneaux actifs (`BROUILLON`, `PLANIFIEE`, `EN_COURS`) pour l'affichage calendrier (`creneaux: []` si libre).

### Tournées (`/api/tournees`)
- `POST /api/tournees` (`RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`) : création en `BROUILLON` (`201 Created`), contrôle `fin > debut` et durée `≤ 12 h` (`400`), détection des chevauchements sur tracteurs/bennes/ouvriers (`409`), récoltes `VALIDEE` non déjà affectées (`409`).
- `GET /api/tournees?debut=&fin=&page=0&size=20` : liste paginée (vue globale pour staff ; filtrée sur `chefEquipeId == currentUser.id` pour `CHEF_EQUIPE`).
- `GET /api/tournees/{id}` : détail (`403 ACCESS_DENIED` si `CHEF_EQUIPE` consulte une tournée qui ne lui est pas assignée).
- `PUT /api/tournees/{id}/affectations` : modification des créneaux, récoltes, chef d'équipe et ressources (gelée dès `EN_COURS` → `409`).
- `POST /api/tournees/{id}/planifier` : passage `BROUILLON → PLANIFIEE` ; vérifie `≥ 1` récolte/ouvrier/tracteur/benne (`TOU-06` → `409`), capacité cumulée des bennes `≥ quantiteEstimeeKg` (`TOU-08` → `409`) et passe automatiquement les récoltes de `VALIDEE` à `PLANIFIEE` via `RecolteService.planifierDepuisTournee`.
- `POST /api/tournees/{id}/annuler` : annulation `BROUILLON` / `PLANIFIEE` → `ANNULEE` (`409` dès `EN_COURS` ou `TERMINEE`).

---

## 3. Comptes & Données de Démonstration (Sfax)

Seedés automatiquement au démarrage en profil `dev` (`SEED_DEFAULT_PASSWORD=Password123!` par défaut) :
- **Nouveaux comptes J06** :
  - `logistique@oliveconnect.tn` (`RESPONSABLE_LOGISTIQUE`)
  - `chef.equipe@oliveconnect.tn` (`CHEF_EQUIPE`)
- **Comptes existants** : `admin@oliveconnect.tn`, `collecte@oliveconnect.tn`, `ali.benmbarek@sfax.tn`, `fatma.triki@sfax.tn`, `habib.kammoun@sfax.tn`.
- **Ressources** : 10 ouvriers (`OUV-001`..`010`), 4 tracteurs (`TR-01`..`TR-04` dont `TR-04` New Holland et `TR-03` `HORS_SERVICE`), 6 bennes de 2000 kg (`BN-01`..`BN-06`).
- **Tournées** : `T-001` (`PLANIFIEE`) et `T-002` (`BROUILLON`).

---

## 4. Tests & Preuves Vérifiés

- **Backend Maven (`cd backend && .\mvnw.cmd -B -ntp test`)** : **99/99 tests réussis** (0 échec, 0 erreur, 0 ignoré) — inclut les 77 tests J02/J03/J05 et 22 nouveaux tests J06 (`RessourceServiceTest`, `TourneeServiceTest`, `RessourceControllerTest`, `TourneeControllerTest`, `TourneeRessourceIntegrationTest`).
- **Preuves HTTP live sur base MongoDB isolée** :
  - `201 Created` sur création de tournée valide
  - `409 Conflict` sur chevauchement de tracteur (`TR-02`)
  - `400 Bad Request` sur dates invalides (`fin <= debut`)
  - `409 Conflict` sur capacité de bennes insuffisante à la planification
  - `409 Conflict` sur suppression logique d'une ressource affectée à `T-001`

---

## 5. Risques, Limites & Instructions de Reprise (J07)

1. **Concurrence multi-documents** : MongoDB tourne en standalone sans replica set ; l'atomicité inter-documents n'est pas transactionnelle (mitigée par `@Version` sur `Tournee` et `Ressource`).
2. **Démarrage/Clôture terrain** : `EN_COURS` et `TERMINEE` sont déclarés dans `StatutTournee` mais leurs endpoints terrain ne font pas partie de J06 (prévoir si nécessaire au J07/J08).
3. **Démarrer le backend pour J07** :
   - `cd backend && .\mvnw.cmd spring-boot:run` (port `8080`, MongoDB sur `127.0.0.1:27017`).
   - Les formats de requêtes/réponses et codes d'erreur (`400`, `401`, `403`, `404`, `409`) sont alignés avec `GlobalExceptionHandler` et [docs/API_CONTRACT.md](docs/API_CONTRACT.md).

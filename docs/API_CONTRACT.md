# Contrat d'API OliveConnect (Validé J02, J03 & J05)

Ce document constitue le contrat formel des endpoints implémentés et testés dans l'API OliveConnect.

---

## 1. Conventions Générales

- **Préfixe global** : `/api`
- **Format de transfert** : JSON UTF-8
- **Identifiants** : Chaînes de caractères opaques (`String` correspondant aux ObjectId MongoDB).
- **Horodatages** : ISO 8601 UTC (`2026-11-23T08:00:00Z`).
- **Surfaces & Poids** : Surfaces en hectares (`ha`), poids en kilogrammes (`kg`).
- **Coordonnées géographiques** : GeoJSON `Point` : `[longitude, latitude]`. Longitude en premier, puis latitude.
- **Bounding Box Sfax (VER-02 / VER-03)** :
  - Latitude : `[34.30, 35.40]`
  - Longitude : `[9.80, 11.20]`
- **Pagination standard** : `?page=0&size=20` (taille bornée entre 1 et 100).
  ```json
  {
    "items": [],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  }
  ```
- **Codes HTTP** :
  - `200 OK` : Lecture ou mise à jour réussie.
  - `201 Created` : Création réussie (avec en-tête `Location: /api/.../{id}`).
  - `204 No Content` : Opération sans corps de retour (déconnexion, suppression logique).
  - `400 Bad Request` : Erreur de validation structurelle ou métier (`VALIDATION_ERROR`).
  - `401 Unauthorized` : Absence ou expiration du jeton JWT (`UNAUTHORIZED`).
  - `403 Forbidden` : Privilèges insuffisants pour l'action demandée (`ACCESS_DENIED`, `ACCOUNT_DISABLED`).
  - `404 Not Found` : Ressource inexistante ou masquage anti-énumération IDOR (`RESOURCE_NOT_FOUND`).
  - `409 Conflict` : Conflit de ressource, duplication de clé ou verrouillage optimiste (`RESOURCE_CONFLICT`, `EMAIL_ALREADY_EXISTS`).

---

## 2. Format Standardisé des Erreurs

Toutes les erreurs retournent la structure standard suivante :

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Vérifiez les informations saisies.",
  "timestamp": "2026-11-23T08:30:00Z",
  "fieldErrors": {
    "latitude": "La latitude doit être comprise entre -90 et 90."
  }
}
```

---

## 3. Endpoints Disponibles

### 3.1. Santé & Authentification (Jour 2 — Ben Ayed)

| Méthode | Route | Accès | Description |
|---|---|---|---|
| `GET` | `/health`, `/api/health` | Public | Vérification de l'état du backend (`{"status":"UP"}`) |
| `POST` | `/api/auth/register` | Public | Inscription (mot de passe haché BCrypt, email normalisé, rôle forcé à `AGRICULTEUR` — 403 si rôle privilégié demandé) |
| `POST` | `/api/auth/login` | Public | Authentification, contrôle compte actif, émission JWT |
| `POST` | `/api/auth/logout` | Authentifié | Déconnexion (204 No Content) |
| `GET` | `/api/auth/me` | Authentifié | Profil utilisateur connecté |
| `GET` | `/api/users/{id}` | Propriétaire / Admin | Consultation sécurisée d'un profil |

---

### 3.2. Référentiel des Variétés (Jour 3 — Ben Slima)

#### `GET /api/varietes`
Retourne la liste des variétés d'olives actives de la coopérative.
- **Accès** : Tout utilisateur authentifié.
- **Réponse nominale (200 OK)** :
```json
[
  {
    "id": "67421ab89c10ef01a8901234",
    "code": "CHEMLALI",
    "nom": "Chemlali de Sfax",
    "seuilMaturite": 0.65,
    "rendementMoyenTHa": 2.2,
    "actif": true
  },
  {
    "id": "67421ab89c10ef01a8901235",
    "code": "CHETOUI",
    "nom": "Chétoui",
    "seuilMaturite": 0.70,
    "rendementMoyenTHa": 1.8,
    "actif": true
  }
]
```

---

### 3.3. Gestion des Vergers (Jour 3 — Ben Slima)

#### `GET /api/vergers`
Liste paginée des vergers.
- **Accès** :
  - Rôle `AGRICULTEUR` : Accède exclusivement à ses propres vergers actifs (`proprietaireId = currentUser.id`).
  - Rôles `RESPONSABLE_COLLECTE` ou `ADMINISTRATEUR` : Voient l'ensemble des vergers actifs de la coopérative.
- **Paramètres** : `page` (défaut 0), `size` (défaut 20, max 100).
- **Réponse nominale (200 OK)** :
```json
{
  "items": [
    {
      "id": "67422bc99d21ef02b9015678",
      "code": "VRG-SFX-001",
      "nom": "Verger El Hencha Nord",
      "proprietaireId": "67420aa11b01ef00a7800001",
      "commune": "El Hencha",
      "longitude": 10.74,
      "latitude": 35.13,
      "precisionGpsMetres": 4.5,
      "sourcePosition": "AUTO",
      "surfaceHa": 15.5,
      "nombreArbres": 250,
      "anneePlantation": 2005,
      "actif": true,
      "version": 0,
      "createdAt": "2026-11-23T08:15:00Z",
      "updatedAt": "2026-11-23T08:15:00Z",
      "surfaceParcellesTotale": 15.5
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

#### `GET /api/vergers/{id}`
Détail d'un verger.
- **Accès** : Propriétaire du verger, Responsable Collecte ou Administrateur.
- **Règle IDOR (AUT-01)** : Si l'agriculteur tente d'accéder au verger d'un tiers, l'API répond `404 RESOURCE_NOT_FOUND` afin de ne pas révéler l'existence de l'identifiant.
- **Réponse nominale (200 OK)** : Corps identique à l'élément de la liste ci-dessus.

---

#### `POST /api/vergers`
Création d'un verger.
- **Accès** : Utilisateur authentifié possédant obligatoirement le rôle `AGRICULTEUR` (`@PreAuthorize("hasRole('AGRICULTEUR')")`, `403 ACCESS_DENIED` sinon).
- **Règles métier** :
  - `proprietaireId` (AUT-02) : Dérivé obligatoirement de la session de l'utilisateur authentifié (toute valeur transmise dans le corps est ignorée).
  - Unicité du `code` : 409 si déjà existant.
  - Bornes GPS (VER-01) : latitude [-90, 90], longitude [-180, 180] &rarr; 400.
  - Bounding Box Sfax (VER-02) : lat [34.30, 35.40], lng [9.80, 11.20] &rarr; 400 si hors zone.
  - Inversion de coordonnées (VER-03) : Si l'inversion `[lat, lng]` tomberait dans Sfax &rarr; 400 avec suggestion d'inversion.
  - Position manuelle (VER-04) : Source `MANUEL` et précision GPS `null` autorisées.
  - Surface (VER-06) : Strictement positive et `≤ 1000.0` ha.
- **Corps de requête** :
```json
{
  "code": "VRG-SFX-006",
  "nom": "Verger Sidi Mansour",
  "commune": "Sidi Mansour",
  "longitude": 10.78,
  "latitude": 34.82,
  "precisionGpsMetres": 3.8,
  "sourcePosition": "AUTO",
  "surfaceHa": 14.0,
  "nombreArbres": 200,
  "anneePlantation": 2015
}
```
- **Réponse nominale (201 Created)** : En-tête `Location: /api/vergers/67422bc...` et objet créé.

---

#### `PUT /api/vergers/{id}`
Modification d'un verger existant.
- **Accès** : Propriétaire ou Staff.
- **Règles métier** :
  - `code` immuable (INT-07) : Non modifiable.
  - Concurrence optimiste : Si `version` est fournie et ne correspond pas &rarr; 409.
  - Cohérence des surfaces (VER-07) : La nouvelle `surfaceHa` ne peut pas être inférieure à la somme des parcelles actives rattachées &rarr; 409.
- **Réponse nominale (200 OK)**.

---

#### `DELETE /api/vergers/{id}`
Suppression logique d'un verger (`actif = false`).
- **Accès** : Propriétaire ou Staff.
- **Règles métier** :
  - Point d'extension récoltes/tournées : Vérifie `VergerUsageChecker.isInUse(id)` via `RecolteVergerUsageChecker` (demandes actives `DEMANDEE`, `VALIDEE`, `PLANIFIEE`, `EN_COURS`) &rarr; 409 si en cours d'utilisation.
  - Supprime en cascade logique (`actif = false`) toutes les parcelles rattachées.
- **Réponse nominale (204 No Content)**.

---

### 3.4. Gestion des Parcelles (Jour 3 — Ben Slima)

#### `GET /api/vergers/{id}/parcelles`
Liste des parcelles actives associées à un verger.
- **Accès** : Propriétaire du verger ou Staff.
- **Réponse nominale (200 OK)** :
```json
[
  {
    "id": "67423cd11e32ef03c0128901",
    "vergerId": "67422bc99d21ef02b9015678",
    "nom": "Parcelle Nord A",
    "varieteId": "67421ab89c10ef01a8901234",
    "surfaceHa": 8.0,
    "actif": true,
    "version": 0,
    "createdAt": "2026-11-23T08:20:00Z",
    "updatedAt": "2026-11-23T08:20:00Z"
  }
]
```

---

#### `POST /api/vergers/{id}/parcelles`
Création d'une parcelle rattachée au verger.
- **Accès** : Propriétaire du verger ou Staff.
- **Règles métier** :
  - Validité de la variété (VER-15) : `varieteId` doit référencer une variété existante et active &rarr; 400.
  - Plafond de surface (VER-07) : La somme des parcelles actives + nouvelle parcelle ne doit pas dépasser la surface totale du verger &rarr; 409 `RESOURCE_CONFLICT`.
- **Corps de requête** :
```json
{
  "nom": "Parcelle Nord B",
  "varieteId": "67421ab89c10ef01a8901235",
  "surfaceHa": 6.0
}
```
- **Réponse nominale (201 Created)** : En-tête `Location: /api/vergers/{id}/parcelles/{parcelleId}`.

---

#### `PUT /api/vergers/{id}/parcelles/{parcelleId}`
Mise à jour d'une parcelle.
- **Accès** : Propriétaire du verger ou Staff.
- **Règles métier** :
  - Contrôle d'appartenance verger / parcelle.
  - Validité de la variété (VER-15).
  - Contrôle du cumul de surface (VER-07) &rarr; 409 si dépassement du verger.
  - Optimistic locking via `version`.
- **Réponse nominale (200 OK)**.

---

#### `DELETE /api/vergers/{id}/parcelles/{parcelleId}`
Suppression logique d'une parcelle (`actif = false`).
- **Accès** : Propriétaire du verger ou Staff.
- **Réponse nominale (204 No Content)**.

---

### 3.5. Demandes de collecte / Récoltes (Jour 5 — Ben Ayed)

#### Règles métier & Idempotence (rapprochement J01/J03/J04)
- **Statuts (`StatutRecolte`)** : `DEMANDEE`, `VALIDEE`, `PLANIFIEE`, `EN_COURS`, `LIVREE`, `PESEE`, `ARCHIVEE`, `REFUSEE`, `ANNULEE`, `INTERROMPUE`.
- **Propriétaire (`proprietaireId`)** : Toujours dérivé du principal authentifié (`SecurityService.getCurrentUserId()`), jamais du corps JSON.
- **Contrôle du verger** : Le verger doit exister, appartenir à l'agriculteur authentifié (sinon `404 RESOURCE_NOT_FOUND` anti-IDOR) et être actif (`actif = true`, sinon `400 VALIDATION_ERROR`).
- **Date souhaitée (`dateSouhaitee`)** : Format `YYYY-MM-DD` (date locale), obligatoire, `>=` aujourd'hui dans le fuseau `Africa/Tunis`.
- **Quantité estimée (`quantiteEstimeeKg`)** : Facultative (`null` autorisé ; si renseignée : strictement positive, finie, `≤ 500 000 kg`, 3 décimales maximum).
- **Urgence (`urgent`, `motifUrgence`)** : `motifUrgence` obligatoire et non vide si `urgent = true` (max 500 caractères).
- **Anti-doublon actif** : Un même verger ne peut pas avoir deux demandes actives (`DEMANDEE`, `VALIDEE`, `PLANIFIEE`, `EN_COURS`) pour la même `dateSouhaitee` (index partiel unique MongoDB `idx_recoltes_verger_date_active_unique` &rarr; `409 RESOURCE_CONFLICT`). Une nouvelle demande redevient possible après `ANNULEE` ou `REFUSEE`.
- **Idempotence (`Idempotency-Key`)** :
  - En-tête optionnel sur `POST /api/recoltes` (1 à 128 caractères, scopé par `proprietaireId`).
  - Même clé + même charge utile (hash SHA-256 canonique) &rarr; `200 OK` avec en-tête `X-Idempotent-Replay: true` et l'enregistrement existant (sans doublon).
  - Même clé + charge utile différente &rarr; `409 RESOURCE_CONFLICT`.
  - Concurrence atomique garantie par l'index partiel unique `{ proprietaireId: 1, idempotencyKey: 1 }`.
- **Périmètre des alertes** : Les demandes urgentes (`urgent = true` + `motifUrgence`) sont gérées dans `/api/recoltes`. Le dossier maître PDF n'étant pas accessible dans le dépôt, aucun module autonome `/api/alertes` (`FieldAlert`) n'a été inventé au Jour 5.

#### `POST /api/recoltes`
Création d'une demande de collecte.
- **Accès** : Utilisateur authentifié avec rôle `AGRICULTEUR` uniquement (`403 ACCESS_DENIED` sinon).
- **En-tête optionnel** : `Idempotency-Key: <uuid>`
- **Corps de requête** :
```json
{
  "vergerId": "67422bc99d21ef02b9015678",
  "dateSouhaitee": "2026-11-28",
  "quantiteEstimeeKg": 2400.5,
  "urgent": true,
  "motifUrgence": "Maturité avancée sur parcelle nord"
}
```
- **Réponse nominale (`201 Created`, ou `200 OK` avec `X-Idempotent-Replay: true` en rejeu)** : En-tête `Location: /api/recoltes/{id}` et objet créé :
```json
{
  "id": "67445aa12c01ef00b8901111",
  "reference": "REC-2026-A1B2C3D4",
  "proprietaireId": "67420aa11b01ef00a7800001",
  "vergerId": "67422bc99d21ef02b9015678",
  "vergerNom": "Verger El Hencha Nord",
  "vergerCode": "VRG-SFX-001",
  "dateSouhaitee": "2026-11-28",
  "quantiteEstimeeKg": 2400.5,
  "urgent": true,
  "motifUrgence": "Maturité avancée sur parcelle nord",
  "statut": "DEMANDEE",
  "annulable": true,
  "historique": [
    {
      "ancienStatut": null,
      "nouveauStatut": "DEMANDEE",
      "acteurId": "67420aa11b01ef00a7800001",
      "acteurNom": "Ali Ben M'Barek",
      "acteurRole": "AGRICULTEUR",
      "motif": "Maturité avancée sur parcelle nord",
      "horodatage": "2026-11-25T18:30:00Z"
    }
  ],
  "version": 0,
  "createdAt": "2026-11-25T18:30:00Z",
  "updatedAt": "2026-11-25T18:30:00Z"
}
```

#### `GET /api/recoltes`
Liste paginée des demandes de collecte.
- **Accès** :
  - `AGRICULTEUR` : voit exclusivement ses propres demandes (`proprietaireId = currentUser.id`).
  - `RESPONSABLE_COLLECTE` / `ADMINISTRATEUR` : voient l'ensemble des demandes.
- **Paramètres** : `page` (défaut 0), `size` (défaut 20, max 100), `q` (recherche sur référence, ID ou nom/code du verger), `statut` (`StatutRecolte`), `urgent` (`true`/`false`), `vergerId`, `dateDebut` (`YYYY-MM-DD`), `dateFin` (`YYYY-MM-DD`).
- **Réponse nominale (`200 OK`)** : `PageResponse<RecolteResponseDTO>`.

#### `GET /api/recoltes/{id}`
Détail complet d'une demande avec historique persisté.
- **Accès** : Propriétaire, Responsable Collecte ou Administrateur (`404 RESOURCE_NOT_FOUND` si demande d'un tiers pour l'agriculteur).
- **Réponse nominale (`200 OK`)** : `RecolteResponseDTO`.

#### `POST /api/recoltes/{id}/annuler`
Annulation d'une demande de collecte.
- **Accès** : Propriétaire, Responsable Collecte ou Administrateur (`404` anti-IDOR).
- **Règle d'annulation** : Autorisée uniquement aux statuts `DEMANDEE` et `VALIDEE` (avant planification et démarrage). Refusée en `409 RESOURCE_CONFLICT` depuis `PLANIFIEE`, `EN_COURS`, `LIVREE`, `PESEE`, `ARCHIVEE`, `REFUSEE`, `ANNULEE`, `INTERROMPUE`.
- **Corps optionnel** :
```json
{
  "version": 0,
  "motif": "Report suite aux prévisions météo"
}
```
- **Réponse nominale (`200 OK`)** : `RecolteResponseDTO` mis à jour (`statut = ANNULEE`, `annulable = false`, nouvelle entrée persistée dans `historique`).

#### `POST /api/recoltes/{id}/transitions`
Transition officielle de statut avec contrôle de rôle et machine à états.
- **Transitions autorisées (Jour 5)** :
  - `DEMANDEE` &rarr; `VALIDEE` (`RESPONSABLE_COLLECTE` / `ADMINISTRATEUR` uniquement, `403` pour agriculteur)
  - `DEMANDEE` &rarr; `REFUSEE` (`RESPONSABLE_COLLECTE` / `ADMINISTRATEUR` uniquement, `motif` obligatoire &rarr; `400` sinon)
  - `DEMANDEE` / `VALIDEE` &rarr; `ANNULEE` (Propriétaire ou Staff)
  - Toute autre transition (ou depuis un état terminal/planifié/en cours) &rarr; `409 RESOURCE_CONFLICT`.
- **Corps de requête** : `{ "statutCible": "VALIDEE", "version": 0, "motif": "Créneau confirmé" }`.
- **Réponse nominale (`200 OK`)** : `RecolteResponseDTO`.

---

### 3.6. Ressources & Calendrier (Jour 6 — Ben Slima)

#### `GET /api/ressources`
Liste paginée des ressources actives (`actif = true`).
- **Accès** : `RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `CHEF_EQUIPE`, `ADMINISTRATEUR`.
- **Paramètres** : `type` (`OUVRIER | TRACTEUR | BENNE`), `statut` (`DISPONIBLE | MAINTENANCE | HORS_SERVICE | INDISPONIBLE`), `page` (défaut 0), `size` (défaut 20, max 100).
- **Réponse nominale (`200 OK`)** : `PageResponse<RessourceResponseDTO>`.

#### `POST /api/ressources`
Création d'une ressource (ouvrier, tracteur ou benne).
- **Accès** : `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR` (`403` sinon).
- **Règles** : `code` unique et immuable ; `capaciteKg` obligatoire et `> 0` si `type = BENNE` (`400`) ; statut inconnu rejeté en `400` avec valeurs acceptées (`STA-05`).
- **Réponse nominale (`201 Created`)** + `Location: /api/ressources/{id}`.

#### `GET /api/ressources/{id}`
Détail d'une ressource active (`404` si introuvable ou inactive).

#### `PUT /api/ressources/{id}`
Mise à jour d'une ressource (`version` pour verrouillage optimiste `409`).
- **Accès** : `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`.
- **Règle TOU-10** : Passage en `HORS_SERVICE` ou `MAINTENANCE` refusé (`409 RESOURCE_CONFLICT`) si la ressource est affectée à une tournée `PLANIFIEE` ou `EN_COURS`.

#### `DELETE /api/ressources/{id}`
Suppression logique (`actif = false`).
- **Accès** : `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`.
- **Règle INT-01** : Refusée (`409 RESOURCE_CONFLICT`) si affectée à une tournée `PLANIFIEE` ou `EN_COURS`, avec la liste des tournées concernées.

#### `GET /api/ressources/{id}/calendrier`
Calendrier des créneaux actifs (`BROUILLON`, `PLANIFIEE`, `EN_COURS`) d'une ressource dans une fenêtre optionnelle `?debut=<ISO-UTC>&fin=<ISO-UTC>`.
- **Réponse nominale (`200 OK`)** : `CalendrierRessourceDTO` (`creneaux` vide `[]` si aucune tournée — `TOU-23`).

---

### 3.7. Tournées de Collecte (Jour 6 — Ben Slima)

#### `POST /api/tournees`
Création d'une tournée au statut initial `BROUILLON`.
- **Accès** : `RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`.
- **Règles** :
  - `TOU-04` : `fin > debut` et durée `≤ 12 h` (`400` sinon).
  - `TOU-01 / TOU-02` : Détection des chevauchements (`debutA < finB + marge && debutB < finA + marge`, marge 0 min par défaut) sur tracteurs, bennes et ouvriers (`409` nommant ressource et tournée). Les créneaux adjacents (ex. fin 12:00 / début 12:00) sont autorisés.
  - `TOU-14` : Une récolte ne peut appartenir qu'à une seule tournée active et doit être au statut `VALIDEE` (`409`).
- **Réponse nominale (`201 Created`)** + `Location: /api/tournees/{id}`.

#### `GET /api/tournees`
Liste paginée des tournées actives (fenêtre optionnelle `?debut=<ISO-UTC>&fin=<ISO-UTC>&page=0&size=20`).
- **Accès** : `RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR` (toutes) ; `CHEF_EQUIPE` (uniquement les tournées où `chefEquipeId == currentUser.id`).

#### `GET /api/tournees/{id}`
Détail d'une tournée (`403 ACCESS_DENIED` si un `CHEF_EQUIPE` demande une tournée qui ne lui est pas assignée).

#### `PUT /api/tournees/{id}/affectations`
Mise à jour des créneaux, récoltes, chef d'équipe et ressources d'une tournée (`BROUILLON` ou `PLANIFIEE` avant démarrage ; gelée dès `EN_COURS` &rarr; `409`). Vérifie `@Version` (`409` si obsolète).

#### `POST /api/tournees/{id}/planifier`
Passe une tournée `BROUILLON` &rarr; `PLANIFIEE`.
- **Accès** : `RESPONSABLE_COLLECTE`, `RESPONSABLE_LOGISTIQUE`, `ADMINISTRATEUR`.
- **Règles** :
  - `TOU-06` : Exige au moins 1 récolte, 1 ouvrier, 1 tracteur et 1 benne (`409` listant les éléments manquants).
  - `TOU-08` : Capacité cumulée des bennes `≥ quantiteEstimeeKg` (`409` avec capacité manquante en kg).
  - Transitionne automatiquement chaque récolte associée de `VALIDEE` vers `PLANIFIEE` via `RecolteService`.

#### `POST /api/tournees/{id}/annuler`
Annule une tournée (`BROUILLON` ou `PLANIFIEE` &rarr; `ANNULEE`). Refusée (`409`) dès `EN_COURS` ou `TERMINEE`.


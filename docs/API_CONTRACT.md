# Contrat d'API OliveConnect (Validé J02 & J03)

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
| `POST` | `/api/auth/register` | Public | Inscription (mot de passe haché BCrypt, email normalisé) |
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
- **Accès** : Utilisateur authentifié (`AGRICULTEUR`).
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
  - Point d'extension récoltes/tournées : Vérifie `VergerUsageChecker.isInUse(id)` &rarr; 409 si en cours d'utilisation.
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

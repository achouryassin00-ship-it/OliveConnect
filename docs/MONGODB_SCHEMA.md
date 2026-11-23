# Spécification & Schéma MongoDB — OliveConnect (Jour 3 — Ben Slima)

Ce document formalise les collections, documents BSON, indexations et règles de modélisation MongoDB implémentées au Jour 3 pour la coopérative oléicole de Sfax.

---

## 1. Principes généraux de modélisation

- **Identifiants** : Clés primaires `_id` stockées sous forme de `String` (générées automatiquement ou mappées sur des chaînes opaques uniques).
- **Horodatages** : Tous les horodatages sont stockés en UTC au format ISO 8601 (`Instant` BSON Date). L'audit automatique Spring Data (`@CreatedDate`, `@LastModifiedDate`) est actif via `@EnableMongoAuditing`.
- **Unités standardisées** : Surfaces exprimées en hectares (`Double`), quantités en kilogrammes (`kg`), rendements en tonnes/hectare (`t/ha`).
- **Coordonnées géographiques** : Stockées au format GeoJSON standard `Point` : `[longitude, latitude]`. Permet l'indexation spatiale `2dsphere` et les requêtes de proximité `$near` et `$geoWithin`.
- **Concurrence optimiste** : Champ `@Version Long version` présent sur les documents modifiables (`Verger`, `Parcelle`, `Variete`), retournant une erreur `409 RESOURCE_CONFLICT` en cas d'accès concurrent.
- **Suppression logique (INT-03)** : Préservation de l'intégrité référentielle par le champ `actif = false`. Aucun document métier n'est supprimé physiquement.

---

## 2. Justification des choix : Référence vs Embarquement

### A. Pourquoi `parcelles` est une collection séparée (Référencement) ?
1. **Contraintes fonctionnelles (B.1)** : Les récoltes futures (Jour 4/5), les déclarations de maturité et les pesées s'effectuent à la granularité de la parcelle. Une collection distincte permet des requêtes directes sans devoir désimbriquer des sous-documents.
2. **Règle CON-04 (Anti-pattern des tableaux non bornés)** : Un verger peut compter de nombreuses parcelles au fil des années. Embarquer un tableau de parcelles dans le document verger risquerait d'atteindre la limite BSON de 16 Mo et créerait des verrous d'écriture concurrents sur l'ensemble du verger lors de la mise à jour d'une seule parcelle.
3. **Verrouillage optimiste unitaire** : Deux agriculteurs ou chefs d'équipe peuvent mettre à jour deux parcelles distinctes d'un même verger simultanément sans conflit de version.

### B. Pourquoi la variété est référencée par `varieteId` sans dénormalisation de son nom ?
1. **Règle INT-05 (Prévention des données obsolètes)** : La variété possède des seuils agronomiques (seuil de maturité, rendement moyen) gérés de façon centralisée. Stocker uniquement `varieteId` dans `Parcelle` évite les désynchronisations de noms ou de libellés si une variété est renommée ou corrigée.

### C. Pourquoi la localisation est embarquée (`GeoJsonPoint`) ?
1. La coordonnée géographique définit intrinsèquement le verger (relation 1-to-1 inséparable).
2. L'index spatial MongoDB `2dsphere` indexe directement le champ `localisation` au sein du document `vergers`.

---

## 3. Description détaillée des collections

### 3.1. Collection `varietes`
Référentiel des variétés d'olives tunisiennes exploitées par la coopérative.

| Champ | Type BSON | Contraintes | Description |
|---|---|---|---|
| `_id` | ObjectId / String | Clé primaire | Identifiant technique |
| `code` | String | Unique, Majuscules, Immuable | Code court (ex: `CHEMLALI`, `CHETOUI`) |
| `nom` | String | Obligatoire | Libellé complet de la variété |
| `seuilMaturite` | Double | 0.0 à 1.0 | Seuil d'indice de maturité optimal pour la récolte |
| `rendementMoyenTHa`| Double | Positif | Rendement moyen observé en tonnes/hectare |
| `actif` | Boolean | Défaut `true` | Disponibilité pour de nouvelles parcelles |
| `version` | Long | Optimistic locking | Numéro de révision `@Version` |
| `createdAt` | Date | Audit UTC | Horodatage de création `@CreatedDate` |
| `updatedAt` | Date | Audit UTC | Dernier horodatage de modification `@LastModifiedDate` |

**Exemple de document :**
```json
{
  "_id": "67421ab89c10ef01a8901234",
  "code": "CHEMLALI",
  "nom": "Chemlali de Sfax",
  "seuilMaturite": 0.65,
  "rendementMoyenTHa": 2.2,
  "actif": true,
  "version": 0,
  "createdAt": ISODate("2026-11-23T08:00:00Z"),
  "updatedAt": ISODate("2026-11-23T08:00:00Z")
}
```

---

### 3.2. Collection `vergers`
Représente une exploitation oléicole rattachée à un agriculteur dans le gouvernorat de Sfax.

| Champ | Type BSON | Contraintes | Description |
|---|---|---|---|
| `_id` | ObjectId / String | Clé primaire | Identifiant technique |
| `code` | String | Unique, Immuable | Identifiant métier unique (ex: `VRG-SFX-001`) |
| `nom` | String | 2 à 150 caractères | Désignation usuelle de l'exploitation |
| `proprietaireId` | String | Ref `users._id` | Propriétaire authentifié (résolution session AUT-02) |
| `commune` | String | Optionnel | Délégation/Commune de Sfax (El Hencha, Sakiet Ezzit, etc.) |
| `localisation` | Object GeoJSON | Point `[lng, lat]` | Position géographique (bornée au gouvernorat de Sfax) |
| `precisionGpsMetres`| Double | Optionnel, Nullable | Précision relevée lors de la mesure GPS (VER-04) |
| `sourcePosition` | String | `AUTO` ou `MANUEL` | Origine du positionnement (VER-04) |
| `surfaceHa` | Double | `> 0` et `≤ 1000.0` | Superficie totale cadastrée en hectares (VER-06) |
| `nombreArbres` | Integer | Positif | Estimation du nombre d'oliviers plantés |
| `anneePlantation` | Integer | Année valide | Année d'implantation principale |
| `actif` | Boolean | Défaut `true` | État d'activation (suppression logique INT-03) |
| `version` | Long | Optimistic locking | Numéro de révision `@Version` |
| `createdAt` | Date | Audit UTC | Date de création `@CreatedDate` |
| `updatedAt` | Date | Audit UTC | Date de modification `@LastModifiedDate` |

**Exemple de document :**
```json
{
  "_id": "67422bc99d21ef02b9015678",
  "code": "VRG-SFX-001",
  "nom": "Verger El Hencha Nord",
  "proprietaireId": "67420aa11b01ef00a7800001",
  "commune": "El Hencha",
  "localisation": {
    "type": "Point",
    "coordinates": [10.74, 35.13]
  },
  "precisionGpsMetres": 4.5,
  "sourcePosition": "AUTO",
  "surfaceHa": 15.5,
  "nombreArbres": 250,
  "anneePlantation": 2005,
  "actif": true,
  "version": 0,
  "createdAt": ISODate("2026-11-23T08:15:00Z"),
  "updatedAt": ISODate("2026-11-23T08:15:00Z")
}
```

---

### 3.3. Collection `parcelles`
Découpage agronomique d'un verger selon les variétés d'oliviers cultivées.

| Champ | Type BSON | Contraintes | Description |
|---|---|---|---|
| `_id` | ObjectId / String | Clé primaire | Identifiant technique |
| `vergerId` | String | Ref `vergers._id` | Référence vers le verger parent |
| `nom` | String | 2 à 150 caractères | Nom de la parcelle (ex: "Parcelle Nord A") |
| `varieteId` | String | Ref `varietes._id` | Référence vers la variété (doit être active VER-15) |
| `surfaceHa` | Double | `> 0` | Surface en hectares (somme ≤ surface verger VER-07) |
| `actif` | Boolean | Défaut `true` | État d'activation (suppression logique) |
| `version` | Long | Optimistic locking | Numéro de révision `@Version` |
| `createdAt` | Date | Audit UTC | Date de création `@CreatedDate` |
| `updatedAt` | Date | Audit UTC | Date de modification `@LastModifiedDate` |

**Exemple de document :**
```json
{
  "_id": "67423cd11e32ef03c0128901",
  "vergerId": "67422bc99d21ef02b9015678",
  "nom": "Parcelle Nord A",
  "varieteId": "67421ab89c10ef01a8901234",
  "surfaceHa": 8.0,
  "actif": true,
  "version": 0,
  "createdAt": ISODate("2026-11-23T08:20:00Z"),
  "updatedAt": ISODate("2026-11-23T08:20:00Z")
}
```

---

## 4. Index MongoDB et Requêtes associées

Les index sont créés de manière explicite et idempotente au démarrage de l'application via `MongoIndexConfig` :

| Collection | Nom de l'index | Définition des clés | Requêtes ciblées & Justification |
|---|---|---|---|
| `users` | `idx_users_email_unique` | `{ "email": 1 }` (unique) | Authentification (`login`), contrôle d'unicité normalisée (CPT-01) |
| `vergers` | `idx_vergers_localisation_2dsphere` | `{ "localisation": "2dsphere" }` | Requêtes spatiales `$near` (VER-05), proximité sur la carte de Sfax |
| `vergers` | `idx_vergers_code_unique` | `{ "code": 1 }` (unique) | Recherche et unicité métier du code d'exploitation |
| `vergers` | `idx_vergers_owner_actif` | `{ "proprietaireId": 1, "actif": 1 }` | Liste paginée des vergers de l'agriculteur connecté (AUT-01, INT-03) |
| `parcelles` | `idx_parcelles_verger_actif` | `{ "vergerId": 1, "actif": 1 }` | Récupération des parcelles actives d'un verger pour calcul de surface |
| `parcelles` | `idx_parcelles_variete` | `{ "varieteId": 1 }` | Vérification de l'utilisation d'une variété avant désactivation |
| `varietes` | `idx_varietes_code_unique` | `{ "code": 1 }` (unique) | Unicité du code variétal, lookup d'alimentation du seeder |

---

## 5. Règles de correspondance UML & Documents

- **Classes métier** &rarr; Documents MongoDB (`@Document(collection = "...")`).
- **Attributs de liaison 1-N** &rarr; Références externes par identifiant (`vergerId`, `varieteId`, `proprietaireId`) sans jointure SQL, assurant l'indépendance de cycle de vie et évitant les verrous de mise à jour.
- **Objets de valeur compacts** &rarr; Embarqués directement (ex: coordonnées géométriques GeoJSON `localisation`).
- **Contrôle d'accès objet** &rarr; Filtrage systématique par `proprietaireId` pour les profils `AGRICULTEUR` et masquage (404) pour contrer les attaques IDOR.

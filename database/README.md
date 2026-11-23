# Spécifications Modèle de Données — Jour 3 (Ben Slima)

Ce document détaille les modèles, index et besoins MongoDB préparés au Jour 2 pour la prise en relais au Jour 3.

## Configuration MongoDB

- **Hôte & Port** : `localhost:27017`
- **Base de données** : `oliveconnect` (développement), `oliveconnect_test` (tests)
- **URI de connexion** : `mongodb://localhost:27017/oliveconnect`

---

## 1. Collection existante : `users` (Jour 2)

Gérée par `UserRepository` et `User.java` :

| Champ | Type BSON | Description & Contraintes |
|---|---|---|
| `_id` | ObjectId / String | Identifiant technique unique |
| `name` | String | Nom complet (obligatoire, 2–100 car.) |
| `email` | String | E-mail normalisé en minuscules (**Index unique**) |
| `password` | String | Hachage BCrypt (ex: `$2a$10$...`, jamais en clair) |
| `roles` | Array[String] | Énumérations : `AGRICULTEUR`, `RESPONSABLE_COLLECTE`, `CHEF_EQUIPE`, `RESPONSABLE_LOGISTIQUE`, `RESPONSABLE_PRESSOIR`, `ADMINISTRATEUR` |
| `active` | Boolean | État d'activation du compte (`true` par défaut) |
| `createdAt` | Date (ISODate) | Horodatage d'audit `@CreatedDate` |
| `updatedAt` | Date (ISODate) | Horodatage d'audit `@LastModifiedDate` |

---

## 2. Collections à implémenter au Jour 3 : `orchards` (Vergers)

Recommandations pour la modélisation des vergers de la région de Sfax :

### Modèle `orchards`

```json
{
  "_id": ObjectId("..."),
  "name": "Verger Sidi Mansour Nord",
  "ownerId": "ID_UTILISATEUR_AGRICULTEUR",
  "areaHa": 12.5,
  "variety": "Chemlali",
  "treeCount": 180,
  "location": {
    "type": "Point",
    "coordinates": [10.7712, 34.8015]
  },
  "active": true,
  "version": 0,
  "createdAt": ISODate("2026-11-23T08:00:00Z"),
  "updatedAt": ISODate("2026-11-23T08:00:00Z")
}
```

### Index requis pour `orchards` :
1. **Index géospatial** : `db.orchards.createIndex({ "location": "2dsphere" })` pour les requêtes spatiales et le schéma de carte.
2. **Index de propriétaire** : `db.orchards.createIndex({ "ownerId": 1 })` pour accélérer le filtrage par agriculteur.
3. **Index d'état** : `db.orchards.createIndex({ "active": 1, "ownerId": 1 })`.

---

## 3. Données de démonstration attendues (Fixtures)

Pour le Jour 3, initialiser des vergers situés dans le gouvernorat de Sfax (ex. Thyna, Sakiet Ezzit, Sidi Mansour, Menzel Chaker) avec les variétés locales tunisiennes (`Chemlali`, `Zalmati`, `Sayali`) et rattachés aux utilisateurs créés via l'API.

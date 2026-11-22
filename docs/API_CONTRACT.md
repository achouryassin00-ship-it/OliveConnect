# Proposition de contrat API — à valider avec Ben Ayed

**Aucun endpoint n’est implémenté au Jour 1.** Ce document propose un vocabulaire commun, pas une API existante. Ben Ayed valide les DTO et la sécurité au Jour 2 ; Ben Slima valide le modèle et les index au Jour 3.

## Conventions

- Préfixe `/api`, JSON UTF-8 ; identifiants opaques en chaînes (MongoDB ObjectId sérialisé), jamais noms comme clés.
- Horodatages ISO 8601 UTC (`2026-11-22T07:00:00Z`), affichage et agrégations de journée en `Africa/Tunis`.
- Poids en kg, surfaces en ha, maturité en pourcentage 0–100 ; `null` signifie inconnue, jamais zéro implicite.
- GeoJSON `{ "type": "Point", "coordinates": [10.77, 34.80] }` : longitude puis latitude. Validation serveur des bornes, politique de région Sfax à décider.
- Pagination `?page=0&size=20`, taille 1–100 ; réponse `{ "items": [], "page": 0, "size": 20, "total": 0 }`. Filtres autorisés documentés et validés.
- Création : 201 avec Location et DTO ; lecture/modification : 200 ; opération sans corps : 204. Pas de suppression physique des historiques.
- Champ `version` pour modifications concurrentes ; 409 si obsolète. `Idempotency-Key` proposé pour créations et transitions, comportement et durée de rétention à convenir.

## Routes proposées

| Méthode et route | Entrée / sortie principale | Accès envisagé |
| --- | --- | --- |
| GET /health | `{ status: "UP" }`, sans information sensible | Public |
| POST /auth/register | `{name,email,password}` → demande d’inscription en attente | Visiteur |
| POST /auth/login | `{email,password}` → session et UserDTO selon stratégie validée | Visiteur |
| POST /auth/logout | Fin de session → 204 | Authentifié |
| GET /auth/me | UserDTO `{id,name,email,roles,active}` | Authentifié |
| GET /vergers | PageResult<Orchard>, filtre `q` | Propriétaire / collecte |
| POST /vergers | `{name,areaHa,variety,location}` → Orchard | Agriculteur ; propriétaire dérivé de la session |
| GET /vergers/{id} | Orchard | Propriétaire / collecte |
| PATCH /vergers/{id} | Champs autorisés et version → Orchard | Propriétaire / rôle habilité |
| POST /vergers/{id}/archive | Version → Orchard inactif | Rôle habilité, aucune collecte active |
| GET /recoltes | PageResult<Harvest>, filtres statut/verger/période | Selon rôle et propriétaire |
| POST /recoltes | `{orchardId,estimatedKg,scheduledAt,urgent,justification?}` | Agriculteur |
| POST /recoltes/{id}/transitions | `{targetStatus,version,reason?}` → Harvest | Selon transition et propriétaire |
| GET, POST /alertes | PageResult<FieldAlert> / création de signalement | Selon rôle et propriétaire |
| GET, POST /tournees | PageResult<Tour> / création de brouillon | Collecte / logistique |
| PATCH /tournees/{id} | Affectations, dates, version → Tour | Collecte / logistique avant démarrage |
| GET /ressources | PageResult<Resource>, filtres type/disponibilité/période | Logistique / collecte |
| GET /public/vergers | DTO réduit, positions approximatives, variété, maturité | Public, aucun propriétaire ni poids |
| GET /pressoirs | PageResult<Mill> | Collecte / pressoir |
| POST /pesees | `{tourId,millId,netKg,quality}` → Weighing | Responsable pressoir |
| POST /pesees/{id}/corrections | Nouvelle écriture avec motif et référence | Responsable habilité, audit |
| GET /dashboard | KPI par période et périmètre autorisé | Collecte / pressoir / admin |
| GET /admin/users | PageResult<UserDTO> | Administrateur |

La navigation utilise « Collectes » ; l’entité et l’API utilisent « récoltes » pour la demande d’un agriculteur. Une tournée regroupe les récoltes. Les noms JSON provisoires en anglais sont alignés sur `frontend/src/types/domain.ts`. Les exemples `demo-*` ne sont pas des ObjectId de production.

## Erreurs

```json
{
  "code": "RESOURCE_CONFLICT",
  "message": "Cette ressource est déjà affectée.",
  "timestamp": "2026-11-22T08:00:00Z",
  "fieldErrors": { "resourceIds": "Vérifiez les disponibilités." }
}
```

400 validation ; 401 session absente/expirée ; 403 rôle ou propriétaire interdit ; 404 introuvable ; 409 conflit métier/version/email ; 429 limitation ; 503 service indisponible. Aucun secret ni stack trace. Le client J01 traduit le statut HTTP et ne reprend pas directement le texte serveur. Un adaptateur pour les codes métier et erreurs de champs sera ajouté lors de la validation du contrat.

## Sécurité et intégrité à implémenter côté serveur

Rôles : AGRICULTEUR, RESPONSABLE_COLLECTE, CHEF_EQUIPE, RESPONSABLE_LOGISTIQUE, RESPONSABLE_PRESSOIR, ADMINISTRATEUR ; visiteur = absence de session. Support de plusieurs rôles à valider. Authentification, BCrypt, normalisation email, CORS limité, contrôle de propriétaire et protection du dernier admin sont obligatoires. Préférence à discuter : cookie HttpOnly sécurisé avec CSRF ; alternative JWT à cadrer sans stockage local durable. Le client ne fournit actuellement ni gestion de cookie cross-origin ni CSRF/token.

Machine proposée : DEMANDEE → VALIDEE → PLANIFIEE → EN_COURS → LIVREE → PESEE → ARCHIVEE. Branches REFUSEE/ANNULEE avant démarrage, INTERROMPUE depuis EN_COURS. Le PDF mélange TERMINEE et LIVREE : le type Harvest retient provisoirement LIVREE, Tour retient TERMINEE. Valider la distinction et les règles de reprise avant implémentation. Aucune transition n’est exécutée au Jour 1.

Bloquer modifications de verger/ressources après démarrage et suppressions d’entités utilisées. Pesées immuables avec correction tracée. Réservations atomiques ; chevauchement si débutA < finB ET débutB < finA, marge de transfert à décider. Isoler les agrégations par propriétaire, exclure les pesées remplacées, gérer zéro/null et les saisons sur deux années.

## Décisions ouvertes

Valider transport d’authentification, DTO, noms de champs, workflow d’inscription, reprise après interruption, cohérence TERMINEE/LIVREE, durée d’idempotence et statut de clôture d’alerte (les annexes divergent). Ces décisions n’empêchent pas la démonstration J01 et ne doivent pas être inventées dans l’interface.

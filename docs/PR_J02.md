# feat(j02): socle backend Spring Boot, sécurité JWT et gestion des utilisateurs

Base : `main` · Branche : `feat/j02-backend-auth`

## Résumé

Cette Pull Request implémente le socle technique du backend Spring Boot pour la plateforme OliveConnect (Jour 2 — Ben Ayed). Elle apporte l'architecture applicative, l'authentification sécurisée, la persistance du modèle utilisateur avec MongoDB, ainsi que la suite de tests automatisés couvrant les contrôles d'accès et règles de gestion.

## Ajouts et modifications

- **Architecture Spring Boot 3.3.5 / Maven** :
  - Structure en couches : `Controller` → `Service` → `Repository` → `Entity`.
  - Modules activés : Spring Web, Spring Security 6, Spring Validation, Spring Data MongoDB.
  - Configuration multi-profils (`dev`, `test`).
- **Endpoints implémentés** :
  - `GET /health` et `GET /api/health` : santé du service (public).
  - `POST /api/auth/register` : inscription avec hachage BCrypt et émission JWT.
  - `POST /api/auth/login` : authentification et contrôle du compte actif.
  - `POST /api/auth/logout` : déconnexion (204 No Content).
  - `GET /api/auth/me` : informations du profil connecté.
  - `GET /api/users/{id}` : consultation protégée par propriétaire ou administrateur.
- **Sécurité et intégrité** :
  - Hachage BCrypt des mots de passe.
  - Jetons JWT sécurisés et gestion de session sans état (`STATELESS`).
  - Contrôle d'accès `@PreAuthorize("@securityService.isOwnerOrAdmin(#id)")` empêchant l'accès aux données d'un tiers.
  - Protection CPT-07 empêchant la désactivation du dernier administrateur actif.
  - Gestionnaire d'exceptions global (`GlobalExceptionHandler`) aligné sur le contrat JSON d'erreurs.
- **Documentation et passation** :
  - [backend/README.md](backend/README.md) : guide de lancement, configuration et endpoints.
  - [database/README.md](database/README.md) : spécifications MongoDB pour Ben Slima (Jour 3).
  - [HANDOFF.md](HANDOFF.md) : compte-rendu de passation J02 vers J03.

## Validation des tests

- **Exécution Maven** : `27 tests exécutés, 0 échec, 0 erreur`.
- Cas validés :
  - Inscription valide et mot de passe stocké sous forme de hachage BCrypt.
  - Détection et rejet des e-mails en doublon (409 Conflict).
  - Rejet des données invalides avec rapport d'erreurs de champs (400 Bad Request).
  - Connexion nominale (200 OK) et rejet des mauvais identifiants (401 Unauthorized).
  - Rejet des comptes inactifs (403 Forbidden).
  - Accès non authentifié bloqué (401 Unauthorized).
  - Blocage d'accès aux données d'un autre utilisateur (403 Forbidden).
  - Accès légitime du propriétaire et de l'administrateur (200 OK).
  - Protection du dernier administrateur actif.

Aucun push ni fusion vers `main` n'est réalisé sans validation préalable.

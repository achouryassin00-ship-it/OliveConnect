# Passation — Jour 2 vers Jour 3

- **Auteur sortant (Jour 2)** : Ben Ayed.
- **Prochain responsable (Jour 3)** : Ben Slima (MongoDB et Vergers).
- **Branche** : `feat/j02-backend-auth` ; cible : `main`.
- **État** : Backend Spring Boot 3.3.5 opérationnel avec authentification, sécurité JWT, modèles utilisateurs, endpoints de santé et suite de 27 tests automatisés validés.

---

## 1. Réalisations du Jour 2

1. **Initialisation Spring Boot & Maven** :
   - Mise en place du socle dans `backend/` avec architecture propre : `Controller`, `Service`, `Repository`, `Entity`, `DTO`, `Security`, `Exception`, `Config`.
   - Intégration de Spring Web, Spring Security 6, Spring Validation et Spring Data MongoDB.
   - Profils de configuration : `dev` et `test` (`application.yml`, `application-dev.yml`, `application-test.yml`).

2. **Endpoints implémentés** :
   - `GET /health` & `GET /api/health` : Health check public (`{"status":"UP"}`).
   - `POST /api/auth/register` : Inscription avec hachage BCrypt, validation des champs et émission du jeton.
   - `POST /api/auth/login` : Authentification, vérification du mot de passe et du compte actif.
   - `POST /api/auth/logout` : Clôture de session (204 No Content).
   - `GET /api/auth/me` : Consultation du profil connecté via jeton Bearer JWT.
   - `GET /api/users/{id}` : Consultation sécurisée d'un utilisateur, protégée par `@securityService.isOwnerOrAdmin(#id)`.

3. **Sécurité & Règles d'intégrité** :
   - Hachage des mots de passe avec `BCryptPasswordEncoder`.
   - Jetons JWT sécurisés (HMAC-SHA256, expiration configurable).
   - Gestion fine des rôles (`AGRICULTEUR`, `RESPONSABLE_COLLECTE`, `CHEF_EQUIPE`, `RESPONSABLE_LOGISTIQUE`, `RESPONSABLE_PRESSOIR`, `ADMINISTRATEUR`).
   - Règle CPT-07 : protection empêchant la désactivation du dernier administrateur actif.
   - Gestion centralisée des erreurs avec `GlobalExceptionHandler` (`VALIDATION_ERROR`, `EMAIL_ALREADY_EXISTS`, `BAD_CREDENTIALS`, `ACCOUNT_DISABLED`, `ACCESS_DENIED`, `UNAUTHORIZED`).

4. **Suite de tests (27/27 validés)** :
   - `AuthServiceTest` : Inscription nominale, mot de passe haché en BCrypt, détection d'e-mail dupliqué, connexion nominale, mot de passe erroné, compte désactivé.
   - `UserServiceTest` : Recherche par ID, gestion d'entité introuvable, désactivation, protection du dernier administrateur.
   - `JwtServiceTest` : Génération, signature, validation et extraction des claims (email, userId, roles).
   - `HealthControllerTest` : Vérification des routes de santé.
   - `AuthControllerTest` : Validation des requêtes (400), conflits (409), erreurs d'identifiants (401), comptes inactifs (403), profil me (200/401).
   - `UserControllerSecurityTest` : Tests d'accès propriétaire (200), blocage d'accès aux données d'un autre utilisateur (403), accès administrateur (200).

---

## 2. Consignes pour Ben Slima — Jour 3

1. **Prise en main** :
   - Consulter [database/README.md](database/README.md) pour les spécifications du modèle MongoDB.
   - Vérifier l'exécution du backend et des tests : `cd backend && ./mvnw test`.
2. **Modélisation MongoDB & Vergers** :
   - Créer le modèle `Orchard` (verger) avec nom, surface (`areaHa`), variété (`Chemlali`, etc.), coordonnées GeoJSON (`Point` [longitude, latitude]).
   - Ajouter l'index géospatial 2dsphere et l'index sur `ownerId`.
   - Créer `OrchardRepository`, `OrchardService` et `OrchardController` pour les routes `/api/vergers` (lecture et création réservée aux agriculteurs pour leurs propres vergers en s'appuyant sur `SecurityService.getCurrentUserId()`).
3. **Important** :
   - Ne pas modifier le frontend sauf intégration API autorisée.
   - Respecter le contrat d'API défini dans `docs/API_CONTRACT.md`.

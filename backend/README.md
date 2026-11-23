# Backend OliveConnect — Jour 2 (Ben Ayed)

API REST Spring Boot 3.3.5 / Java 21+ assurant la santé, l'authentification sécurisée, la gestion des utilisateurs et le contrôle d'accès par propriétaire et par rôle.

## Démarrage et exécution

Prérequis : JDK >= 21 (testé avec OpenJDK 25.0.2) et instance MongoDB locale sur le port 27017.

```bash
cd backend
./mvnw spring-boot:run
```

L'API est accessible sur `http://localhost:8080`.

## Tests

Lancer la suite complète de 27 tests unitaires et d'intégration :

```bash
cd backend
./mvnw test
```

Vérifier le packaging de l'application :

```bash
cd backend
./mvnw clean package -DskipTests=false
```

## Endpoints disponibles (Jour 2)

| Méthode | Route | Description | Accès |
|---|---|---|---|
| GET | `/health`, `/api/health` | Vérification de l'état du service (`{"status":"UP"}`) | Public |
| POST | `/api/auth/register` | Inscription d'un utilisateur (mot de passe haché BCrypt) | Public |
| POST | `/api/auth/login` | Connexion, vérification compte actif, émission JWT | Public |
| POST | `/api/auth/logout` | Déconnexion (invalidation côté client / 204 No Content) | Public |
| GET | `/api/auth/me` | Informations du profil de l'utilisateur connecté | Authentifié (Bearer JWT) |
| GET | `/api/users/{id}` | Consultation d'un utilisateur (propriétaire ou admin) | Authentifié (Propriétaire / Admin) |

## Architecture technique

- **Sécurité** : Spring Security 6 avec session `STATELESS`, hachage des mots de passe en `BCryptPasswordEncoder`, jetons HMAC-SHA256 (`JwtService`).
- **Contrôle d'accès** : Service `@securityService.isOwnerOrAdmin(#id)` via `@PreAuthorize` pour garantir le cloisonnement des données entre agriculteurs.
- **Règles métier** :
  - Unicité et normalisation en minuscules des adresses e-mail.
  - Vérification du compte actif (`active = true`) lors de l'authentification et sur chaque requête via le filtre JWT.
  - Règle CPT-07 : interdiction de désactiver le dernier administrateur actif de la plateforme.
- **Validation & Erreurs** : `@Valid` sur les DTOs d'entrée, `GlobalExceptionHandler` retournant une structure d'erreur standardisée (`code`, `message`, `timestamp`, `fieldErrors`).

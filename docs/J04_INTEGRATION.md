# Audit et intégration Jour 4 — Yassine Achour

Audit effectué le 25 novembre 2026. La mission reste le lot Jour 4, indépendamment de la date du calendrier initial. Référence : dossier maître PDF, sujet officiel, programme J04/J05 et cas REC-01 à REC-14. Le périmètre de la demande actuelle prime : demandes de collecte, sans réécriture du backend.

## État vérifié avant développement

Base : `origin/main` au commit `87c6609`, incluant les PR J01, J02 et J03. Dossier de travail initial propre. Nouvelle branche : `feat/j04-harvest-requests-ui`. Aucun reset, aucune modification des branches des collègues, aucun push.

- J01 : SPA React/Vite, navigation, design olive/sauge, fixtures et écrans de démonstration. Pas d’authentification frontend réelle.
- J02 : Spring Boot 3.3.5, JWT Bearer, BCrypt, login/register/me/logout, profils utilisateurs et contrôles d’accès. Session STATELESS.
- J03 : documents `users`, `vergers`, `parcelles`, `varietes`, index uniques/2dsphere/composés, seeder, pagination, filtres propriétaire/actif, contrôle IDOR 404 dans VergerService.
- Absents : controller/service/repository/entity de récoltes, DTO de création/liste/détail, historique et annulation. Aucune route `/api/recoltes` utilisée au J04.
- Tests hérités exécutés : 52 Maven réussis (dont MongoDB), 5 Vitest et 2 Playwright réussis avant modification.

## Connexions réalisées

| Endpoint existant | Usage frontend |
| --- | --- |
| POST /api/auth/login | Connexion avec email/password ; reçoit token et user |
| GET /api/auth/me | Vérifie le profil associé au token avant d’ouvrir la session |
| POST /api/auth/logout | Appel de déconnexion, suppression locale du token même en cas d’échec réseau |
| GET /api/vergers?page=N&size=100 | Charge toutes les pages, garde seulement les vergers actifs du propriétaire courant |
| GET /api/vergers/{id} | Revérifie propriétaire et disponibilité lors de la vérification d’un brouillon |

Le contrôle serveur des vergers a été inspecté ; le filtre frontend s’ajoute à ce contrôle, notamment pour un utilisateur cumulant AGRICULTEUR et un rôle staff. Un compte sans AGRICULTEUR n’utilise pas ce parcours. Les récoltes n’ont aucune protection serveur testable tant que leur API n’existe pas.

Les DTO J03 ont des noms français (`nom`, `proprietaireId`, `actif`), contrairement aux fixtures anglaises du J01. `src/types/api.ts` modélise les DTO existants ; l’adaptateur transforme les vergers en choix du formulaire. Pagination réelle : `totalElements`, `totalPages`, pas le `total` historique du J01.

## Modes et conservation des données

- `VITE_API_ENABLED=false` (défaut) : `/collectes` est une démonstration explicite. `/demo/collectes` force toujours cet espace isolé.
- `VITE_API_ENABLED=true` : `/collectes` utilise une session réelle et de vrais vergers. Aucune liste fictive n’est injectée dans ce mode. Le bouton « Vérifier le brouillon » effectue seulement un GET du verger ; le message confirme explicitement l’absence d’envoi.
- JWT et profil en mémoire React, jamais localStorage/sessionStorage. Rechargement = reconnexion. Une erreur 401 conserve le brouillon dans l’écran et propose la reconnexion ; un changement de compte ou une déconnexion détruit les données du compte précédent.
- Brouillons et exemples uniquement en mémoire de l’espace Collectes. Ils disparaissent en quittant cet espace ou en rechargeant. Pas de file hors ligne ni de promesse d’enregistrement. Un échec réseau pendant la vérification laisse les saisies intactes.
- Le dashboard, Vergers hors formulaire, Carte, Alertes et les autres modules restent des démonstrations J01, avec bandeau explicite. L’inscription reste une maquette sans envoi.

## Préparation des récoltes : proposition, pas contrat validé

Le contrat J03 ne contient aucune définition de récolte. Il n’est pas modifié au J04. `src/features/harvests/model.ts` définit uniquement un modèle de présentation :

| Champ UI | Décision provisoire |
| --- | --- |
| orchardId | Verger actif du propriétaire authentifié |
| desiredDate | Date locale YYYY-MM-DD à Tunis, aujourd’hui ou ultérieure ; pas de conversion arbitraire vers un horaire UTC |
| estimatedKg | Quantité facultative, positive si renseignée ; virgule acceptée ; 3 décimales maximum pour la saisie |
| urgent | Normale / urgente |
| justification | Obligatoire pour l’urgence (REC-08), maximum 500 caractères |
| id, status, createdAt | Données de présentation fictives ; un futur backend devra les produire |

Les statuts reprennent le vocabulaire provisoire J01. Aucune transition n’est proposée et aucun historique n’est fabriqué. Les plafonds, période de saison, conversion date/heure, noms JSON, quantité obligatoire/facultative et statuts TERMINEE/LIVREE restent à valider au J05.

`src/api/harvests.ts` expose un port TypeScript (list/get/create) avec erreur explicite HARVEST_API_UNAVAILABLE. Il n’appelle aucun endpoint supposé. Les capacités list/create/history/cancel sont toutes fausses. La signature create réserve une clé d’idempotence ; sa sémantique serveur reste à définir. Le double clic est bloqué immédiatement par un verrou en mémoire et le bouton désactivé ; cela ne remplace pas l’idempotence serveur.

## Manques à traiter au Jour 5

1. Valider le DTO et implémenter liste paginée filtrée côté serveur, création et détail des récoltes (routes à convenir, `/api/recoltes` restant une proposition historique).
2. Déduire le propriétaire de la session, refuser les vergers étrangers/inactifs/inexistants et vérifier le rôle. Tester IDOR sur chaque route, pas uniquement via l’UI.
3. Refuser dates invalides, doublons actifs et transitions interdites ; définir idempotence et conflits 409 en concurrence.
4. Définir l’annulation avant/après planification ; aucune action Annuler n’est affichée au J04 faute de contrat réel.
5. Fournir l’historique seulement s’il est réellement persisté, avec horodatages serveur et auteur.
6. Remplacer `DefaultVergerUsageChecker` qui retourne actuellement toujours false par la vérification de récoltes actives.
7. Brancher le port frontend après validation, avec tests nominaux et erreurs métier. Les tests API simulés du J04 ne prouvent pas l’intégrité des futures récoltes.

## Écarts backend observés et transmis sans modification silencieuse

- **Prioritaire :** `AuthService.register` reprend `RegisterRequest.roles` depuis une route publique ; un client pourrait demander un rôle privilégié. À corriger et tester côté serveur avant toute exposition publique. Le formulaire J04 n’active pas l’inscription.
- Le contrat décrit logout comme authentifié, mais `SecurityConfig` autorise cette route publiquement. Logout renvoie 204 sans révocation serveur du JWT ; l’interface supprime le jeton local, sans annoncer sa révocation.
- `VergerController.createVerger` ne porte pas de restriction de rôle et `VergerService` vérifie la propriété mais pas le rôle AGRICULTEUR pour la création. Le contrôle UI ne corrige pas ce manque serveur.
- `application.yml` contient déjà des valeurs de secours JWT et mot de passe de seed connues. Elles sont héritées et doivent être remplacées par des variables privées avant déploiement. Aucun nouveau secret n’est ajouté au dépôt J04.
- `docs/ARCHITECTURE.md` et `database/README.md` décrivaient encore certaines intentions J01/J02. Le schéma réel de référence est `docs/MONGODB_SCHEMA.md` et le code J03.

## Vérifications reproductibles

Depuis frontend : `npm.cmd run build`, `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd test`, `npm.cmd run test:e2e`.

Les tests Playwright lancent deux serveurs Vite isolés : 5173 (démo) et 5174 (API avec réponses interceptées). Fermer les serveurs personnels occupant ces ports avant de lancer la suite. Aucun backend n’est nécessaire pour cette suite ; le filtrage réseau distingue les modules Vite `/src/api/` des véritables requêtes `/api/`.

Test réel opt-in, avec MongoDB local disponible :

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-25' # adapter au JDK 21+ installé
cd backend
.\mvnw.cmd -B -ntp package -DskipTests
cd ../frontend
python scripts/verify-live.py
```

Le script lance le jar sur 8085 et Vite sur 5174, génère les secrets dans l’environnement du processus, autorise cette origine CORS et utilise une nouvelle base `oliveconnect_j04_qa_<suffixe>`. Les services de test sont arrêtés ensuite. La base QA reste disponible pour inspection et n’est pas supprimée automatiquement. Les captures/logs ignorés sont dans tmp/ ; aucun token ni mot de passe ne doit être joint à la PR.

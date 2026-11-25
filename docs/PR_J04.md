# feat(j04): parcours agriculteur et demandes de collecte

Base : main (`87c6609` lors de l’audit). Branche : `feat/j04-harvest-requests-ui`. PR préparée localement ; aucun push ni merge sans confirmation de Yassine.

Le frontend J01 ne disposait que d’aperçus, tandis que J02/J03 apportaient une API d’authentification et de vergers. Cette évolution connecte login/me/logout et la lecture des vergers, et fournit un parcours de préparation de collecte accessible sur mobile.

En mode connecté, l’agriculteur sélectionne uniquement un de ses vergers actifs puis vérifie son brouillon avec le backend. **Aucune récolte n’est envoyée**, car les endpoints/DTO de récoltes ne sont pas encore implémentés. En démonstration isolée : liste, recherche, filtres, détail et ajout d’exemples locaux. Aucun faux historique ou faux succès d’annulation.

Validation : build, TypeScript et ESLint réussis ; 26 tests unitaires frontend et 14 parcours Edge desktop/mobile réussis (API interceptée). 52 tests backend hérités réussis et 1 parcours réel Spring Boot/MongoDB réussi : authentification, contrôle propriétaire et vérification de brouillon. Résultats et limites détaillés dans HANDOFF.md.

Le backend et le contrat J03 restent inchangés. Les écarts d’audit (rôles de l’inscription publique, usage checker provisoire, logout et valeurs de configuration de développement) sont transmis explicitement dans docs/J04_INTEGRATION.md.

Revue Ben Ayed : valider DTO/statuts de récolte, règles d’annulation et d’idempotence, puis implémenter et brancher le port API au Jour 5. Ne pas confondre les réponses interceptées des tests frontend avec une API de récoltes fonctionnelle.

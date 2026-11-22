# feat(j01): fondations React et interface de démonstration OliveConnect

Base : `main` · Branche : `feat/j01-foundation`

Le dépôt initial ne contenait qu’un README. Cette PR apporte une SPA React/Vite/TypeScript navigable et responsive, les douze écrans demandés et une base d’intégration HTTP pour Spring Boot. Les données sont explicitement fictives ; aucun compte ni opération métier n’est enregistré.

Ajouts : layout olive/sauge, composants réutilisables, recherche locale, états d’interface, aide, types de domaine, contrat API proposé, dossiers réservés aux J02/J03 et documentation de passation.

Validation : build, TypeScript, ESLint, 5 tests unitaires et 2 parcours Playwright Edge ; navigation desktop, toutes les routes à 320 px, rechargement direct, 404 et absence d’appels API. Audit npm sans vulnérabilité. Captures desktop/mobile examinées.

Limites : connexion et inscription non fonctionnelles, carte schématique, pas de backend/MongoDB, de RBAC réel, de CRUD, de calcul métier ni de déploiement. Arbitrages API listés dans docs/API_CONTRACT.md.

Revue demandée à Ben Ayed : vocabulaire JSON, authentification, noms de statuts et responsabilités Jour 2.

**PR préparée localement, pas créée sur GitHub** : publication de la branche nécessaire et soumise à la confirmation de Yassine. Aucun push ni merge exécuté.

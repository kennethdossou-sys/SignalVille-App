# Avancement SignalVille

## Séance 2 — Fondations + Module 1 (citoyen)

**DoD EPF S2** : « Login → JWT → route protégée → affichage selon rôle : connexion front↔back prouvée. »
→ **Atteinte et vérifiée dans le navigateur** (voir la section Démonstration du README).

### Backend — fait

- [x] Projet Spring Boot 4.1.0 / Java 21, PostgreSQL 16 via Docker Compose (port 5433)
- [x] Enums domaine : `Role`, `AccountStatus`, `ReportStatus`, `Priority`
- [x] Entités : `User`, `RefreshToken`, `Category`, `Report`, `ReportPhoto`, `StatusHistory`
- [x] Migrations Flyway `V1` → `V4` (users, refresh_tokens, categories + seed, reports/photos/historique)
- [x] Sécurité : `SecurityConfig`, `JwtAuthenticationFilter`, `JwtService` (JJWT 0.12), BCrypt, CORS 4200
- [x] `@RestControllerAdvice` renvoyant l'`ErrorResponse` du contrat (y compris pour les erreurs de sécurité)
- [x] Auth : `register` (201/409), `login` (200/401 + `lastLoginAt`), `refresh` (rotation), `logout` (204)
- [x] `GET /users/me`
- [x] `GET /categories` (public, actives uniquement) + seed de 4 catégories
- [x] `POST /reports` multipart 1–3 photos, référence `SV-YYYY-NNNNNN`, historique initial
- [x] `GET /reports` paginé, `GET /reports/{id}`, `PUT /reports/{id}`, `POST /reports/{id}/cancel`
- [x] Upload disque + `GET /photos/{id}` authentifié, validation 5 Mo / JPEG-PNG

### Frontend — fait

- [x] Arborescence `core/` (auth, guards, interceptors), `features/` (auth, dashboard), `shared/models`
- [x] `environment.ts` avec `apiUrl: http://localhost:8080/api/v1`
- [x] `AuthService` (signal `currentUser`), intercepteur Bearer + refresh sur 401, `authGuard` / `roleGuard`
- [x] Pages `/login` (gestion 401), `/register` (gestion 409 + 400), `/dashboard` (profil + liste)
- [x] Types TypeScript miroir du contrat OpenAPI

### Non fait — reporté

- [ ] Pages `/reports`, `/reports/new` (carte Leaflet), `/reports/:id` — **backend prêt, UI non écrite**
- [ ] Tests d'intégration Testcontainers — validation faite manuellement par 18 appels HTTP
- [ ] Swagger UI (springdoc)
- [ ] Module 2 : interventions, notifications, statistiques

Détail et justifications : [`CLAUDE_CODE_SESSION_REPORT.md`](CLAUDE_CODE_SESSION_REPORT.md).

# SignalVille

Plateforme de signalement d'incidents urbains — projet M1 EPF Africa.

- **Backend** : Spring Boot 4.1.0, Java 21, PostgreSQL 16, Flyway, Spring Security + JWT
- **Frontend** : Angular 21 (standalone, zoneless), SCSS, sans SSR
- **Contrat d'API** : [`docs/api/signalville-openapi.yaml`](docs/api/signalville-openapi.yaml) — **source de vérité**

## Prérequis

| Outil | Version |
|---|---|
| JDK | **21** (le `pom.xml` cible `java.version=21`) |
| Node.js | 22.x |
| Docker Desktop | pour PostgreSQL |

> Sur cette machine, `JAVA_HOME` pointe par défaut sur le JDK 17. Il faut le forcer sur le JDK 21
> avant tout appel Maven, sinon la compilation échoue :
>
> ```bash
> $env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
> ```

## 1. Base de données

```bash
cd signalville-backend && docker compose up -d
```

PostgreSQL 16 écoute sur **localhost:5433** (conteneur `signalville-db`, base/user/pass = `signalville`).
Flyway applique les migrations `V1` à `V4` au démarrage du backend.

## 2. Backend

```bash
cd signalville-backend && .\mvnw.cmd spring-boot:run
```

API disponible sur **http://localhost:8080/api/v1** (context-path aligné sur le contrat OpenAPI).

## 3. Frontend

```bash
cd signalville-frontend && npm install && npm start
```

Application disponible sur **http://localhost:4200**.

## Démonstration de bout en bout

1. Ouvrir http://localhost:4200 → redirection vers `/login`.
2. Cliquer « Créer un compte citoyen », remplir le formulaire, valider.
   → `POST /auth/register` renvoie **201** + `AuthResponse`, le front stocke les tokens et bascule sur `/dashboard`.
3. Le dashboard affiche le profil renvoyé par **`GET /users/me`** (route protégée par JWT) et la liste
   **`GET /reports`** limitée aux signalements du citoyen connecté.
4. « Se déconnecter » → `POST /auth/logout` (**204**), le refresh token est révoqué en base.
5. Se reconnecter via `/login` : `lastLoginAt` est mis à jour côté serveur.

Un mot de passe erroné affiche « E-mail ou mot de passe incorrect » (401),
un e-mail déjà pris affiche « Un compte existe déjà avec cette adresse e-mail » (409).

## Endpoints implémentés

| Méthode | Route | Accès |
|---|---|---|
| POST | `/auth/register` | public — crée un CITOYEN |
| POST | `/auth/login` | public |
| POST | `/auth/refresh` | public — rotation du refresh token |
| POST | `/auth/logout` | authentifié — révoque le refresh en base |
| GET | `/users/me` | authentifié |
| GET | `/categories` | public — catégories actives |
| POST | `/reports` | CITOYEN — multipart, 1 à 3 photos |
| GET | `/reports` | authentifié — citoyen : les siens uniquement |
| GET | `/reports/{id}` | authentifié — 403 si non propriétaire |
| PUT | `/reports/{id}` | CITOYEN — 409 hors statut NOUVEAU |
| POST | `/reports/{id}/cancel` | CITOYEN — 409 hors statut NOUVEAU |
| GET | `/photos/{id}` | authentifié — propriétaire uniquement |

Voir [`CLAUDE_CODE_SESSION_REPORT.md`](CLAUDE_CODE_SESSION_REPORT.md) pour les décisions techniques
et [`PROGRESS.md`](PROGRESS.md) pour l'avancement.

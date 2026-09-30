# SecurityConfig — Guide de lecture ligne par ligne

**Projet** : SignalVille  
**Fichier** : `signalville-backend/src/main/java/africa/epf/signalville_backend/infrastructure/config/SecurityConfig.java`  
**Public** : Kenneth Dossou (auteur du projet)  
**Objectif** : comprendre chaque décision de configuration pour la défendre en soutenance et la maintenir sereinement en S3/S4.

---

## Sommaire

1. [Vue d'ensemble : ce que fait cette classe](#1-vue-densemble)
2. [Bloc 1 — Annotations de classe](#2-bloc-1--annotations-de-classe)
3. [Bloc 2 — Constructeur et injection des dépendances](#3-bloc-2--constructeur-et-injection-des-dépendances)
4. [Bloc 3 — Configuration HTTP de base](#4-bloc-3--configuration-http-de-base)
5. [Bloc 4 — Règles d'autorisation, gestion des erreurs, insertion du filtre JWT](#5-bloc-4--règles-dautorisation-gestion-des-erreurs-insertion-du-filtre-jwt)
6. [Bloc 5 — Configuration CORS détaillée](#6-bloc-5--configuration-cors-détaillée)
7. [Bloc 6 — PasswordEncoder](#7-bloc-6--passwordencoder)
8. [Vérifications de compréhension](#8-vérifications-de-compréhension)

---

## 1. Vue d'ensemble

`SecurityConfig` est la classe qui configure toute la sécurité HTTP de l'application Spring Boot. Elle répond à cinq questions :

- **Qui a le droit d'appeler quelle route ?** (autorisation)
- **Comment identifie-t-on un utilisateur ?** (authentification par JWT)
- **Comment CORS est-il géré ?** (accès depuis Angular)
- **Comment les mots de passe sont-ils hachés et vérifiés ?** (BCrypt)
- **Comment les erreurs de sécurité sont-elles formatées ?** (uniformité avec le reste de l'API)

C'est la classe la plus critique du projet du point de vue sécurité. Une erreur ici peut exposer toute l'API.

---

## 2. Bloc 1 — Annotations de classe

```java
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class, CorsProperties.class})
public class SecurityConfig {
```

### `@Configuration`

Dit à Spring : « cette classe contient des définitions de beans. »

Un bean, c'est un objet géré par Spring — créé une fois au démarrage, injecté partout où on en a besoin. Ce fichier contient trois méthodes annotées `@Bean` : `securityFilterChain()`, `corsConfigurationSource()`, `passwordEncoder()`. Sans `@Configuration` sur la classe, Spring ne scannerait pas ces méthodes, et aucun de ces beans n'existerait — aucune sécurité configurée.

### `@EnableMethodSecurity`

Active la sécurité **au niveau des méthodes**, via les annotations `@PreAuthorize` et `@PostAuthorize` posées sur les contrôleurs.

Exemple concret dans le projet :

```java
@PreAuthorize("hasRole('CITOYEN')")
@PostMapping("/reports")
public ReportResponse createReport(...) { ... }
```

Spring vérifie automatiquement, avant d'exécuter la méthode, que l'utilisateur connecté a bien le rôle CITOYEN. Sinon → `AccessDeniedException` → 403.

**Danger si absente** : les annotations `@PreAuthorize` deviennent des commentaires silencieux — Spring les ignore. Aucune erreur au démarrage, aucun test évident qui échoue, mais toutes les règles de rôle disparaissent. Un utilisateur simple pourrait alors appeler des endpoints réservés aux administrateurs. C'est exactement le type de faille qu'on veut éviter à tout prix.

### `@EnableConfigurationProperties({JwtProperties.class, StorageProperties.class, CorsProperties.class})`

Active trois classes de configuration qui matérialisent les sections YAML en objets Java typés.

Dans `application.yml` :

```yaml
signalville:
  cors:
    allowed-origins:
      - http://localhost:4200
  jwt:
    secret: ...
  storage:
    upload-dir: ...
```

Ces valeurs doivent atterrir quelque part dans le code Java. `CorsProperties`, `JwtProperties`, `StorageProperties` sont trois classes/records annotées `@ConfigurationProperties` qui portent ces sections. Spring ne les traite pas automatiquement — il faut lui indiquer lesquelles activer. C'est le rôle de `@EnableConfigurationProperties`.

C'est grâce à ça que plus bas dans le constructeur, on peut injecter `CorsProperties corsProperties` — Spring l'a déjà construite et remplie depuis le YAML.

---

## 3. Bloc 2 — Constructeur et injection des dépendances

```java
private final JwtAuthenticationFilter jwtAuthenticationFilter;
private final CorsProperties corsProperties;
private final HandlerExceptionResolver handlerExceptionResolver;

public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                      CorsProperties corsProperties,
                      @Lazy @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.corsProperties = corsProperties;
    this.handlerExceptionResolver = handlerExceptionResolver;
}
```

Trois dépendances injectées.

### `JwtAuthenticationFilter`

Le filtre custom qui, à chaque requête, extrait le token `Authorization: Bearer ...` et l'authentifie. On le voit ajouté à la chaîne de filtres plus bas dans `securityFilterChain`.

### `CorsProperties`

La classe qui matérialise la config CORS depuis le YAML. Utilisée dans `corsConfigurationSource()`.

### `HandlerExceptionResolver`

Composant standard de Spring MVC qui sait convertir n'importe quelle exception levée pendant une requête en réponse HTTP formatée.

**Pourquoi ici** : normalement, quand Spring Security intercepte un accès non autorisé, il renvoie une réponse brute (401 ou 403 avec un corps par défaut). Le problème : cette réponse ne passe pas par le `@RestControllerAdvice` (`GlobalExceptionHandler`), donc elle ne ressemble pas à un `ErrorResponse` conforme au contrat OpenAPI.

En déléguant à `HandlerExceptionResolver`, l'exception est renvoyée dans le circuit MVC standard → traverse le `@RestControllerAdvice` → produit un `ErrorResponse` uniforme. Résultat : le frontend a une seule façon de gérer les erreurs, qu'elles viennent d'un endpoint métier ou d'une erreur de sécurité.

### Le `@Lazy` sur l'injection

`HandlerExceptionResolver` fait partie de la config MVC de Spring, qui peut elle-même dépendre de la config sécurité. Cycle de dépendances au démarrage : sécurité → MVC → sécurité.

`@Lazy` dit à Spring : « n'injecte pas tout de suite, injecte un proxy qui résoudra la vraie instance au premier appel. » Ça casse le cycle proprement.

### Le `@Qualifier("handlerExceptionResolver")`

Spring peut avoir plusieurs beans du même type `HandlerExceptionResolver`. Le qualifier précise lequel on veut — celui enregistré sous ce nom exact.

---

## 4. Bloc 3 — Configuration HTTP de base

```java
http
    .csrf(AbstractHttpConfigurer::disable)
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .formLogin(AbstractHttpConfigurer::disable)
    .httpBasic(AbstractHttpConfigurer::disable)
    .logout(AbstractHttpConfigurer::disable)
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

### `csrf(disable)`

Désactive la protection CSRF (Cross-Site Request Forgery).

Cette protection existe pour les applications avec sessions cookies — le navigateur envoie automatiquement le cookie à chaque requête, ce qui permet à un site malveillant de déclencher des actions à l'insu de l'utilisateur. Solution CSRF : exiger un token additionnel dans chaque formulaire.

**Vous êtes en stateless JWT** : pas de session, pas de cookie envoyé automatiquement — la protection CSRF est inutile ici. Le JWT est envoyé explicitement dans le header `Authorization` par le JavaScript, pas automatiquement par le navigateur.

### `cors(...)`

Active le CORS et pointe vers le bean `corsConfigurationSource()`. Le contenu de ce bean est détaillé au Bloc 5.

### `formLogin(disable)`

Désactive la page HTML de login que Spring Security propose par défaut. Vous êtes une API REST — pas de formulaire côté serveur, tout se passe en JSON via Angular.

### `httpBasic(disable)`

Désactive l'authentification Basic (login/password en base64 dans le header à chaque requête). Vous utilisez JWT — cette méthode alternative est inutile et pourrait créer une seconde voie d'entrée non désirée.

### `logout(disable)`

Désactive l'endpoint `/logout` par défaut de Spring Security, qui invalide une session cookie. Vous avez votre propre `POST /auth/logout` qui gère la révocation du refresh token en base — celui de Spring est inutile et pourrait interférer.

### `sessionManagement(STATELESS)`

Le point le plus important. Dit à Spring : « ne crée jamais de session HTTP. » À chaque requête, l'utilisateur doit prouver son identité à nouveau (via son token JWT). Aucun état côté serveur.

**Pourquoi stateless en JWT ?** L'idée même du JWT est que tout est dans le token. Le serveur ne stocke rien. C'est ce qui permet la scalabilité : n'importe quel serveur peut traiter n'importe quelle requête, sans avoir à synchroniser des sessions entre nœuds.

---

## 5. Bloc 4 — Règles d'autorisation, gestion des erreurs, insertion du filtre JWT

C'est le cœur métier de la classe.

### 5.1 — `authorizeHttpRequests` : la table des autorisations

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .requestMatchers(HttpMethod.POST,
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password").permitAll()
    .requestMatchers(HttpMethod.GET, "/categories").permitAll()
    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
    .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .anyRequest().authenticated())
```

Spring évalue les règles **de haut en bas**, la première qui matche gagne. **L'ordre est critique.**

| # | Règle | Justification |
|---|---|---|
| 1 | `OPTIONS /**` → permitAll | Le preflight CORS ne porte jamais de token. Doit toujours passer. |
| 2 | `POST /auth/{register,login,refresh,forgot-password,reset-password}` → permitAll | Endpoints publics par nature : on ne peut pas exiger un token pour se connecter. Seul `POST` est autorisé (protection contre les mauvais verbes). |
| 3 | `GET /categories` → permitAll | Catalogue consultable sans compte (affichage public). |
| 4 | `/actuator/health,info` → permitAll | Endpoints de monitoring, appelés par des sondes sans token. Les autres endpoints Actuator (`/env`, `/beans`...) tombent dans le filet ci-dessous. |
| 5 | Swagger UI → permitAll | Anticipe l'ajout futur de Swagger UI. |
| 6 | `anyRequest().authenticated()` | **Filet de sécurité fondamental** : tout ce qui n'a pas matché exige un utilisateur authentifié. |

**Point crucial** : `/auth/logout` **n'est pas** dans la liste des endpoints publics — c'est délibéré. Se déconnecter exige d'être connecté. Il tombe donc dans `anyRequest().authenticated()`.

**Principe secure by default** : si vous ajoutez un nouvel endpoint et oubliez d'écrire une règle pour lui, il est protégé automatiquement. Mieux vaut un 401 par erreur qu'une fuite de données.

### 5.2 — `exceptionHandling` : donner du sens aux erreurs de sécurité

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) ->
            handlerExceptionResolver.resolveException(request, response, null, authException))
    .accessDeniedHandler(accessDeniedHandler()))
```

Deux types d'exceptions peuvent survenir :

- **`AuthenticationException`** : « je ne sais pas qui vous êtes » (pas de token, token invalide, token expiré). → 401 attendu.
- **`AccessDeniedException`** : « je sais qui vous êtes, mais vous n'avez pas le droit ». → 403 attendu.

**Par défaut, Spring Security répond avec un corps plat** qui ne correspond pas à votre contrat OpenAPI. Solution utilisée ici : déléguer au `HandlerExceptionResolver`. L'exception remonte alors dans le circuit MVC → traverse le `@RestControllerAdvice` → produit un `ErrorResponse` uniforme, identique aux erreurs métier.

Le résultat : le frontend a **une seule structure JSON à gérer** pour toutes les erreurs de l'API.

### 5.3 — `addFilterBefore` : insérer le filtre JWT dans la chaîne

```java
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

Spring Security fonctionne comme une chaîne de filtres empilés. Chaque requête traverse la chaîne dans l'ordre. Cette ligne place `JwtAuthenticationFilter` juste avant `UsernamePasswordAuthenticationFilter`.

`UsernamePasswordAuthenticationFilter` est le filtre historique qui gère le login par formulaire — déjà désactivé pour vous, mais qui reste un **point de repère standard** dans la chaîne : le point où l'authentification se joue.

Ce que ça produit concrètement :

1. Requête arrive → `JwtAuthenticationFilter` tourne tôt.
2. Si le JWT est valide, il remplit le `SecurityContext` avec un objet `Authentication`. Les filtres suivants (autorisation) le voient authentifié.
3. Si le JWT est absent ou invalide, il ne fait rien — la chaîne poursuit, et finira par lever une `AuthenticationException` sur les endpoints protégés.

**Analogie** : le `JwtAuthenticationFilter` est un portier qui inspecte les cartes d'identité à l'entrée. S'il vous reconnaît, il vous donne un badge. Les portes protégées à l'intérieur vérifient le badge, pas la carte d'identité. Pas de badge = accès refusé.

---

## 6. Bloc 5 — Configuration CORS détaillée

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.allowedOrigins());
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

**Rappel** : CORS est une protection **côté navigateur**. Quand Angular (`localhost:4200`) appelle Spring (`localhost:8080`), les origines diffèrent → le navigateur bloque par défaut. Ce bean dit au navigateur ce qu'il a le droit de faire.

### Ligne par ligne

**`setAllowedOrigins(corsProperties.allowedOrigins())`**  
La liste des origines autorisées, récupérée dynamiquement depuis `application.yml`. Éviter le `"*"` (interdit avec `allowCredentials=true`, voir plus bas).

**`setAllowedMethods(...)`**  
Verbes HTTP autorisés. Inclut explicitement `OPTIONS` (le preflight). Liste complète pour couvrir tous les endpoints actuels et futurs.

**`setAllowedHeaders(List.of("*"))`**  
Autorise tous les headers dans la requête (Authorization, Content-Type, headers custom...). Simplification acceptable en dev, à durcir éventuellement en prod.

**`setExposedHeaders(List.of("Location", "Content-Disposition"))`**  
**Subtilité importante** : par défaut, un JavaScript frontend n'a accès qu'à quelques headers "safe" dans la réponse. Les autres sont masqués par le navigateur, même s'ils sont bien envoyés par le serveur. Cette ligne rend explicitement lisibles :
- **`Location`** : renvoyé sur un `POST /reports` réussi (`Location: /reports/abc-123`). Sans exposition, Angular ne peut pas lire l'URL de la ressource créée.
- **`Content-Disposition`** : sur `GET /photos/{id}`, indique le nom du fichier téléchargé. Sans exposition, Angular ne peut pas nommer le fichier côté client.

**`setAllowCredentials(true)`**  
Autorise le navigateur à envoyer les credentials (cookies, headers d'auth) dans les requêtes cross-origin. **Piège** : quand `allowCredentials=true`, la norme CORS interdit l'usage de `"*"` dans `allowedOrigins`. C'est pour ça qu'on liste des origines explicites.

**`setMaxAge(3600L)`**  
Durée en secondes de mise en cache de la réponse au preflight par le navigateur. 3600 = 1h. Concrètement : le premier `POST /reports` déclenche un `OPTIONS` de vérification. Les 100 suivants dans l'heure n'ont plus besoin de refaire le preflight. Gain de performance significatif.

### Le `UrlBasedCorsConfigurationSource`

Conteneur qui permet d'associer différentes configurations CORS à différents patterns d'URL. Ici on applique la même configuration à toutes les URLs (`/**`) — un seul pattern, un seul objet `CorsConfiguration`.

---

## 7. Bloc 6 — PasswordEncoder

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Rôle

Composant qui hache et vérifie tous les mots de passe. Utilisé à trois endroits :

1. **À l'inscription** (`AuthService.register`) : `passwordEncoder.encode(rawPassword)` transforme le mot de passe en clair en hash BCrypt. Le hash est stocké en base.
2. **Au login** (`AuthService.login`) : `passwordEncoder.matches(rawPassword, storedHash)` compare le mot de passe fourni au hash stocké. Renvoie un booléen — le hash n'est jamais "déchiffré".
3. **À la migration V5** (indirectement) : les hashs BCrypt insérés en dur doivent avoir été générés par un `BCryptPasswordEncoder` compatible pour que le login admin fonctionne.

### Pourquoi BCrypt

Standard de facto en 2026, pour trois raisons :

- **Résistant au brute force** : délibérément lent (2¹⁰ itérations par défaut). Un attaquant qui vole la base ne peut tester que quelques milliers de mots de passe par seconde, pas des milliards comme avec un simple SHA.
- **Sel intégré** : chaque hash contient son propre sel aléatoire (les caractères après `$2a$10$`). Deux utilisateurs avec le même mot de passe auront des hashs différents. Rainbow tables inutilisables.
- **Auto-vérifiable** : le hash contient toutes les métadonnées (algorithme, cost, sel). `matches()` sait tout seul comment vérifier.

### Le cost factor

`new BCryptPasswordEncoder()` sans argument utilise **cost 10 par défaut**. Compromis actuel entre sécurité et performance :

- Trop bas (ex. 4) : trop rapide, plus vulnérable au brute force.
- Trop haut (ex. 15) : login trop lent, expérience dégradée.

Cost 10 = ~100 ms par vérification sur du hardware moderne. Imperceptible pour un utilisateur, trop lent pour un attaquant.

### Pourquoi un bean global

- **Injection dans les services** : `AuthService` reçoit `PasswordEncoder` en dépendance.
- **Cohérence** : un seul bean = une seule config BCrypt pour toute l'application. Impossible d'avoir un service qui encode avec un cost et un autre qui vérifie avec un cost différent.

### Lien avec la migration V5

Le hash inséré dans la migration V5 :  
`$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy`

Décomposition : `$2a$` (BCrypt version 2a) + `10$` (cost 10) + sel + hash. **Ce format correspond exactement à ce que produit `new BCryptPasswordEncoder()`.** C'est ce qui garantit que le login admin fonctionne.

Note subtile : `BCryptPasswordEncoder.matches()` lit le cost dans le hash stocké et l'utilise pour la vérification. Donc changer le cost par défaut casse la production de nouveaux hashs, pas la vérification d'anciens.

---

## 8. Vérifications de compréhension

Questions posées pendant la relecture, avec les réponses attendues.

### Q1 — Si on retire `@EnableMethodSecurity` de la classe, que se passe-t-il ?

**Réponse** : les annotations `@PreAuthorize` posées sur les contrôleurs deviennent des commentaires silencieux — Spring les lit mais ne fait rien avec. Un utilisateur simplement authentifié (avec n'importe quel rôle, y compris CITOYEN) pourrait alors appeler des endpoints réservés aux SUPERVISEUR ou ADMINISTRATEUR.

**Le pire** : aucune erreur au démarrage. L'application marche, les tests d'auth passent, tout semble normal. Ça ne se voit que si on tente une intrusion réelle.

---

## Références croisées

- **Configuration YAML** : `signalville-backend/src/main/resources/application.yml`, section `signalville.cors`
- **Filtre JWT** : `signalville-backend/src/main/java/africa/epf/signalville_backend/infrastructure/security/JwtAuthenticationFilter.java`
- **Gestion globale des exceptions** : `signalville-backend/src/main/java/africa/epf/signalville_backend/api/exception/GlobalExceptionHandler.java`
- **Migration seed comptes internes** : `signalville-backend/src/main/resources/db/migration/V5__seed_internal_users.sql`
- **Contrat OpenAPI** : `docs/api/signalville-openapi.yaml`

---

*Document rédigé pendant la relecture guidée de la Séance 2, en préparation de la Séance 3.*
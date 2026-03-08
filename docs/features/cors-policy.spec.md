# Feature Specification: Centralized CORS Policy (`@CrossOrigin` removal)

## 1. Summary

Replace scattered `@CrossOrigin(origins = "*")` annotations across individual
controllers with a single `CorsConfigurationSource` bean in `SecurityConfig`. Allowed
origins are read from a `wordai.cors.allowed-origins` property, defaulting to `*` in
dev and requiring an environment variable in production.

## 2. Problem Statement

`@CrossOrigin(origins = "*")` was previously applied directly to the API layer, including legacy controllers that were later removed during the resource-controller split:

| Controller | Package |
|---|---|
| `WordGameController` | `com.fistraltech.server.controller` |
| `AdminController` | `com.fistraltech.server.controller` |
| `DictionaryController` / `AnalysisController` / `AlgorithmController` / `HistoryController` | `com.fistraltech.server.controller` |
| `UserManagementController` | `com.fistraltech.security.controller` |

Problems with the annotation-per-controller approach:

- **Wildcard + credentials is a security risk.** The app uses session cookies. Any origin
  can make credentialed requests (reading protected endpoints from a malicious foreign
  site).
- **No profile-based control.** The wildcard applies equally in dev and production. There
  is no mechanism to narrow it to a specific domain at deploy time.
- **Scattered configuration.** Three controllers not yet annotated (`GameController`,
  `AuthController`, `HomeController`) silently lack CORS handling. Adding a new
  controller requires remembering to add the annotation.
- **Spring Security bypass risk.** Per-controller `@CrossOrigin` runs *before* Spring
  Security's CORS filter, which can lead to preflight requests bypassing security checks
  in certain configurations.

## 3. Fix Design

| Component | Change |
|---|---|
| `SecurityConfig` | Add `CorsConfigurationSource` bean; wire `.cors()` in filter chain |
| `application.properties` (dev) | `wordai.cors.allowed-origins=*` — dev keeps wildcard for convenience |
| `application-prod.properties` | `wordai.cors.allowed-origins=${WORDAI_CORS_ALLOWED_ORIGINS:http://localhost:8080}` |
| API controllers | Remove `@CrossOrigin(origins = "*")` |

### Why `CorsConfigurationSource` in `SecurityConfig`?

Spring Security processes CORS **before** authentication/authorization when the
`CorsConfigurationSource` bean is registered centrally. This ensures:

1. Preflight `OPTIONS` requests are handled correctly without auth checks.
2. A single place controls the allowed-origins policy for *all* endpoints.
3. The policy can vary between `dev` and `prod` profiles via a property, with no code change.

### Why `setAllowedOriginPatterns` instead of `setAllowedOrigins`?

`setAllowedOrigins(List.of("*"))` combined with `setAllowCredentials(true)` throws an
`IllegalArgumentException` in Spring. `setAllowedOriginPatterns` supports both the
wildcard (`*`) and specific origins alongside `allowCredentials = true`.

### Production default

The app serves both frontend static files and the REST API from the same Spring Boot
instance. Browser requests from `index.html` to `/api/**` are **same-origin** and do
not trigger CORS — so the production default (`http://localhost:8080` if
`WORDAI_CORS_ALLOWED_ORIGINS` is unset) safely satisfies the deployed UI while
restricting external API consumers.

Set `WORDAI_CORS_ALLOWED_ORIGINS=http://your-public-ip:8080` (comma-separated for
multiple origins) at the cloud host to open the API to specific external consumers.

## 4. Acceptance Tests (TDD)

| # | Scenario | Assertion |
|---|---|---|
| T1 | CORS preflight (`OPTIONS`) to a public endpoint from a configured allowed origin | Response status 200; `Access-Control-Allow-Origin` header equals the request origin |
| T2 | CORS preflight from an origin NOT in the allowed list | No `Access-Control-Allow-Origin` header in response |
| T3 | Simple GET request from a configured allowed origin | `Access-Control-Allow-Origin` header equals the request origin |

## 5. Files Changed

| File | Change |
|---|---|
| `src/main/java/com/fistraltech/security/config/SecurityConfig.java` | Add `@Value wordai.cors.allowed-origins`; add `CorsConfigurationSource` bean; wire `.cors()` |
| `application.properties` | Add `wordai.cors.allowed-origins=*` |
| `application-prod.properties` | Add `wordai.cors.allowed-origins=${WORDAI_CORS_ALLOWED_ORIGINS:http://localhost:8080}` |
| `WordGameController.java` and split resource controllers | Remove `@CrossOrigin` |
| `AdminController.java` | Remove `@CrossOrigin` |
| `UserManagementController.java` | Remove `@CrossOrigin` |
| `src/test/java/com/fistraltech/security/config/CorsConfigTest.java` | **New** — TDD tests T1–T3 |
| `docs/IMPLEMENTATION_STATUS.md` | Record completion |

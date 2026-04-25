# Implementation Status

Single source of truth for what is built, what is in progress, and what is planned. Update this file when completing or starting any significant piece of work.

*Last updated: April 2026 — ARCH-24 API version boundary added*

---

## Features

### ✅ Complete

| Feature | Notes |
|---|---|
| Core game engine | `WordGame`, `Dictionary`, `Response`, `Filter`, response codes G/A/X/R |
| Selection strategies | RANDOM, ENTROPY, BELLMAN_FULL_DICTIONARY |
| REST API — game lifecycle | `POST /games`, `POST /games/{id}/guess`, `GET /games/{id}/suggestion`, `DELETE /games/{id}` |
| REST API — dictionaries | `GET /dictionaries`, dictionary options endpoint |
| REST API — analysis and algorithm catalog | `POST /analysis` in `AnalysisController`, `GET /algorithms` in `AlgorithmController` |
| Performance optimisation (all 4 phases) | `ResponseMatrix` (~98% memory saving), `WordIdSet` (~92%), bitmask column length, parallel/lazy computation |
| Spring Security + OAuth2 | Login, user registration, role management (`ROLE_USER`, `ROLE_ADMIN`) |
| Admin endpoints | Session/stats/activity operations plus runtime algorithm policy updates via `AdminController` |
| Game history panel | Session stats + last 5 games in UI |
| Web UI | `index.html`, `game.js`, `style.css` with Play, Auto, Analyse, Dictionary, History screens |
| Help page | `help.html` |
| GitHub Actions CI | `.github/workflows/ci.yml` — runs `npm run lint` plus `mvn clean verify` on Java 25 for push/PR to `main`; `.github/workflows/security.yml` now also runs on dependency-sensitive pull requests |
| Governance documentation baseline | `ARCHITECTURE.md`, `EXECUTION_PLAYBOOK.md`, `API.md`, `GLOSSARY.md`, `STATE_MACHINES.md`, `coding-standards.md` now form the authoritative governance set for the current remediation wave |
| Spec-driven contribution workflow | `specs/TEMPLATE.md`, `CONTRIBUTING.md`, and the refreshed execution/docs indexes now define the minimum ticket, validation, and status-update path for significant changes |
| Staged coverage gate | `pom.xml` now enforces a 60% JaCoCo bundle line-coverage floor during `verify`, ratcheted upward in ARCH-19 and still satisfied after ARCH-22 raised the measured baseline to 72% line coverage |
| API boundary validation | Server request DTOs now carry bean-validation constraints across game, analysis, and challenge flows, and controller-local blank-request checks have been reduced to boundary validation |
| API error contract and explicit failure handling | `ApiErrors`, `ApiExceptionHandler`, and `SecurityApiExceptionHandler` now align handled REST failures across server/security controllers, while session persistence and persisted-session reconstruction failures surface as explicit operational errors instead of silently degrading |
| High-risk server regression coverage | `SessionTrackingServiceTest`, `PlayerGameServiceTest`, and `CustomOAuth2UserServiceTest` now cover three of the highest-risk low-coverage server/security classes, with the later ARCH-20 verify baseline reaching 71% overall line coverage |
| WordEntropy matrix-only execution path | `WordEntropy` now uses the `ResponseMatrix` for cache-miss entropy lookup, keeps only the Bellman-specific external-guess bucket fallback, and no longer carries the mutable `WordGame`/decode helper scaffolding from the removed legacy path |
| GameSession responsibility split | `GameSession` now delegates mutable metadata to `GameSessionMetadata` and filter/strategy/suggestion state to `GameSessionContext`, reducing the session type to a smaller coordinator while preserving the existing public API |
| Entropy memoization for equivalent filter states | Shared `WordEntropy` instances now memoize bounded lazy-entropy best-word results by canonical candidate/target word-set signatures, and filtered `ENTROPY` session suggestions reuse that cache instead of rebuilding per-session analyzers |
| Runtime algorithm policy | Algorithm enablement now persists in the `algorithm_policies` table, `AlgorithmFeatureService` reads/writes the effective state, and admins can update algorithm exposure at runtime through `PUT /api/v1/wordai/admin/algorithms/{algorithmId}` with the legacy admin route retained as a bridge |
| API version boundary | `/api/v1/wordai` is now the primary documented REST root, while the legacy `/api/wordai` routes remain available as a compatibility bridge during migration |
| EntropyKey encapsulation | `EntropyKey` is now an explicitly constructed immutable value object with focused regression coverage for null validation, equality/hashCode, and ordering semantics |
| Architectural fitness tests | `ArchitectureFitnessTest` — layer rules, runtime-boundary rule, and package cycle rule all active |
| Architecture cleanup tranche | Shared DTO/filter moves, legacy runtime removal, controller split, registry-driven algorithm metadata/toggles, and cycle cleanup completed; see `specs/` |
| Dictionaries | 4, 5, 6 letter word files in `src/main/resources/dictionaries/` (7-letter file also present but not configured) |
| Cloud deployment | Oracle Cloud / OCI scripts in `deployment/` |
| Javadoc | Comprehensive on core, bot, server packages (see Documentation section below) |
| **Session TTL (memory-leak fix)** | `WordGameService` now uses a Caffeine cache with 30-min idle TTL and 10 000-session cap. Configurable via `wordai.session.ttl-minutes`. See `docs/features/session-ttl.spec.md`. |
| **Per-session concurrency fix** | `synchronized(session)` block in `WordGameService.makeGuess()`; `synchronized` on `GameSession.suggestWord()` and `setSelectedStrategy()`. Prevents concurrent guess/suggestion/strategy-change races. See `docs/features/session-concurrency.spec.md`. |
| **Admin credentials security hardening** | Removed hardcoded defaults from `DataInitializer.java` `@Value` annotations. `application.properties` now reads from env vars (`WORDAI_ADMIN_EMAIL`, `WORDAI_ADMIN_PASSWORD`) with safe dev fallbacks. `application-prod.properties` requires env vars with no fallback (Spring aborts startup if absent). `AdminCredentialsValidator` adds defence-in-depth: rejects blank or known-default credentials when the `prod` profile is active. See `docs/features/admin-credentials.spec.md`. |
| **Flyway schema migration** | Replaced `ddl-auto=update` in production with `ddl-auto=validate`. Flyway manages all schema changes via versioned migration scripts. `V1__baseline.sql` captures the current three-table schema (`users`, `user_roles`, `player_games`). `baseline-on-migrate=true` protects the existing production database. Dev still uses `ddl-auto=update` for fast iteration. See `docs/features/flyway-schema-migration.spec.md`. |
| **CORS policy centralisation** | Removed `@CrossOrigin(origins="*")` from all 5 controllers. Replaced with a single `CorsConfigurationSource` bean in `SecurityConfig`. Allowed origins are driven by `wordai.cors.allowed-origins`: `*` in dev (backward-compatible), `${WORDAI_CORS_ALLOWED_ORIGINS:http://localhost:8080}` in production. `setAllowedOriginPatterns` is used to support both wildcards and credential-bearing requests. See `docs/features/cors-policy.spec.md`. |
| **Frontend JS modularisation** | `game.js` monolith split into 11 ES modules: `admin.js`, `analytics.js`, `api.js`, `autoplay.js`, `browser-session.js`, `game.js`, `keyboard.js`, `navigation.js`, `player-analysis.js`, `state.js`, `ui.js` |
| **Session persistence across restarts** | `SessionPersistenceService` persists active game sessions to the database via `ActiveGameSessionEntity`. Flyway migrations V2 (`active_game_sessions` table) and V3 (browser session scoping) manage the schema. |
| **Browser session isolation** | Sessions scoped to browser via `browser-session.js` and Flyway V3 migration. Each browser tab/instance gets its own game state. |
| **Anonymous activity tracking** | `ActivityService` tracks game activity for both authenticated and anonymous users. `AdminController` exposes `/activity` endpoint. Flyway V4 adds tracking support. |

### 🔲 Planned / Backlog

| Feature | Priority | Notes |
|---|---|---|
| Javadoc — `util` package | Medium | `Config`, `ConfigManager`, `ConfigFile`, `Timer` — flagged in legacy DOCUMENTATION_STATUS |
| Javadoc — `analysis` package | Medium | `DictionaryAnalytics`, `PlayerAnalyser`, `ComplexityAnalyser`, `Entropy`, `EntropyKey` |
| Javadoc — new server classes | Medium | `ActivityService`, `SessionPersistenceService`, `SessionTrackingService`, `PlayerGameService`, `SessionInfo`, `UserActivityDto` |
| Public interface documentation backlog | Medium | Tracked by `ARCH-25`; folds remaining Javadocs and package-level docs into the active ticket backlog |
| Remaining server coordination split | Medium | Tracked by `ARCH-26` for `WordGameController` and `WordGameService` |
| Test convention cleanup | Low | Tracked by `ARCH-27` |

---

## Known Architectural Debt

The previously documented ArchUnit violations are closed. Active architecture work is now tracked in [specs/README.md](../specs/README.md), with remaining backlog concentrated in documentation polish and longer-horizon product/design work rather than broken package boundaries.

---

## Test Coverage

| Package | Test class(es) | Tests |
|---|---|---|
| `core` | `DictionaryTest`, `WordGameTest` | 26 |
| `core` | `FilterTest`, `DictionaryTest`, `WordGameTest`, `ResponsePatternTest` | core filtering, engine, and response-pattern encoding coverage |
| `bot.selection` | `SelectionAlgoTest`, `SelectBellman*Test` | 20+ |
| `bot` | `WordGamePlayerTest` | 11 |
| `analysis` | `DictionaryAnalyticsTest`, `ResponseMatrixTest`, `WordEntropyLazyTest`, `WordIdSetTest` | matrix/lazy entropy coverage |
| `security` | `UserManagementControllerTest`, `UserServiceTest`, `AdminCredentialsValidatorTest`, `CorsConfigTest`, `CustomOAuth2UserServiceTest`, `AuthControllerTest` | 30+ |
| `util` | `ConfigManagerTest` | 6 |
| `(root)` | `ArchitectureFitnessTest`, `FlywayMigrationTest` | architecture rules active, no documented skipped violations |
| `server` | `SessionTtlTest`, `SessionConcurrencyTest`, `SessionTrackingServiceTest`, `PlayerGameServiceTest`, `SessionPersistenceServiceTest` | targeted session/persistence regression coverage |
| **Total** | | **346 pass, 0 skipped** |

Run the full suite: `mvn clean verify`

Current measured line-coverage baseline: 71.99% bundle line coverage (`target/site/jacoco/index.html`)

Current staged coverage gate: 60% bundle line coverage enforced in Maven during `verify`

---

## Documentation Status

### ✅ Comprehensive Javadoc
`Column`, `ResponseEntry`, `WordSource`, `InvalidWordException`, `InvalidWordLengthException`, `WordGamePlayer`, `ResultHistory`, `DictionaryHistory`, `GameAnalytics`, `Filter`, `FilterCharacters`, `SelectionAlgo`, `SelectRandom`, `SelectMaximumEntropy`, `WordGameService`, `HomeController`, `WordGameController`, `DictionaryController`, `AnalysisController`, `AlgorithmController`, `HistoryController`, `GameSession`, runtime DTOs in `server.dto`, `DictionaryOption` (in `util`), `AnalysisResponse` (in `analysis`), `AnalysisGameResult` (in `analysis`)

### ⚠️ Needs review / minimal
`Response`, `WordGame`, `Dictionary`, `ResponseHelper`

### 🔲 Not yet documented
`Config`, `ConfigManager`, `ConfigFile`, `Timer`, `DictionaryAnalytics`, `PlayerAnalyser`, `ComplexityAnalyser`, `Entropy`, `EntropyKey`

---

## Deferred Planning Files (superseded by this document)

The following root-level files predated this status tracker and are now superseded:
- `CONTROLLER_METHODS_TO_ADD.txt` — superseded by the split resource controllers and current Spring API surface
- `DOCUMENTATION_STATUS.md` (root) — consolidated into the Documentation section above
- `PerformanceOptimisation.md` (root) — full detail preserved in `docs/development/performance-optimization.md`

# Implementation Status

Single source of truth for what is built, what is in progress, and what is planned. Update this file when completing or starting any significant piece of work.

*Last updated: March 2026 — architecture cleanup and fitness coverage completed*

---

## Features

### ✅ Complete

| Feature | Notes |
|---|---|
| Core game engine | `WordGame`, `Dictionary`, `Response`, `Filter`, response codes G/A/X/R |
| Selection strategies | RANDOM, ENTROPY, MOST_COMMON_LETTERS, MINIMISE_COLUMN_LENGTHS, DICTIONARY_REDUCTION, BELLMAN_OPTIMAL, BELLMAN_FULL_DICTIONARY |
| REST API — game lifecycle | `POST /games`, `POST /games/{id}/guess`, `GET /games/{id}/suggestion`, `DELETE /games/{id}` |
| REST API — dictionaries | `GET /dictionaries`, dictionary options endpoint |
| REST API — analysis and algorithm catalog | `POST /analysis` in `AnalysisController`, `GET /algorithms` in `AlgorithmController` |
| Performance optimisation (all 4 phases) | `ResponseMatrix` (~98% memory saving), `WordIdSet` (~92%), bitmask column length, parallel/lazy computation |
| Spring Security + OAuth2 | Login, user registration, role management (`ROLE_USER`, `ROLE_ADMIN`) |
| Admin endpoints | User management via `AdminController` |
| Game history panel | Session stats + last 5 games in UI |
| Web UI | `index.html`, `game.js`, `style.css` with Play, Auto, Analyse, Dictionary, History screens |
| Help page | `help.html` |
| GitHub Actions CI | `.github/workflows/ci.yml` — runs `mvn clean test` on push/PR to `main` |
| Architectural fitness tests | `ArchitectureFitnessTest` — layer rules, runtime-boundary rule, and package cycle rule all active |
| Architecture cleanup tranche | Shared DTO/filter moves, legacy runtime removal, controller split, registry-driven algorithm metadata/toggles, and cycle cleanup completed; see `specs/` |
| Dictionaries | 4, 5, 6 letter word files in `src/main/resources/dictionaries/` |
| Cloud deployment | Oracle Cloud / OCI scripts in `deployment/` |
| Javadoc | Comprehensive on core, bot, server packages (see Documentation section below) |
| **Session TTL (memory-leak fix)** | `WordGameService` now uses a Caffeine cache with 30-min idle TTL and 10 000-session cap. Configurable via `wordai.session.ttl-minutes`. See `docs/features/session-ttl.spec.md`. |
| **Per-session concurrency fix** | `synchronized(session)` block in `WordGameService.makeGuess()`; `synchronized` on `GameSession.suggestWord()` and `setSelectedStrategy()`. Prevents concurrent guess/suggestion/strategy-change races. See `docs/features/session-concurrency.spec.md`. |
| **Admin credentials security hardening** | Removed hardcoded defaults from `DataInitializer.java` `@Value` annotations. `application.properties` now reads from env vars (`WORDAI_ADMIN_EMAIL`, `WORDAI_ADMIN_PASSWORD`) with safe dev fallbacks. `application-prod.properties` requires env vars with no fallback (Spring aborts startup if absent). `AdminCredentialsValidator` adds defence-in-depth: rejects blank or known-default credentials when the `prod` profile is active. See `docs/features/admin-credentials.spec.md`. |
| **Flyway schema migration** | Replaced `ddl-auto=update` in production with `ddl-auto=validate`. Flyway manages all schema changes via versioned migration scripts. `V1__baseline.sql` captures the current three-table schema (`users`, `user_roles`, `player_games`). `baseline-on-migrate=true` protects the existing production database. Dev still uses `ddl-auto=update` for fast iteration. See `docs/features/flyway-schema-migration.spec.md`. |
| **CORS policy centralisation** | Removed `@CrossOrigin(origins="*")` from all 5 controllers. Replaced with a single `CorsConfigurationSource` bean in `SecurityConfig`. Allowed origins are driven by `wordai.cors.allowed-origins`: `*` in dev (backward-compatible), `${WORDAI_CORS_ALLOWED_ORIGINS:http://localhost:8080}` in production. `setAllowedOriginPatterns` is used to support both wildcards and credential-bearing requests. See `docs/features/cors-policy.spec.md`. |

### 🔲 Planned / Backlog

| Feature | Priority | Notes |
|---|---|---|
| Javadoc — `util` package | Medium | `Config`, `ConfigManager`, `ConfigFile`, `Timer` — flagged in legacy DOCUMENTATION_STATUS |
| Javadoc — `analysis` package | Medium | `DictionaryAnalytics`, `PlayerAnalyser`, `ComplexityAnalyser`, `Entropy`, `EntropyKey` |
| `package-info.java` files | Low | One per package to document package-level responsibility |
| CONTRIBUTING.md | Low | Contribution guidelines for open-source readiness |

---

## Known Architectural Debt

The previously documented ArchUnit violations are closed. Active architecture work is now tracked in [specs/README.md](../specs/README.md), with remaining backlog concentrated in documentation polish and longer-horizon product/design work rather than broken package boundaries.

---

## Test Coverage

| Package | Test class(es) | Tests |
|---|---|---|
| `core` | `DictionaryTest`, `WordGameTest` | 26 |
| `core` | `FilterTest`, `DictionaryTest`, `WordGameTest` | core filtering and engine coverage |
| `bot.selection` | `SelectionAlgoTest`, `SelectBellman*Test` | 20+ |
| `bot` | `WordGamePlayerTest` | 11 |
| `analysis` | `DictionaryAnalyticsTest`, `ResponseCacheTest`, `ResponseMatrixTest`, `WordEntropyLazyTest`, `WordIdSetTest` | ~70 |
| `security` | `UserManagementControllerTest`, `UserServiceTest`, `AdminCredentialsValidatorTest`, `CorsConfigTest` | 20+ |
| `util` | `ConfigManagerTest` | 6 |
| `(root)` | `ArchitectureFitnessTest`, `FlywayMigrationTest` | architecture rules active, no documented skipped violations |
| `server` | `SessionTtlTest`, `SessionConcurrencyTest` | 9 |
| **Total** | | **239 pass, 4 skipped** |

Run the full suite: `mvn clean test`

---

## Documentation Status

### ✅ Comprehensive Javadoc
`Column`, `ResponseEntry`, `WordSource`, `InvalidWordException`, `InvalidWordLengthException`, `WordGamePlayer`, `ResultHistory`, `DictionaryHistory`, `GameAnalytics`, `Filter`, `FilterCharacters`, `SelectionAlgo`, `SelectRandom`, `SelectMostCommonLetters`, `SelectMaximumEntropy`, `SelectFixedFirstWord`, `WordGameService`, `HomeController`, `WordGameController`, `DictionaryController`, `AnalysisController`, `AlgorithmController`, `HistoryController`, `GameSession`, runtime DTOs in `server.dto`, `DictionaryOption`, `AnalysisResponse`, `AnalysisGameResult`

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

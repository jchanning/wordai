# Implementation Status

Single source of truth for what is built, what is in progress, and what is planned. Update this file when completing or starting any significant piece of work.

*Last updated: February 2026*

---

## Features

### ✅ Complete

| Feature | Notes |
|---|---|
| Core game engine | `WordGame`, `Dictionary`, `Response`, `Filter`, response codes G/A/X/R |
| Selection strategies | RANDOM, ENTROPY, MOST_COMMON_LETTERS, MINIMISE_COLUMN_LENGTHS, DICTIONARY_REDUCTION, BELLMAN_OPTIMAL, BELLMAN_FULL_DICTIONARY |
| REST API — game lifecycle | `POST /games`, `POST /games/{id}/guess`, `GET /games/{id}/suggestion`, `DELETE /games/{id}` |
| REST API — dictionaries | `GET /dictionaries`, dictionary options endpoint |
| REST API — analysis | `POST /analysis`, `GET /algorithms` (implemented in `AnalyticsController`) |
| Performance optimisation (all 4 phases) | `ResponseMatrix` (~98% memory saving), `WordIdSet` (~92%), bitmask column length, parallel/lazy computation |
| Spring Security + OAuth2 | Login, user registration, role management (`ROLE_USER`, `ROLE_ADMIN`) |
| Admin endpoints | User management via `AdminController` |
| User statistics | `UserStatsController` |
| Game history panel | Session stats + last 5 games in UI |
| Web UI | `index.html`, `game.js`, `style.css` with Play, Auto, Analyse, Dictionary, History screens |
| Help page | `help.html` |
| GitHub Actions CI | `.github/workflows/ci.yml` — runs `mvn clean test` on push/PR to `main` |
| Architectural fitness tests | `ArchitectureFitnessTest` — ArchUnit rules, 2 active + 4 documented violations |
| Dictionaries | 4, 5, 6 letter word files in `src/main/resources/dictionaries/` |
| Cloud deployment | Oracle Cloud / OCI scripts in `deployment/` |
| Javadoc | Comprehensive on core, bot, server packages (see Documentation section below) |

### 🔲 Planned / Backlog

| Feature | Priority | Notes |
|---|---|---|
| Fix architectural violations | High | Move `DictionaryOption` out of `server.dto`; enables 3 disabled fitness tests — see [ARCHITECTURE.md](ARCHITECTURE.md) |
| Break `core` ↔ `bot` cycle | High | `Dictionary` → `FilterCharacters` circular dep; enables 1 disabled fitness test |
| Javadoc — `util` package | Medium | `Config`, `ConfigManager`, `ConfigFile`, `Timer` — flagged in legacy DOCUMENTATION_STATUS |
| Javadoc — `analysis` package | Medium | `DictionaryAnalytics`, `PlayerAnalyser`, `ComplexityAnalyser`, `Entropy`, `EntropyKey` |
| `package-info.java` files | Low | One per package to document package-level responsibility |
| CONTRIBUTING.md | Low | Contribution guidelines for open-source readiness |

---

## Known Architectural Debt

Tracked with `@Disabled` tests in `ArchitectureFitnessTest`. Each item below is a self-contained refactoring task.

| Debt | Classes involved | Refactor action | Fitness test unlocked |
|---|---|---|---|
| `core` imports `server.dto` | `DictionaryManager` → `DictionaryOption` | Move `DictionaryOption` to `com.fistraltech.core` | `core_mustNotDependOn_server` |
| `util` imports `server.dto` | `Config`, `ConfigManager` → `DictionaryOption` | Same move as above | `util_mustNotDependOn_server` |
| `analysis` imports `server.dto` | `PlayerAnalyser` → `AnalysisGameResult`, `AnalysisResponse` | Move DTOs to `com.fistraltech.analysis` | `analysis_mustNotDependOn_server` |
| `core` ↔ `bot` cycle | `Dictionary` → `FilterCharacters`; `ResponseHelper` → `Filter` | Extract interface in `core`; `bot` implements it | `noCyclicPackageDependencies` |

Use [refactor.prompt.md](../.github/prompts/refactor.prompt.md) to action any of these.

---

## Test Coverage

| Package | Test class(es) | Tests |
|---|---|---|
| `core` | `DictionaryTest`, `WordGameTest` | 26 |
| `bot.filter` | `FilterTest` | — |
| `bot.selection` | `SelectionAlgoTest`, `SelectBellman*Test` | 20+ |
| `bot` | `WordGamePlayerTest` | 11 |
| `analysis` | `DictionaryAnalyticsTest`, `ResponseCacheTest`, `ResponseMatrixTest`, `WordEntropyLazyTest`, `WordIdSetTest` | ~70 |
| `game` | `GameControllerTest` | 12 |
| `security` | `UserManagementControllerTest`, `UserServiceTest` | 20+ |
| `util` | `ConfigManagerTest` | 6 |
| `(root)` | `ArchitectureFitnessTest` | 6 (4 skipped — known violations) |
| **Total** | | **219 pass, 4 skipped** |

Run the full suite: `mvn clean test`

---

## Documentation Status

### ✅ Comprehensive Javadoc
`Column`, `ResponseEntry`, `WordSource`, `InvalidWordException`, `InvalidWordLengthException`, `WordGamePlayer`, `ResultHistory`, `DictionaryHistory`, `GameAnalytics`, `Filter`, `FilterCharacters`, `SelectionAlgo`, `SelectRandom`, `SelectMostCommonLetters`, `SelectMaximumEntropy`, `SelectFixedFirstWord`, `WordGameService`, `HomeController`, `WordGameController`, `GameSession`, all DTOs in `server.dto`

### ⚠️ Needs review / minimal
`Response`, `WordGame`, `Dictionary`, `ResponseHelper`

### 🔲 Not yet documented
`Config`, `ConfigManager`, `ConfigFile`, `Timer`, `DictionaryAnalytics`, `PlayerAnalyser`, `ComplexityAnalyser`, `Entropy`, `EntropyKey`

---

## Deferred Planning Files (superseded by this document)

The following root-level files predated this status tracker and are now superseded:
- `CONTROLLER_METHODS_TO_ADD.txt` — methods were implemented in `AnalyticsController`
- `DOCUMENTATION_STATUS.md` (root) — consolidated into the Documentation section above
- `PerformanceOptimisation.md` (root) — full detail preserved in `docs/development/performance-optimization.md`

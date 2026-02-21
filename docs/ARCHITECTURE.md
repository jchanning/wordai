# WordAI Architecture

This document is the single source of truth for architectural decisions. It is the reference every AI session and every developer should consult before making structural changes.

---

## System Overview

WordAI is a Wordle-like game simulation and analysis system. It provides:
- An interactive web UI for playing the game
- A bot that plays automatically using configurable strategies
- Analytics over full-dictionary runs to compare strategy performance
- A REST API to drive both the UI and automated analysis

**Stack:** Java 21 · Spring Boot 3.4 · H2 (dev) / file-based H2 (prod) · Spring Security · Maven

---

## Package Layer Model

```
┌─────────────────────────────────────────────────────┐
│  server   (HTTP, Spring controllers, DTOs, security) │
├─────────────────────────────────────────────────────┤
│  analysis  (ResponseMatrix, WordEntropy, analytics)  │
├─────────────────────────────────────────────────────┤
│  bot  (WordGamePlayer, SelectionAlgo strategies,     │
│        Filter, DictionaryHistory, GameAnalytics)     │
├─────────────────────────────────────────────────────┤
│  core  (WordGame, Dictionary, Response, Column,      │
│         DictionaryManager, WordSource)               │
├─────────────────────────────────────────────────────┤
│  util  (Config, ConfigManager, Timer)                │
└─────────────────────────────────────────────────────┘
```

**Rule:** Each layer may only import from layers below it. `server` may import any layer. `util` may import nothing from this project.

### Currently enforced (via `ArchitectureFitnessTest`)
- `bot` does not import from `server` ✅
- `game` does not import from `server` ✅

### Known violations (documented in `ArchitectureFitnessTest` as `@Disabled`)
| Violation | Root cause | Fix |
|---|---|---|
| `core` imports `server.dto` | `DictionaryManager` uses `DictionaryOption` | Move `DictionaryOption` to `core` |
| `util` imports `server.dto` | `Config`/`ConfigManager` use `DictionaryOption` | Move `DictionaryOption` to `core` or `util` |
| `analysis` imports `server.dto` | `PlayerAnalyser` uses `AnalysisGameResult`/`AnalysisResponse` | Move those DTOs to `analysis` |
| `core` ↔ `bot` cycle | `Dictionary` → `FilterCharacters`; `ResponseHelper` → `Filter` | Extract interface in `core`; `bot` implements |

Do not introduce new cross-layer imports. Each violation above is a refactoring task — see [refactor.prompt.md](../.github/prompts/refactor.prompt.md).

---

## Primary Data Flow

```
Config / ConfigManager
       │
       ▼
DictionaryManager  ──load once──►  Dictionary (per word length)
                                        │
                                        ▼
                              WordGame.evaluate(guess, target)
                                        │ Response (G/A/X/R per position)
                                        ▼
                              SelectionAlgo.selectWord(response)
                                   │ uses Filter to prune Dictionary
                                   │ uses WordEntropy / ResponseMatrix
                                        │
                                        ▼
                              WordGamePlayer.playGame()
                                        │ ResultHistory / DictionaryHistory
                                        ▼
                              GameSession  (server.model)
                                        │
                                        ▼
                              WordGameController  (REST API)
                                        │ JSON
                                        ▼
                              browser / UI (index.html + game.js)
```

---

## Response Code Semantics

Every guess produces one code per letter position:

| Code | Meaning | Filter action |
|---|---|---|
| `G` | Correct letter, correct position | Fix letter at that position; exclude all others there |
| `A` | Correct letter, wrong position | Remove from guessed position; add to must-contain |
| `X` | Letter present but count exceeded | Treat like `A`; track maximum occurrence count |
| `R` | Letter absent from word | Remove from all positions |

Encoded as a base-4 integer: `G=0, A=1, R=2, X=3`. For a 5-letter word, patterns fit in a `short` (range 0–1023).

---

## Selection Strategy Registration

All strategies must be registered in `AlgorithmFeatureService` to be addressable by the UI and API. Strategies extend `SelectionAlgo` and override:

```java
String selectWord(Response lastResponse, Dictionary dictionary)
```

**Registered strategies:**

| API key | Class | Description |
|---|---|---|
| `RANDOM` | `SelectRandom` | Uniform random from valid candidates — baseline |
| `ENTROPY` | `SelectMaximumEntropy` | Maximises information gain (Shannon entropy) |
| `MOST_COMMON_LETTERS` | `SelectMostCommonLetters` | Prefers words covering high-frequency letters |
| `MINIMISE_COLUMN_LENGTHS` | `SelectMinimiseColumnLengths` | Minimises expected letter options per position |
| `DICTIONARY_REDUCTION` | `SelectMaximumDictionaryReduction` | Maximises expected reduction in remaining words |
| `BELLMAN_OPTIMAL` | `SelectBellman*` | Bellman-optimal strategy (single dictionary subset) |
| `BELLMAN_FULL_DICTIONARY` | `SelectBellmanFullDictionary` | Bellman-optimal across full dictionary |

To add a new strategy, see [new-selection-algo.prompt.md](../.github/prompts/new-selection-algo.prompt.md).

---

## Performance Architecture

Four completed optimisation phases replaced naive string/HashMap approaches:

| Component | Key class | Technique | Memory saving |
|---|---|---|---|
| Response lookup | `ResponseMatrix` | Pre-computed `short[][]` matrix | ~98% |
| Word tracking | `WordIdSet` | `int[]` IDs vs `Set<String>` | ~92% |
| Column length | `ResponseMatrix` | Bitmask (26-bit int) vs `HashSet<Character>` | ~99% |
| Entropy (large dict) | `WordEntropy` | `IntStream.parallel()` above threshold | 2–4× throughput |

**Key invariant:** `ResponseMatrix` is built once per `Dictionary` and cached via `DictionaryManager`. It must not be reconstructed per game or per guess.

**Lazy threshold:** Selection algorithms switch between cached pre-computed values and lazy per-call computation at `LAZY_THRESHOLD = 0.8` (use cache when >80% of dictionary still valid).

Detailed notes: [docs/development/performance-optimization.md](development/performance-optimization.md).

---

## Configuration

| Source | Loaded by | Purpose |
|---|---|---|
| `src/main/resources/application.properties` | Spring Boot | Default config including dictionary paths, word lengths 4–6 |
| `src/main/resources/application-prod.properties` | Spring Boot (`prod` profile) | Oracle Cloud production overrides |
| `wordai.properties` (repo root, gitignored) | `ConfigManager` | Local developer overrides |
| `deployment/wordai.properties` | `ConfigManager` on server | Cloud server overrides |

`Config` is the single config value object. `ConfigManager` resolves and populates it. Word lengths supported: **4, 5, 6** (7-letter option intentionally removed).

---

## Invariants

These must hold at all times. Tests or fitness functions enforce them where possible.

1. **Word length is immutable per game.** `Dictionary`, `Filter`, and `SelectionAlgo` are all constructed with a fixed word length. Never mix lengths within a game session.
2. **`Filter` is not thread-safe.** Each `SelectionAlgo` instance owns its own `Filter`. Never share across threads.
3. **One `WordGame` per evaluation.** `WordGame.evaluate(guess, target)` is stateless; call it freely. Do not share a single `WordGame` instance across concurrent calls.
4. **`DictionaryManager` is a singleton.** Dictionaries are loaded once at startup via `DictionaryManager.initialise(config)`. Never call `initialise` again after startup.
5. **`ResponseMatrix` is immutable after construction.** It is safe to share across threads; do not mutate after the constructor returns.
6. **Selection strategies must fall back to `dictionary.selectRandomWord()` on an empty filtered dictionary.** This prevents `NullPointerException` on edge-case game states.

---

## Key Non-Goals

- No multiplayer / real-time features.
- No persistent game state between server restarts (game sessions are in-memory only).
- No mobile-native apps — web UI only.
- No support for word lengths outside 4–6 letters.
- No external ML model integration — all strategies are deterministic algorithms.

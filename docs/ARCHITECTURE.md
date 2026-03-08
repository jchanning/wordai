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
│  server   (HTTP API, Spring controllers, DTOs,       │
│            security, algorithm registry/feature policy) │
├─────────────────────────────────────────────────────┤
│  analysis  (analytics, reporting, dictionary studies)│
├─────────────────────────────────────────────────────┤
│  bot  (WordGamePlayer, SelectionAlgo strategies,     │
│        simulation orchestration, CSV analytics)      │
├─────────────────────────────────────────────────────┤
│  core  (WordGame, Dictionary, Response, Filter,      │
│         WordEntropy, ResponseMatrix, histories)      │
├─────────────────────────────────────────────────────┤
│  util  (Config, ConfigManager, DictionaryOption,     │
│         Timer)                                       │
└─────────────────────────────────────────────────────┘
```

**Rule:** Each layer may only import from layers below it. `server` may import any layer. `util` may import nothing from this project.

### Enforced via `ArchitectureFitnessTest`
- `bot` does not import from `server`
- `game` does not import from `server` (legacy package allowed to remain empty)
- `core` does not import from `server`
- `util` does not import from `server`
- `analysis` does not import from `server`
- `core` does not import from `bot`
- `server` runtime config access is centralised in `DictionaryService`
- `com.fistraltech` packages are cycle-free

Do not introduce new cross-layer imports. Architectural cleanup work is tracked in [specs/README.md](../specs/README.md).

---

## Primary Data Flow

```
Config / ConfigManager
       │
       ▼
DictionaryService  ──load once──►  Dictionary / WordEntropy caches
       │                                │
       │                                ▼
       │                        GameSession clone per game
       │                                │
       ▼                                ▼
WordGameService  ───────────────► WordGame.evaluate(guess, target)
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
             WordGameController / DictionaryController / AnalysisController
                           / AlgorithmController / HistoryController
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

API-visible strategies are defined by `AlgorithmDescriptor` implementations, discovered by `AlgorithmRegistry`, and filtered for exposure by `AlgorithmFeatureService`. Strategies still extend `SelectionAlgo` and override:

```java
String selectWord(Response lastResponse, Dictionary dictionary)
```

`AlgorithmRegistry` is the canonical place for algorithm ID normalisation and descriptor lookup. `AlgorithmFeatureService` adds environment-driven enablement policy on top of that registry metadata.

**Currently exposed API strategies:**

| API key | Class | Description |
|---|---|---|
| `RANDOM` | `SelectRandom` | Uniform random from valid candidates |
| `ENTROPY` | `SelectMaximumEntropy` | Maximises information gain (Shannon entropy) |
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

**Key invariant:** `ResponseMatrix` is built once per loaded dictionary and cached behind `DictionaryService`. It must not be reconstructed per game or per guess.

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
4. **`DictionaryService` is the single runtime dictionary boundary.** Server code must obtain loaded dictionaries and cached entropy through `DictionaryService`, not through ad hoc `ConfigManager` access.
5. **`ResponseMatrix` is immutable after construction.** It is safe to share across threads; do not mutate after the constructor returns.
6. **Selection strategies must fall back to `dictionary.selectRandomWord()` on an empty filtered dictionary.** This prevents `NullPointerException` on edge-case game states.

---

## Key Non-Goals

- No multiplayer / real-time features.
- No persistent game state between server restarts (game sessions are in-memory only).
- No mobile-native apps — web UI only.
- No support for word lengths outside 4–6 letters.
- No external ML model integration — all strategies are deterministic algorithms.

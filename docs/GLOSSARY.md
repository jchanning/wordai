# Glossary

This glossary is the semantic source of truth for recurring WordAI terms used across architecture tickets, server code, and governance docs.

| Term | Definition |
|---|---|
| `WordGame` | Core engine object that owns the target word and evaluates guesses into response codes. |
| `Response` | Per-guess evaluation result returned by the core engine; exposes winner state, remaining word counts, and per-letter statuses. |
| `Dictionary` | Fixed-length set of candidate words together with column and frequency metadata. |
| `Dictionary option` | API-visible descriptor for one loadable dictionary choice, identified by a stable `dictionaryId`. |
| `DictionaryService` | Server-side boundary that resolves dictionary options, loaded dictionaries, and shared `WordEntropy` caches. |
| `ResponseMatrix` | Immutable precomputed matrix of response codes for word pairs in a loaded dictionary. |
| `WordEntropy` | Entropy calculator built on top of the response matrix for algorithm suggestion and analysis flows. |
| `SelectionAlgo` | Strategy base type used by bots and API suggestion flows to choose the next guess. |
| `AlgorithmDescriptor` | Metadata object that names, documents, and classifies one strategy for API exposure. |
| `AlgorithmRegistry` | Canonical lookup and normalization boundary for algorithm descriptors. |
| `AlgorithmFeatureService` | Policy layer that decides which registered algorithms are enabled for API consumers. |
| `GameSession` | In-memory representation of one active single-word game. It binds `WordGame`, `Filter`, dictionary context, selected strategy, and browser/user ownership fields. |
| `Browser session ID` | UI-generated identifier used to keep resumable sessions isolated per browser window or tab. |
| `Active game session` | Persisted `active_game_sessions` row for an authenticated in-progress game. Its database `status` is currently `ACTIVE` while the game remains resumable. |
| `Persisted game history` | Completed player game record stored after a win or loss. History DTOs expose `WON` or `LOST` as the finished status. |
| `ChallengeSession` | In-memory challenge-mode aggregate representing a 10-puzzle timed run. |
| `Puzzle summary` | Completed challenge puzzle record containing puzzle number, target word, completion status, score, attempts used, and elapsed time. |
| `Session tracking` | Admin-focused monitoring view over authenticated web sessions, current game association, and recent browser metadata. |
| `Architecture fitness test` | ArchUnit-based rule set that enforces package boundaries and selected runtime invariants. |

## Boundary Notes

- `GameSession` and `ChallengeSession` are coordination objects, not HTTP DTOs.
- `Dictionary option` is shared domain/configuration metadata and must not move back into the HTTP DTO layer.
- `DictionaryService` is the only supported runtime entry point for server code that needs dictionary metadata or shared entropy caches.
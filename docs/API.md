# API Sketch

This document is the human-readable API sketch for WordAI. It complements the generated OpenAPI output at `/swagger-ui.html` and focuses on stable resource boundaries, request intent, and the main DTOs used by the UI.

## Base Paths

- Public application routes: `/`, `/privacy`, `/terms`, `/cookies`, `/terms-sale`
- Primary REST API root: `/api/v1/wordai`
- Compatibility bridge: `/api/wordai` remains supported for the current client surface while callers migrate to `/api/v1/wordai`

## Main Resources

### Service Health

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/wordai/health` | Liveness plus active-session count |

### Games

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/wordai/games` | Create a new game or resume an existing browser-scoped game |
| `POST` | `/api/v1/wordai/games/{gameId}/guess` | Submit one guess and receive letter-by-letter feedback |
| `GET` | `/api/v1/wordai/games/{gameId}` | Read the current game state |
| `DELETE` | `/api/v1/wordai/games/{gameId}` | Remove the active game session |
| `GET` | `/api/v1/wordai/games/{gameId}/words` | Inspect remaining candidate words |
| `GET` | `/api/v1/wordai/games/{gameId}/suggestion` | Ask the selected algorithm for the next guess |
| `PUT` | `/api/v1/wordai/games/{gameId}/strategy` | Change the active suggestion strategy |

`POST /api/v1/wordai/games` accepts `CreateGameRequest`, with optional `dictionaryId`, `wordLength`, `targetWord`, `browserSessionId`, and `resumeExisting`.

`POST /api/v1/wordai/games/{gameId}/guess` returns `GameResponse`, including:
- `gameWon`
- `gameOver`
- `attemptNumber`
- `maxAttempts`
- `results[]` with letter/status pairs
- `remainingWordsCount`
- `message`

### Dictionaries

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/wordai/dictionaries` | List available dictionary options |
| `GET` | `/api/v1/wordai/dictionaries/{dictionaryId}` | Read one dictionary option and metadata |

### Algorithms

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/wordai/algorithms` | List API-exposed algorithms after feature-policy filtering |

### Analysis

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/wordai/analysis` | Run a batch analysis job against a dictionary/strategy combination |

### History

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/wordai/history` | Read completed game history for the authenticated user |

### Admin

| Method | Route | Purpose |
|---|---|---|
| `GET` | `/api/v1/wordai/admin/sessions` | List active tracked sessions |
| `GET` | `/api/v1/wordai/admin/stats` | Read admin summary metrics |
| `POST` | `/api/v1/wordai/admin/cleanup-sessions` | Trigger stale-session cleanup |
| `GET` | `/api/v1/wordai/admin/activity` | Read player and anonymous activity summaries |
| `PUT` | `/api/v1/wordai/admin/algorithms/{algorithmId}` | Update runtime algorithm exposure policy |

### Challenge Mode

| Method | Route | Purpose |
|---|---|---|
| `POST` | `/api/v1/wordai/challenges` | Start a 10-puzzle challenge run |
| `GET` | `/api/v1/wordai/challenges/{challengeId}` | Read current challenge state |
| `POST` | `/api/v1/wordai/challenges/{challengeId}/guess` | Submit a guess for the active puzzle |
| `POST` | `/api/v1/wordai/challenges/{challengeId}/assist` | Use one assist on the active puzzle |
| `POST` | `/api/v1/wordai/challenges/{challengeId}/pause` | Use the one-time pause extension |
| `POST` | `/api/v1/wordai/challenges/{challengeId}/skip` | Skip the active puzzle with a score penalty |
| `GET` | `/api/v1/wordai/challenges/leaderboard` | Read persisted challenge leaderboard results |

`GET /api/v1/wordai/challenges/{challengeId}` returns `ChallengeStateResponse`, including:
- overall `status`
- `currentPuzzleNumber`
- `puzzlesCompleted`
- `totalScore`
- `secondsRemaining`
- `currentPuzzleAssistsRemaining`
- `pauseUsed` and `skipUsed`
- the `lastGuess` summary for the current puzzle

## Authentication Expectations

- Game creation and play support guest access.
- History, leaderboard persistence, and admin data depend on the authenticated user context.
- Admin routes are operational endpoints and should be treated as privileged surfaces.
- The legacy `/api/wordai` routes remain available as a temporary compatibility bridge for existing clients.

## Response Semantics

- Game letter feedback uses `G`, `A`, `R`, and `X` semantics defined in [ARCHITECTURE.md](./ARCHITECTURE.md).
- The server package conventions prefer explicit `ResponseEntity` responses and an `{ "error", "message" }` error map for handled failures.
- Error handling is being standardized further in ARCH-18; this sketch records the current stable resource layout rather than every controller-specific edge case.
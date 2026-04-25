# State Machines

This document records the lifecycle state transitions that are currently present in WordAI. Some of these flows are represented by explicit `status` fields in persisted models; others are logical runtime states derived from controller and service behavior.

## Single-Game Lifecycle

### Logical States

| State | Meaning |
|---|---|
| `REQUESTED` | A create-game request has been received but no in-memory session exists yet. |
| `ACTIVE` | A `GameSession` exists in memory and the player can guess, inspect state, or request suggestions. |
| `WON` | The player guessed the target word before the attempt limit. |
| `LOST` | The player exhausted the allowed attempts without solving the word. |
| `DELETED` | The active session was explicitly removed. |

### Transitions

| From | Event | To | Notes |
|---|---|---|---|
| `REQUESTED` | `POST /api/v1/wordai/games` succeeds | `ACTIVE` | Creates a `GameSession`; authenticated users may also create an `active_game_sessions` row with database status `ACTIVE`. |
| `ACTIVE` | Non-winning guess below max attempts | `ACTIVE` | Session stays live; filter and remaining-word state are updated. |
| `ACTIVE` | Winning guess | `WON` | Completed game is persisted to history and the active resumable session is retired. |
| `ACTIVE` | Guess reaches max attempts without solving | `LOST` | Completed game is persisted to history and the active resumable session is retired. |
| `ACTIVE` | `DELETE /api/v1/wordai/games/{gameId}` | `DELETED` | Session is removed without creating a completed-history result. |

## Persisted Active Game Row

The `active_game_sessions` table currently stores only one explicit persisted state:

| Persisted status | Meaning |
|---|---|
| `ACTIVE` | Authenticated game is resumable and still in progress. |

The row is updated after guesses and removed when the game completes, is replaced, or is explicitly deleted.

## Challenge Lifecycle

### Top-Level Challenge Status

| Status | Meaning |
|---|---|
| `ACTIVE` | Challenge is live and its current puzzle can accept actions. |
| `COMPLETED` | All 10 puzzles have been completed successfully or by allowed completion actions. |
| `FAILED_TIMEOUT` | The challenge expired because the current puzzle deadline was exceeded. |

`pauseUsed` and `skipUsed` are flags, not separate top-level states. Using pause or skip keeps the challenge in `ACTIVE` unless the action also ends the final puzzle and completes the challenge.

### Top-Level Transitions

| From | Event | To | Notes |
|---|---|---|---|
| `REQUESTED` | `POST /api/v1/wordai/challenges` succeeds | `ACTIVE` | Creates a `ChallengeSession` and starts puzzle 1. |
| `ACTIVE` | Solve or skip a non-final puzzle | `ACTIVE` | Records a puzzle summary, advances to the next puzzle, resets timers and assist allowance. |
| `ACTIVE` | Solve or skip the final puzzle | `COMPLETED` | Records the final puzzle and persists leaderboard data for authenticated users. |
| `ACTIVE` | Current puzzle exceeds deadline | `FAILED_TIMEOUT` | Recorded as a failed challenge and persisted when eligible. |

## Challenge Puzzle Outcome Status

Completed challenge puzzles record one of these summary outcomes:

| Puzzle outcome | Meaning |
|---|---|
| `SOLVED` | Puzzle completed successfully within attempts and time. |
| `FAILED_ATTEMPTS` | Puzzle exhausted the per-puzzle attempt limit. |
| `SKIPPED` | Player used the skip action and accepted the score penalty. |
| `FAILED_TIMEOUT` | Puzzle timer expired before completion. |

`FAILED_ATTEMPTS` is a puzzle-summary outcome, not a top-level challenge status.
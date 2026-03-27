## Challenge Mode

## Goal

Add a new play mode called Challenge Mode where a player attempts a fixed sequence of 10 puzzles in one challenge session, with score, timing, assists, pause/skip rules, and leaderboard support.

## Summary

- A challenge contains 10 puzzles selected at random from the chosen dictionary.
- Each puzzle uses a unique target word within that challenge.
- Puzzle 1 starts with 60 seconds, 6 attempts, and 3 AI assists.
- Time limit decreases by 5 seconds after each completed puzzle, with a floor of 10 seconds.
- AI assists decrease by 1 after every 3 completed puzzles, with a floor of 1 assist.
- A player may pause once per challenge for 10 seconds.
- A player may skip one puzzle per challenge with a 20-point penalty.
- A challenge has a unique challenge ID and tracks cumulative progress and score.
- At the end of the challenge, the player sees the total score and a leaderboard.

## Architecture Fit

Challenge Mode should be implemented as a server-layer orchestration feature that reuses the existing single-puzzle game engine.

- `core` remains responsible for word evaluation, responses, filtering, and entropy.
- `bot` remains responsible for selection algorithms and suggestions.
- `server` owns challenge lifecycle, scoring, persistence, API contracts, and recovery.
- `DictionaryService` remains the single runtime dictionary boundary.

Challenge-specific state must not be pushed into lower layers. The existing `GameSession` model represents one puzzle; Challenge Mode should compose puzzle sessions rather than turning `GameSession` into a multi-puzzle aggregate.

## Design Principles

- Reuse the existing word game flow instead of building a second rules engine.
- Keep timer and score decisions server-authoritative.
- Treat Challenge Mode as additive; do not regress the existing single-game flow.
- Persist active challenge state separately from completed challenge results.
- Follow existing controller, DTO, and test conventions.

## Assumptions To Confirm

These points should be confirmed before coding because they materially affect API and persistence design.

1. Failing a puzzle by time or attempts ends the full challenge immediately.
2. AI assists are per puzzle, not one pooled balance for the full challenge.
3. Pause is allowed once per challenge, not once per puzzle.
4. Skip is allowed once per challenge, not once per puzzle.
5. Leaderboard entries are for authenticated users only.
6. Score is capped at 100 per solved puzzle before penalties and bonuses are applied.

If any of these are not correct, update this plan before implementation starts.

## Proposed Server Design

### New Components

- `ChallengeService`
	Orchestrates challenge creation, guess submission, timer validation, assist usage, skip/pause logic, score calculation, puzzle progression, and challenge completion.

- `ChallengeSession`
	In-memory aggregate representing one active challenge. Holds challenge-wide state and current puzzle state.

- `ChallengeController`
	REST API entry point under `/api/wordai/challenges`.

- Challenge DTOs in `com.fistraltech.server.dto`
	Request and response contracts for challenge creation, challenge state, guesses, assists, pause, skip, and leaderboard responses.

- Persistence entities and repositories
	One for active challenge state and one for completed challenge results.

### ChallengeSession State

The active challenge model should include at minimum:

- `challengeId`
- `userId`
- `browserSessionId`
- `dictionaryId`
- `currentPuzzleIndex`
- `totalScore`
- `challengeStatus`
- `pauseUsed`
- `skipUsed`
- `currentPuzzleTargetWord`
- `currentPuzzleAttemptsUsed`
- `currentPuzzleMaxAttempts`
- `currentPuzzleDeadline`
- `currentPuzzleAssistsRemaining`
- `completedPuzzleSummaries`

### Persistence Model

Add new Flyway migrations and corresponding JPA models.

- `active_challenge_sessions`
	Stores resumable active challenge state.

- `challenge_results`
	Stores completed challenge summaries for leaderboard/history.

- Optional: `challenge_puzzle_results`
	Stores one row per puzzle if detailed reporting or replay is needed.

Do not overload the existing single-game persistence tables for challenge runs.

## Proposed API Breakdown

### Core Endpoints

- `POST /api/wordai/challenges`
	Starts a new challenge and returns the initial challenge state.

- `GET /api/wordai/challenges/{challengeId}`
	Returns the current challenge state, including current puzzle, score, timer state, and remaining assists.

- `POST /api/wordai/challenges/{challengeId}/guess`
	Submits a guess for the current puzzle and returns updated challenge state.

- `POST /api/wordai/challenges/{challengeId}/assist`
	Consumes one assist and returns a suggestion or hint plus updated assist count.

- `POST /api/wordai/challenges/{challengeId}/pause`
	Applies the one allowed 10-second pause.

- `POST /api/wordai/challenges/{challengeId}/skip`
	Skips the current puzzle, applies the score penalty, and advances challenge state.

- `GET /api/wordai/challenges/leaderboard`
	Returns ranked completed challenge results.

### Controller Rules

Implementation must follow the project API conventions:

- `@RestController` and explicit base path.
- Constructor injection only.
- `ResponseEntity<T>` for all endpoints.
- `Logger` for key events and error conditions.
- Explicit error payloads with `error` and `message` fields.

## Front-End Breakdown

### New UI Module

- `challenge.js`
	Orchestrates the challenge flow in the browser: start challenge, show countdown, submit guesses, trigger assist/pause/skip actions, render score, and show completion state.

### Existing Modules To Extend

- `api.js`
	Add challenge endpoint functions.

- `state.js`
	Add a `challenge` state subtree.

- `ui.js`
	Add challenge timer, score, pause/skip controls, assist count, and challenge summary rendering.

- `navigation.js`
	Add route or screen activation for Challenge Mode.

- `index.html`
	Add Challenge Mode screen, controls, and leaderboard display.

### UI Behavior Requirements

- Timer shown to user must be derived from server state.
- Pause and skip controls must disable once consumed.
- Assist count must update immediately after use.
- Puzzle progression must be explicit after solve, skip, or fail.
- Final challenge screen must show total score and leaderboard results.

## Scoring Breakdown

The scoring formula needs to be specified before implementation. The current feature brief is not precise enough to code safely.

The implementation plan assumes a deterministic score calculator with:

- maximum 100 points per solved puzzle
- penalties for higher attempt usage
- penalties for higher time usage
- fixed 20-point penalty for skip
- zero points for a failed puzzle if the challenge ends on failure

Add the exact formula before starting coding so unit tests can lock it down.

## Delivery Phases

### Phase 1: Rules And Domain Model

- Confirm unresolved product assumptions.
- Define challenge statuses and invariants.
- Add challenge rules model and score calculator.
- Add `ChallengeSession` and `ChallengeService` skeleton.

### Phase 2: Persistence

- Add Flyway migration for challenge tables.
- Add JPA entities and repositories.
- Add persistence mapping for active and completed challenge sessions.

### Phase 3: API

- Add challenge DTOs.
- Implement `ChallengeController`.
- Integrate auth/user ownership and browser session tracking.
- Add leaderboard endpoint.

### Phase 4: Front End

- Add challenge UI screen and navigation entry.
- Add challenge API wiring.
- Add timer, score, assists, pause, skip, and completion UX.

### Phase 5: Validation

- Run service, controller, and repository tests.
- Run full regression suite.
- Perform manual UI validation for countdown and progression behavior.

## Testing Breakdown

Testing must follow the repository testing rules and use JUnit 5.

### Unit Tests

Add pure unit tests for challenge rules and scoring.

- time limit progression from puzzle 1 through puzzle 10
- assist reduction after every 3 completed puzzles
- time floor at 10 seconds
- assist floor at 1
- skip penalty application
- pause allowed once only
- skip allowed once only
- score calculation edge cases and caps

### Service Tests

Add Mockito-based service tests for `ChallengeService`.

- start challenge creates valid initial state
- challenge picks 10 unique target words
- guess submission advances solved puzzle to next puzzle
- timeout prevents further guesses for that puzzle
- assist consumption decrements remaining assists
- second pause is rejected
- second skip is rejected
- challenge completion persists final result

### Controller Tests

Add `@WebMvcTest` coverage for `ChallengeController`.

- create challenge returns initial state
- guess endpoint returns updated challenge snapshot
- assist endpoint enforces remaining assists
- pause endpoint enforces one-use rule
- skip endpoint applies penalty and advances state
- leaderboard endpoint returns sorted results

### Repository Tests

Add `@DataJpaTest` coverage for challenge persistence.

- active challenge save/load
- completed challenge result save/load
- leaderboard ordering
- optional per-user challenge history queries

### Regression Tests

- existing single-game create/guess flow remains unchanged
- existing suggestion flow remains unchanged
- existing history endpoints remain unchanged unless intentionally extended

### Manual Validation

- timer display matches server state
- browser refresh resumes active challenge correctly
- puzzle transitions are correct on solve/skip/fail
- score and assist counters update correctly
- leaderboard shows expected result ordering

## Acceptance Criteria

- A player can start a Challenge Mode session and receive a challenge ID.
- A challenge consists of 10 unique puzzles from the selected dictionary.
- Per-puzzle timer decreases by 5 seconds after each completed puzzle, with a minimum of 10 seconds.
- AI assists decrease by 1 after every 3 completed puzzles, with a minimum of 1.
- Pause can be used once per challenge for 10 seconds.
- Skip can be used once per challenge and applies a 20-point penalty.
- The challenge tracks cumulative score and current puzzle progress.
- Completed challenge results are persisted and can be ranked in a leaderboard.
- Existing non-challenge gameplay remains functional.
- Automated tests cover rules, service flow, controller contracts, and persistence.

## Risks

- Client-side timer authority would make the mode easy to bypass.
- Extending `GameSession` with challenge-only concerns would blur the architecture boundary.
- Implementing the full feature in one pass increases regression risk across gameplay, persistence, and UI.

## Recommended Work Breakdown

1. Finalize rules and score formula.
2. Build the server-side challenge domain and tests.
3. Add persistence and migrations.
4. Add controller endpoints and WebMvc tests.
5. Add UI and manual validation.
6. Run `mvn clean test` before completion.


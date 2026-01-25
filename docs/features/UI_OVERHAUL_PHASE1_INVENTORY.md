    # UI Overhaul — Phase 1 Inventory (Modes, Routes, UI States)

## Purpose
This document is the execution output of UI Overhaul **Phase 1**. It inventories:
- The **current UI “modes”** as implemented today (single-page + modals/panels)
- The **API surface area** the UI depends on
- The **UI state machines** needed to support each mode cleanly

Scope: frontend files in:
- [src/main/resources/static/index.html](src/main/resources/static/index.html)
- [src/main/resources/static/css/style.css](src/main/resources/static/css/style.css)
- [src/main/resources/static/js/game.js](src/main/resources/static/js/game.js)

And server routes in:
- [src/main/java/com/fistraltech/server/controller/WordGameController.java](src/main/java/com/fistraltech/server/controller/WordGameController.java)

---

## 1) Current UI Surfaces (what exists today)

### Global header actions (always visible)
From [src/main/resources/static/index.html](src/main/resources/static/index.html):
- Status banner (`#status`) — single inline banner
- Dictionary selector (`#dictionarySelector`) — changes grid size immediately; does not start a new game by itself
- Buttons:
  - New Game
  - Autoplay Games (toggles running; uses modal for config)
  - Player Analysis (modal for config)
  - Server Status
  - New Session
  - View Session

### Major sections / panels
- `#gameView` (grid layout)
  - Session stats + guess distribution + recent games (history panel)
  - Game board (input grid + guess history)
  - Game assistant panel (strategy selector + “Get Suggestion”)
  - Game analytics panel
- `#sessionViewer` (a full panel that hides the game grid)
- Modals:
  - `#autoplayModal`
  - `#analysisModal`

### “Modes” as currently implemented
The current app behaves like multiple modes already, but the separation is UI-structural rather than screen-based:

1. **Interactive Play (human)**
   - Primary: `#gameView`
   - Uses assistant + analytics + history panels

2. **Bot Demo / Automated Play (autoplay)**
   - Config in `#autoplayModal`
   - Executes inside `#gameView` (board updates), then auto-navigates to `#sessionViewer` at the end

3. **Bot Performance / Player Analysis (full dictionary test)**
   - Config in `#analysisModal`
   - Runs headless loops (does *not* update board UI during analysis)
   - Displays results in `#analysisResults` (a section in index.html)

4. **Dictionary Analysis**
   - Not implemented

5. **Admin**
   - Not implemented

---

## 2) API Inventory (Server routes + current UI usage)

Base path: `API_BASE = /api/wordai`

### Legend
- **Used by UI** means there is an actual `fetch()` in [src/main/resources/static/js/game.js](src/main/resources/static/js/game.js)
- **Server-only** means route exists but current UI does not call it

### Endpoints used by the UI

| Endpoint | Method | Used by mode(s) | UI caller(s) / notes | Request | Response fields used |
|---|---:|---|---|---|---|
| `/health` | GET | Interactive, Admin (future) | `checkHealth()` | none | `status`, `activeSessions`, `timestamp` (display) |
| `/dictionaries` | GET | All | `loadDictionaries()` populates selectors | none | list of `DictionaryOption` fields used in UI (`id`, `name`, `wordLength`, `available`, `description`) |
| `/dictionaries/{dictionaryId}` | GET | Bot Performance (analysis), future Dictionary Analysis | `startPlayerAnalysis()` loads **all words** | none | `wordLength`, `wordCount`, `words[]` |
| `/games` | POST | Interactive, Bot Demo, Bot Performance | `newGame()`, `runAutoplayGames()`, `runAnalysisGames()` | JSON optional: `{ dictionaryId?, targetWord?, wordLength? }` | `gameId`, `wordLength`, `maxAttempts`, `dictionaryMetrics` |
| `/games/{gameId}` | GET | Interactive, Bot Demo | Used to fetch target word at end / status recovery | none | `gameEnded`, `targetWord` (only if ended), `attempts`, `maxAttempts`, `guesses` |
| `/games/{gameId}` | DELETE | All | Cleanup old sessions | none | `message`, `gameId` |
| `/games/{gameId}/guess` | POST | Interactive, Bot Demo, Bot Performance | Core gameplay loop | `{ word }` | UI relies on: `attemptNumber`, `results[]`, `gameWon`, `gameOver`, `remainingWordsCount`, `dictionaryMetrics` |
| `/games/{gameId}/suggestion` | GET | Interactive, Bot Demo, Bot Performance | `getSuggestion()`, autoplay/analysis loops | none | `suggestion`, `strategy`, `remainingWords` |
| `/games/{gameId}/strategy` | PUT | Interactive, Bot Demo, Bot Performance | `changeStrategy()`, loops set strategy | `{ strategy }` | `strategy`, `message` |
| `/games/{gameId}/words` | GET | Interactive | Dictionary Viewer panel | none | `words[]`, `count` |

### Endpoints present on server but **not** used by current UI

| Endpoint | Method | Current status | Notes |
|---|---:|---|---|
| `/analysis` | POST | Server-only (unused) | Server supports running analysis via `WordGameService.runAnalysis(...)`, but current UI performs analysis client-side by looping over `/games` with `targetWord`. |
| `/algorithms` | GET | Server-only (unused) | Could drive strategy selectors from server rather than hard-coded lists in HTML. |

**Implication for UI overhaul:** the “Bot Performance” mode has two possible implementations.
- **Client-driven analysis (current):** heavy client loops; hard to cancel; stresses server with many sessions.
- **Server-driven analysis (available):** one call to `/analysis`; needs progress/streaming if we want live progress.

---

## 3) Frontend operation inventory (key workflows)

### Interactive Play workflow (human)
- Create game: `POST /games`
- Guess: `POST /games/{id}/guess`
- Suggest: `GET /games/{id}/suggestion`
- Dictionary viewer: `GET /games/{id}/words`
- Strategy change: `PUT /games/{id}/strategy`
- Cleanup: `DELETE /games/{id}` on new game

### Bot Demo (Autoplay) workflow
- Creates games in a loop: `POST /games`
- Sets strategy per game: `PUT /games/{id}/strategy`
- Plays per guess:
  - `GET /games/{id}/suggestion`
  - `POST /games/{id}/guess`
  - On end: `GET /games/{id}` (to fetch target word when lost)
- Cleans up per game: `DELETE /games/{id}`

### Bot Performance (Player Analysis) workflow (current implementation)
- Loads dictionary words: `GET /dictionaries/{dictionaryId}`
- For each target word:
  - `POST /games` with `{ dictionaryId, targetWord }`
  - `PUT /games/{id}/strategy`
  - Guess loop: `GET /suggestion` + `POST /guess` until win/lose
  - `DELETE /games/{id}`
- Results rendered client-side in a table; CSV download is client-side

---

## 4) UI State Machines (what the new screens must explicitly support)

These are the minimal state models to implement for reliable UX (progress/errors/disable rules).

### 4.1 Interactive Play state machine
- **no_game**: no active `currentGameId`
- **creating_game**: POST `/games` in-flight
- **ready_for_guess**: inputs enabled, `gameEnded = false`
- **submitting_guess**: POST `/guess` in-flight (disable submit)
- **game_won**: ended; show win message; disable inputs
- **game_lost**: ended; show loss message; disable inputs
- **error**: show actionable error; keep last stable state if possible

Key transitions:
- no_game → creating_game → ready_for_guess
- ready_for_guess → submitting_guess → ready_for_guess | game_won | game_lost
- any → error (recover to previous stable state)

### 4.2 Bot Demo (Autoplay) state machine
- **configuring**: user editing parameters
- **starting**: UI locks in parameters; clears session stats; first game created
- **running_game**: within a single game (guess loop running)
- **between_games**: short delay between games (optional)
- **stopping**: stop requested; finish current game
- **completed**: reached desired game count or stopped
- **error**: show error; decide whether to continue next game or halt

Notes:
- Current implementation uses `autoplayState.shouldStop` (stop-after-current-game behavior). This should map to **stopping** state explicitly in the new UI.

### 4.3 Bot Performance (Player Analysis) state machine
- **configuring**: selecting algorithm + dictionary + delay
- **loading_dictionary**: GET `/dictionaries/{id}` in-flight
- **running**: analysis loop executing
- **rendering_results**: building tables/summary
- **completed**: results visible + downloads enabled
- **error**: show error + allow returning to configuring

Notes:
- Current implementation disables header buttons `#autoplayBtn` and `#analysisBtn` while running.
- Cancellation is **not** currently supported; future UI should either implement cancel (client-side stop flag) or explicitly state “cannot cancel once started”.

### 4.4 Session Viewer UI states
- **hidden**: game grid visible
- **visible**: session analytics visible; game grid hidden

---

## 5) Mode-to-screen mapping (current vs future)

| Mode | Current UI implementation | Future target | Key missing pieces |
|---|---|---|---|
| Interactive Play | `#gameView` grid | Dedicated screen | Responsive re-layout, better status/progress |
| Bot Demo | Autoplay modal + runs in game grid | Dedicated screen | Start/pause/stop UX, progress, narration area |
| Bot Performance | Analysis modal + `#analysisResults` | Dedicated screen | Progress UX, potential server-based analysis |
| Dictionary Analysis | none | Placeholder screen | Needs endpoints/compute decisions |
| Admin | none | Placeholder screen | May use `/health` + future user/session endpoints |

---

## 6) Phase 1 Findings that affect the UI redesign

1. **Two analysis implementations exist** (client-driven today vs server-driven endpoint available). We should choose one for “Bot Performance” before Phase 7 implementation.
2. **Long-running work currently lives in the browser** (autoplay + analysis loops). The new UI must implement explicit running/stopping/error states.
3. **The current “status” model is a single banner** (`showStatus()`), used for everything (success, warning, error, progress). This is the core bottleneck for improving feedback UX in later phases.
4. **Session analytics is currently client-stored** (`sessionStorage`), which means “sessions” here are browser sessions, not server sessions. Admin “connected sessions” will need server-side concepts.

---

## Next phase (Phase 2 readiness)
Phase 2 can start once we confirm:
- Whether Bot Performance should be **client-loop** or **server `/analysis`**
- Whether routing should be hash-based (recommended) or multi-page (requires server changes)

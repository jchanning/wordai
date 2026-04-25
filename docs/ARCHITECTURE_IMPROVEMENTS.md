# Architecture & Design Improvement Recommendations

> Generated: 2026-02-27
> Reviewer: Claude Code (claude-sonnet-4.6)
> Codebase version: v1.9.0 (recommendations made); current version: v1.14.7
>
> Status update (April 2026): recommendations 1–15 have been implemented, including the API version boundary follow-up in `ARCH-24`. Historical sections for completed items are retained below for audit context.

---

## Summary Table

| Priority | # | Open issue |
|----------|---|------------|
| None | — | All items from this review are complete |

---

## High Priority

### 1. In-Memory Sessions Have No TTL (Memory Leak Risk)

**File:** `WordGameService.java`

`WordGameService` stores sessions in a `ConcurrentHashMap<String, GameSession>` with no expiry. A user who creates a game and walks away leaks that session forever. Under any non-trivial traffic, this map grows unbounded.

**Fix:** Replace with a `Caffeine` or `Guava` cache configured with a TTL (e.g., 30 minutes of inactivity). Spring's `CacheManager` supports this cleanly.

```java
// Example using Caffeine
@Bean
public Cache<String, GameSession> sessionCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build();
}
```

---

### 2. Per-Session Concurrency Not Protected

**File:** `WordGameService.java`, `GameSession.java`

`ConcurrentHashMap` ensures safe concurrent *map* access, but `GameSession` itself is mutable without synchronization. Two concurrent requests for the same `gameId` (e.g., a guess and a suggestion arriving simultaneously) can corrupt session state — the filter and filtered dictionary can diverge.

**Fix:** Either lock on `gameId` during mutations using `ConcurrentHashMap.compute()`, or synchronize the mutation methods on `GameSession`. If sessions are eventually persisted to a database, this is resolved by transaction isolation.

```java
// Example: lock on gameId during guess
activeSessions.compute(gameId, (id, session) -> {
    session.applyGuess(word);
    return session;
});
```

---

### 3. Admin Credentials Hardcoded in `application.properties`

**File:** `application.properties`

```properties
wordai.admin.seed.email=admin@wordai.local
wordai.admin.seed.password=ChangeMe123!
```

Even with "ChangeMe" in the value, these are checked into source control. Any repository exposure (accidental push, leaked backup) reveals the default admin password.

**Fix:** Remove from `application.properties`. Read from environment variables with no fallback default. Fail fast at startup if absent in non-dev profiles.

```properties
# application-prod.properties
wordai.admin.seed.email=${WORDAI_ADMIN_EMAIL}
wordai.admin.seed.password=${WORDAI_ADMIN_PASSWORD}
```

---

### 4. `ddl-auto=update` in Production Configuration

**File:** `application-prod.properties`

`spring.jpa.hibernate.ddl-auto=update` is a well-known production antipattern. Hibernate's DDL diffing is not reliable for all schema changes — it silently ignores some migrations and can cause data loss on column type changes.

**Fix:** Switch to `validate` in production and manage schema with **Flyway** or **Liquibase**. Add versioned migration scripts under `src/main/resources/db/migration/`.

```properties
# application-prod.properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true
```

---

### 5. Open CORS Wildcard in Production

**File:** `WordGameController.java`

```java
@CrossOrigin(origins = "*")
```

Allowing any origin to make credentialed requests (with OAuth2 cookies) is a meaningful security risk in production. The wildcard also prevents `SameSite` cookie protections from functioning correctly.

**Fix:** Move CORS configuration to `SecurityConfig` and configure allowed origins from a property. The wildcard should only apply in the `dev` profile.

```java
// SecurityConfig.java
configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
```

```properties
# application-prod.properties
wordai.allowed-origins=https://yourdomain.com
```

---

## Medium Priority

### 6. Fat Controller and Service Layer

**Files:** `WordGameController.java` (611 lines), `WordGameService.java` (304 lines)

`WordGameController` mixes game logic, history persistence, algorithm selection, analytics, and CORS configuration. `WordGameService` acts as both a session manager and an algorithm factory. Neither class has a single responsibility.

**Fix:** Extract and separate concerns:
- `AlgorithmFactory` — encapsulate the `getAlgorithm()` switch
- `GameController` — game creation and guessing only
- `DictionaryController` — dictionary listing and entropy endpoints
- `AnalysisController` — analysis and stats endpoints
- `GameHistoryService` — user history persistence

---

### 7. Duplicate Filter State Between `SelectionAlgo` and `GameSession`

**Files:** `SelectionAlgo.java`, `GameSession.java`

Both classes maintain a `Filter`. `GameSession.suggestWord()` applies its own filter to produce `filteredDict`, while `SelectionAlgo` also accumulates filter state internally. These can drift if the lifecycle is not perfectly coordinated — a subtle and hard-to-diagnose bug.

**Fix:** Make `SelectionAlgo` stateless. Pass the already-filtered `Dictionary` directly into `selectWord()`. The session is the single owner of filter lifecycle. Algorithms become pure functions: `(Dictionary, Response) -> String`.

---

### 8. Algorithm Selection Should Use a Registry

**File:** `WordGameService.java`

The `getAlgorithm()` method contains a `switch` on a string enum that directly instantiates concrete algorithm classes. Every new algorithm requires modifying `WordGameService`.

**Fix:** Use a Spring-managed Strategy Registry. Tag each algorithm implementation with a qualifier or custom annotation, then auto-discover them via `ApplicationContext`. New algorithms require zero changes to existing code.

```java
@Component
@AlgorithmType("entropy")
public class SelectMaximumEntropy extends SelectionAlgo { ... }

// Registry
@Component
public class AlgorithmRegistry {
    private final Map<String, SelectionAlgo> algorithms;

    public AlgorithmRegistry(List<SelectionAlgo> all) {
        this.algorithms = all.stream()
            .collect(toMap(a -> a.getType(), Function.identity()));
    }

    public SelectionAlgo get(String type) {
        return algorithms.getOrDefault(type, algorithms.get("random"));
    }
}
```

---

### 9. `game.js` is a 3967-Line Monolith — ✅ COMPLETED

**Status:** Resolved. The frontend has been split into 11 ES modules: `admin.js`, `analytics.js`, `api.js`, `autoplay.js`, `browser-session.js`, `game.js`, `keyboard.js`, `navigation.js`, `player-analysis.js`, `state.js`, `ui.js`.

**File:** `src/main/resources/static/js/game.js` (was)

The entire frontend lives in one file mixing DOM manipulation, API calls, game state, analytics rendering, autoplay logic, and CSV export. This makes isolated testing impossible and changes high-risk.

**Fix (incremental, no build toolchain required):** Split into ES modules and load with `<script type="module">`:

| Module | Responsibility |
|--------|---------------|
| `api.js` | All `fetch()` calls, error handling |
| `gameState.js` | Client-side state machine |
| `analytics.js` | Chart and metric rendering |
| `autoplay.js` | Autoplay engine |
| `export.js` | CSV/session export |
| `game.js` | Entry point, wires modules together |

---

### 10. No Session Persistence Across Server Restarts — ✅ COMPLETED

**Status:** Resolved. `SessionPersistenceService` persists active game sessions to the database via `ActiveGameSessionEntity`. Flyway migrations V2 and V3 manage the schema. Sessions survive server restarts and are scoped to browser sessions.

Active game sessions live only in JVM memory. A server restart (deployment, crash) silently destroys all in-progress games with no user feedback. Authenticated users are particularly affected since their game history and streak state can be corrupted.

**Fix (historical):** Persist `GameSession` state (at minimum: target word, guess history, strategy) to the database linked to the authenticated user. For anonymous users, use a short-lived signed token to reconstitute state. The `User` entity and JPA infrastructure are already in place.

---

## Low Priority

### 11. Entropy Recomputation Has No Memoization

**Status:** Resolved by `ARCH-22`. Shared `WordEntropy` instances now memoize lazy best-word lookups behind a bounded cache keyed by canonical candidate/target word-set signatures, and filtered `ENTROPY` session suggestions route through that shared cache instead of rebuilding one-off analyzers.

**File:** `SelectMaximumEntropy.java`, `GameSession.java`

Entropy computation is O(n²) over the candidate word set and is recomputed from scratch on every `suggestWord()` call. Many different game sessions will reach identical filter states (same first two guesses, for example) and redundantly repeat the same computation.

**Fix (implemented):** Cache entropy results keyed on a canonical representation of the candidate and target word sets, with bounded eviction so equivalent filtered states can reuse prior lazy entropy computations without unbounded memory growth.

---

### 12. `GameSession` is a God Object - COMPLETED

**Status:** Resolved by `ARCH-21`. `GameSession` now delegates metadata to `GameSessionMetadata` and mutable filter/strategy/suggestion state to `GameSessionContext`, leaving the session type as a thinner coordinator around `WordGame` and config.

**File:** `GameSession.java` (251 lines)

`GameSession` owns: the `WordGame`, a `Filter`, the original `Dictionary`, a filtered `Dictionary`, a `Config`, a cached `SelectionAlgo`, a cached `WordEntropy`, and the strategy name. It manages too many concerns.

**Fix (implemented):** Decompose:
- `GameState` — immutable snapshot (guesses, results, target word)
- `GameContext` — mutable: filter, filtered dictionary, active algorithm
- `GameMetadata` — strategy name, config, creation timestamp
- `GameSession` becomes a thin coordinator delegating to these

---

### 13. Feature Toggles in Application Properties

**Status:** Resolved by `ARCH-23`. Runtime algorithm enablement now lives in the `algorithm_policies` table, `AlgorithmFeatureService` applies persisted policy ahead of static defaults, and admins can change exposure through the secured admin API without redeploying.

**File:** `application.properties`

```properties
wordai.algorithm.random.enabled=true
wordai.algorithm.entropy.enabled=true
```

These flags require a redeploy to toggle, making A/B testing, gradual rollouts, or incident response (disabling a misbehaving algorithm) impractical.

**Fix (implemented):** Move feature flags to a persisted runtime policy table and add a secured admin endpoint to toggle them at runtime. Static application properties no longer act as the operational source of truth.

---

### 14. No API Versioning

**Status:** Resolved by `ARCH-24`. `/api/v1/wordai` is now the explicit API root, and the existing `/api/wordai` routes remain in place as a documented compatibility bridge while callers migrate.

All endpoints were previously at `/api/wordai/*` with no version prefix. Any breaking change to the API contract would have forced all clients to upgrade simultaneously with no transition period.

**Fix (implemented):** Add a `/api/v1/wordai` prefix as the primary API surface and keep the legacy routes available temporarily as a compatibility bridge.

```java
// Before
@RequestMapping("/api/wordai")

// After
@RequestMapping("/api/v1/wordai")
```

---

### 15. ArchUnit Tests Are Underutilised

**File:** `ArchitectureFitnessTest.java`

ArchUnit is present in the project but the rules it enforces are minimal. It provides the most value when it actively prevents architectural regressions — for example, ensuring controllers never instantiate core classes directly, or that the `bot` package never imports from `server`.

**Fix:** Add explicit layering rules that mirror the intended dependency graph:

```java
layeredArchitecture()
    .consideringAllDependencies()
    .layer("Controller").definedBy("..server.controller..")
    .layer("Service").definedBy("..server.service..", "..server..")
    .layer("Core").definedBy("..core..")
    .layer("Bot").definedBy("..bot..")
    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
    .whereLayer("Core").mayOnlyBeAccessedByLayers("Bot", "Service", "Controller");
```

---

*End of recommendations.*

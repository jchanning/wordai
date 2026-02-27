# Feature Specification: Per-Session Concurrency Protection

## 1. Summary

Eliminate two categories of race condition that can corrupt active `GameSession` state
when concurrent HTTP requests target the same `gameId`.

## 2. Problem Statement

The Caffeine cache introduced in `session-ttl.spec.md` ensures the session *map* is
accessed safely across threads, but `GameSession` itself is mutable and has no
thread-safety guarantees (as its Javadoc acknowledges).

Two concrete races exist today:

### Race 1 — Check-then-act in `makeGuess` (critical)

```
Thread A: session.isGameEnded()   → false    (game still live)
Thread B: session.isGameEnded()   → false    (hasn't seen A's update yet)
Thread A: wordGame.guess("crane") → attempts = 1
Thread B: wordGame.guess("slate") → attempts = 2
…all N threads proceed…
Thread A: session.setGameEnded(true) after attempt 3
Thread B–N: still guessing, attempt counter now exceeds maxAttempts
```

`WordGame`'s internal guess list (`ArrayList`) is not thread-safe, so repeated
concurrent appends also risk `ConcurrentModificationException`.

### Race 2 — `suggestWord` interleaves with `makeGuess` (data corruption)

`makeGuess` mutates `Filter` state (`filter.update(response)`) while
`session.suggestWord()` (called directly from the controller) reads the same `Filter`
to build its filtered dictionary. Both operations run unsynchronized, so
`suggestWord` can observe a half-updated filter.

## 3. Lock Design

`GameSession` is already the unit of isolation (one per `gameId`). Use the session's
**intrinsic lock** (`synchronized`) as the mutex:

| Location | Change | Protects |
|---|---|---|
| `WordGameService.makeGuess()` | `synchronized (session) { … }` around the full guess-update-endCheck block | Race 1 |
| `GameSession.suggestWord()` | Add `synchronized` modifier | Race 2 |
| `GameSession.setSelectedStrategy()` | Add `synchronized` modifier | strategy read/write race during `suggestWord` |

`synchronized (session)` in the service and `synchronized` on `GameSession` methods
both use the **same** intrinsic lock (the `session` object), so they mutually exclude
one another.

## 4. Scope Boundary

- Only the critical mutating and compound-read operations are synchronized.
- Read-only accessors (`isGameEnded`, `getCurrentAttempts`, `getFilteredDictionary`
  called from the API response builder) are intentionally left unsynchronized;
  they are snapshot reads where slightly-stale values are acceptable.
- This fix does **not** address the God Object concern or the duplicate filter state
  (tracked separately in `ARCHITECTURE_IMPROVEMENTS.md`).

## 5. Acceptance Tests (TDD)

| # | Scenario | Assertion |
|---|---|---|
| T1 | `N > maxAttempts` threads fire concurrent guesses with a `CountDownLatch` | Successful guesses ≤ `maxAttempts`; no `ConcurrentModificationException` |
| T2 | One thread guesses while another concurrently requests a suggestion | Neither throws an unexpected exception; suggestion reflects a consistent dictionary state |
| T3 | Two threads concurrently call `setSelectedStrategy` and `suggestWord` | `suggestWord` never observes a null or partially-written strategy |

## 6. Files Changed

| File | Change |
|------|--------|
| `GameSession.java` | `synchronized` on `suggestWord()` and `setSelectedStrategy()` |
| `WordGameService.java` | `synchronized (session)` block in `makeGuess()` |
| `src/test/.../server/SessionConcurrencyTest.java` | **New** — TDD concurrency tests |
| `docs/IMPLEMENTATION_STATUS.md` | Record completion |

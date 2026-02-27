# Feature Specification: Session TTL (Memory Leak Fix)

## 1. Summary

Replace the unbounded `ConcurrentHashMap` session store in `WordGameService` with a
Caffeine cache that automatically evicts sessions that have been inactive for a
configurable period (default 30 minutes).

## 2. User Story

As a **server operator**, I want active game sessions to expire automatically when
inactive, so that the JVM heap does not grow without bound under sustained traffic.

## 3. Technical Constraints

- **Library:** `com.github.ben-manes.caffeine:caffeine:3.1.8` (no Spring `@Cacheable`
  abstraction — the cache is managed directly inside `WordGameService`).
- **Eviction policy:** expire-after-access (idle timeout, not absolute TTL). A session
  that is being actively played should never be evicted mid-game.
- **Configurable:** TTL is read from `wordai.session.ttl-minutes` with a default of `30`.
- **Limit:** maximum 10 000 concurrent sessions; requests beyond this will cause the
  oldest to be evicted (size-based eviction fires _before_ TTL eviction).
- **Observability:** a `Logger.info` message must be emitted when a session is evicted
  so that operators can monitor session churn.
- **Backward compatibility:** the public API of `WordGameService` (`createGame`,
  `makeGuess`, `getGameSession`, `removeGameSession`, `getActiveSessionCount`) must not
  change.
- **Testability:** the service must expose a package-private constructor that accepts a
  pre-built `Cache<String, GameSession>`, enabling unit tests to inject a
  `FakeTicker`-backed cache without `Thread.sleep`.

## 4. Interface — No External Contract Changes

`WordGameService` public method signatures are unchanged. Only internal implementation
is modified.

## 5. Acceptance Tests (TDD)

| # | Given | When | Then |
|---|-------|------|------|
| T1 | A session has just been created | `getGameSession(id)` is called immediately | Session is returned (not null) |
| T2 | A session has been idle past the TTL | `getGameSession(id)` is called | Returns `null` (evicted) |
| T3 | A session is accessed within the TTL window | TTL re-evaluated after access | Session still accessible after the original TTL would have elapsed |
| T4 | A session expires | Eviction listener fires | A log message is emitted (listener is invoked with `wasEvicted() == true`) |
| T5 | A session is explicitly removed via `removeGameSession` | `getGameSession(id)` is called | Returns `null` and active count decreases |
| T6 | Multiple sessions exist, one expires | `getActiveSessionCount()` is called | Count reflects only the live sessions |

## 6. Edge Cases

- `getActiveSessionCount()` uses `Cache.estimatedSize()` which is eventually consistent;
  tests must call `cleanUp()` on the cache before asserting counts after eviction.
- The `FakeTicker` injected during tests increments nanoseconds; advance it past
  `ttl-minutes` to trigger idle eviction.

## 7. Files Changed

| File | Change |
|------|--------|
| `pom.xml` | Add `caffeine:3.1.8` dependency |
| `application.properties` | Add `wordai.session.ttl-minutes=30` |
| `WordGameService.java` | Replace `ConcurrentHashMap` with Caffeine `Cache`; add package-private test constructor |
| `src/test/.../server/SessionTtlTest.java` | **New** — TDD test class |
| `docs/IMPLEMENTATION_STATUS.md` | Record completion |

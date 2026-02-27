package com.fistraltech.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

/**
 * TDD tests for the session TTL (memory-leak fix) feature.
 *
 * <p>Tests use a {@link FakeTicker} so that time can be advanced programmatically
 * without {@code Thread.sleep}.  They rely on a package-private constructor on
 * {@link WordGameService} that accepts a pre-built {@link Cache}, which keeps the
 * tests free of Spring context overhead.
 *
 * <p>Spec: {@code docs/features/session-ttl.spec.md}
 */
class SessionTtlTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Nanosecond-resolution fake clock for Caffeine. Thread-unsafe — single-threaded tests only. */
    private static class FakeTicker implements Ticker {
        private long nanos = 0;

        @Override
        public long read() { return nanos; }

        void advance(long amount, TimeUnit unit) {
            nanos += unit.toNanos(amount);
        }
    }

    private static Cache<String, GameSession> buildCache(FakeTicker ticker, long ttlMinutes) {
        return Caffeine.newBuilder()
                .expireAfterAccess(ttlMinutes, TimeUnit.MINUTES)
                .ticker(ticker)
                .build();
    }

    /**
     * Construct a {@link WordGameService} backed by the supplied cache.
     * Uses the package-private constructor added for testability.
     */
    private static WordGameService serviceWith(Cache<String, GameSession> cache) {
        DictionaryService mockDict = mock(DictionaryService.class);
        when(mockDict.getConfig()).thenReturn(new Config());
        return new WordGameService(mockDict, cache);
    }

    // -----------------------------------------------------------------------
    // T1 — session is accessible immediately after insertion
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: Session is retrievable immediately after creation")
    void givenFreshSession_whenRetrievedImmediately_thenNotNull() {
        FakeTicker ticker = new FakeTicker();
        Cache<String, GameSession> cache = buildCache(ticker, 30);
        WordGameService service = serviceWith(cache);

        cache.put("game-1", mock(GameSession.class));

        assertNotNull(service.getGameSession("game-1"),
                "Session should be present before TTL elapses");
    }

    // -----------------------------------------------------------------------
    // T2 — idle session is evicted after TTL
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: Session is evicted after idle TTL elapses")
    void givenIdleSession_whenTtlElapses_thenEvicted() {
        FakeTicker ticker = new FakeTicker();
        Cache<String, GameSession> cache = buildCache(ticker, 30);
        WordGameService service = serviceWith(cache);

        cache.put("game-2", mock(GameSession.class));

        ticker.advance(31, TimeUnit.MINUTES);
        cache.cleanUp();

        assertNull(service.getGameSession("game-2"),
                "Session should be evicted after idle TTL of 30 minutes");
    }

    // -----------------------------------------------------------------------
    // T3 — accessing a session resets its idle timer
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: Accessing a session within the TTL window resets the idle timer")
    void givenActiveSession_whenAccessedWithinTtl_thenTtlReset() {
        FakeTicker ticker = new FakeTicker();
        Cache<String, GameSession> cache = buildCache(ticker, 30);
        WordGameService service = serviceWith(cache);

        cache.put("game-3", mock(GameSession.class));

        // Advance to just before expiry and access the session — resets idle timer
        ticker.advance(29, TimeUnit.MINUTES);
        assertNotNull(service.getGameSession("game-3"), "Should be present at 29 min");

        // Advance another 29 minutes; total elapsed = 58 min, but only 29 since last access
        ticker.advance(29, TimeUnit.MINUTES);
        cache.cleanUp();

        assertNotNull(service.getGameSession("game-3"),
                "Session TTL should have been reset by the access; still within 30 min idle window");
    }

    // -----------------------------------------------------------------------
    // T4 — eviction listener fires on TTL expiry
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T4: Eviction listener is invoked when a session expires via TTL")
    void givenExpiredSession_thenEvictionListenerInvoked() {
        FakeTicker ticker = new FakeTicker();
        AtomicBoolean listenerFired = new AtomicBoolean(false);

        Cache<String, GameSession> cache = Caffeine.newBuilder()
                .expireAfterAccess(30, TimeUnit.MINUTES)
                .ticker(ticker)
                .executor(Runnable::run)   // run listener inline so cleanUp() fires it synchronously
                .removalListener((key, value, cause) -> {
                    if (cause.wasEvicted()) {
                        listenerFired.set(true);
                    }
                })
                .build();

        WordGameService service = serviceWith(cache);
        cache.put("game-4", mock(GameSession.class));

        ticker.advance(31, TimeUnit.MINUTES);
        cache.cleanUp();

        assertTrue(listenerFired.get(),
                "Eviction listener should fire when a session expires via TTL");
    }

    // -----------------------------------------------------------------------
    // T5 — explicitly removed session is gone and count drops
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T5: Explicitly removed session is no longer accessible and count decreases")
    void givenSession_whenExplicitlyRemoved_thenNotFoundAndCountDecreases() {
        FakeTicker ticker = new FakeTicker();
        Cache<String, GameSession> cache = buildCache(ticker, 30);
        WordGameService service = serviceWith(cache);

        cache.put("game-5", mock(GameSession.class));
        cache.cleanUp();
        assertEquals(1, service.getActiveSessionCount(), "Count should be 1 before removal");

        service.removeGameSession("game-5");
        cache.cleanUp();

        assertNull(service.getGameSession("game-5"),
                "Session should be null after explicit removal");
        assertEquals(0, service.getActiveSessionCount(),
                "Count should drop to 0 after explicit removal");
    }

    // -----------------------------------------------------------------------
    // T6 — only the idle session is evicted; others remain
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T6: Only the idle session is evicted; recently accessed sessions survive")
    void givenMultipleSessions_whenOnlyOneIsIdle_thenOnlyIdleSessionEvicted() {
        FakeTicker ticker = new FakeTicker();
        Cache<String, GameSession> cache = buildCache(ticker, 30);
        WordGameService service = serviceWith(cache);

        cache.put("s1", mock(GameSession.class));
        cache.put("s2", mock(GameSession.class));

        // Advance to 29 min and access s2 only — resets s2's idle timer
        ticker.advance(29, TimeUnit.MINUTES);
        assertNotNull(service.getGameSession("s2"), "Access s2 to reset its TTL");

        // Advance 2 more minutes: s1 has been idle for 31 min, s2 only 2 min
        ticker.advance(2, TimeUnit.MINUTES);
        cache.cleanUp();

        assertNull(service.getGameSession("s1"),
                "s1 should be evicted (31 min idle — past 30-min TTL)");
        assertNotNull(service.getGameSession("s2"),
                "s2 should survive (only 2 min idle since last access)");
    }
}

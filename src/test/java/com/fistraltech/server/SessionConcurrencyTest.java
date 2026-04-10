package com.fistraltech.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * TDD tests for the per-session concurrency fix.
 *
 * <p><strong>Why these tests are reliably RED without the fix:</strong>
 * {@link WordGame#guess} appends to an internal {@code ArrayList} of guesses.
 * With {@code N > maxAttempts} threads starting simultaneously via a
 * {@link CountDownLatch}, multiple threads attempt concurrent ArrayList appends,
 * producing a {@code ConcurrentModificationException} (or at minimum an attempt
 * count that exceeds {@code maxAttempts}).  Both conditions are caught and
 * reported as test failures.
 *
 * <p>Spec: {@code docs/features/session-concurrency.spec.md}
 */
@DisplayName("Session Concurrency Tests")
class SessionConcurrencyTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final int THREAD_COUNT = 10;

    private Dictionary dict;
    private Config config;
    private WordGameService service;
    private Cache<String, GameSession> cache;

    @BeforeEach
    void setUp() {
        dict = new Dictionary(5);
        Set<String> words = new HashSet<>(Arrays.asList(
                "arose", "crane", "slate", "raise", "stare",
                "house", "beach", "world", "tests", "hello"));
        dict.addWords(words);

        config = new Config();
        config.setWordLength(5);
        config.setMaxAttempts(MAX_ATTEMPTS);

        cache = Caffeine.newBuilder().build();

        DictionaryService mockDict = mock(DictionaryService.class);
        when(mockDict.getConfig()).thenReturn(config);
        service = new WordGameService(mockDict, cache);
    }

    /** Creates a fresh GameSession with the given target and registers it in the cache. */
    private String newSession(String target) throws InvalidWordException {
        WordGame game = new WordGame(dict, config);
        game.setTargetWord(target);
        String gameId = "session-" + target;
        GameSession session = new GameSession(gameId, game, config, dict, AlgorithmRegistry.withDefaults());
        cache.put(gameId, session);
        return gameId;
    }

    // -----------------------------------------------------------------------
    // T1 — concurrent guesses never exceed maxAttempts, no CME
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: N concurrent guesses produce ≤ maxAttempts successes and no unexpected exceptions")
    void concurrentGuesses_neverExceedMaxAttempts_noUnexpectedExceptions()
            throws Exception {

        String gameId = newSession("arose");

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    startGate.await();
                    service.makeGuess(gameId, "crane");
                    successCount.incrementAndGet();
                } catch (InvalidWordException e) {
                    // "game has already ended" is expected once the limit is reached
                    if (!e.getMessage().contains("already ended")
                            && !e.getMessage().contains("not found")) {
                        unexpectedErrors.add(e);
                    }
                } catch (Throwable t) {
                    // ConcurrentModificationException, ArrayIndexOutOfBoundsException, etc.
                    unexpectedErrors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(10, TimeUnit.SECONDS), "Threads did not finish in time");
        pool.shutdown();

        assertTrue(unexpectedErrors.isEmpty(),
                "Unexpected exception(s) from concurrent guesses: " + unexpectedErrors);
        assertTrue(successCount.get() <= MAX_ATTEMPTS,
                "More guesses succeeded (" + successCount.get()
                + ") than maxAttempts (" + MAX_ATTEMPTS + ") — race condition in makeGuess");
    }

    // -----------------------------------------------------------------------
    // T2 — concurrent guess + suggestion: no unexpected exceptions
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: Concurrent makeGuess and suggestWord do not throw unexpected exceptions")
    void concurrentGuessAndSuggestion_noUnexpectedExceptions() throws Exception {

        String gameId = newSession("arose");
        GameSession session = cache.getIfPresent(gameId);

        int rounds = 30;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(rounds * 2);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(rounds * 2);

        for (int i = 0; i < rounds; i++) {
            // Thread type A: make a guess
            pool.submit(() -> {
                try {
                    startGate.await();
                    service.makeGuess(gameId, "crane");
                } catch (InvalidWordException e) {
                    // "game ended" or "not found" after session ends — expected
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    endGate.countDown();
                }
            });

            // Thread type B: request a suggestion
            pool.submit(() -> {
                try {
                    startGate.await();
                    session.suggestWord();
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(10, TimeUnit.SECONDS), "Threads did not finish in time");
        pool.shutdown();

        assertTrue(unexpectedErrors.isEmpty(),
                "Unexpected exception(s) from concurrent guess+suggestion: " + unexpectedErrors);
    }

    // -----------------------------------------------------------------------
    // T3 — concurrent setSelectedStrategy + suggestWord: no NullPointerException
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: Concurrent strategy change and suggestWord do not produce NullPointerException")
    void concurrentStrategyChangeAndSuggestion_noNullPointerException() throws Exception {

        String gameId = newSession("arose");
        GameSession session = cache.getIfPresent(gameId);

        String[] strategies = {"RANDOM", "ENTROPY", "RANDOM", "ENTROPY"};
        int rounds = 40;

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(rounds * 2);
        List<Throwable> unexpectedErrors = new CopyOnWriteArrayList<>();
        List<String> nullSuggestions = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(rounds * 2);

        for (int i = 0; i < rounds; i++) {
            final String strategy = strategies[i % strategies.length];

            // Thread A: change strategy
            pool.submit(() -> {
                try {
                    startGate.await();
                    session.setSelectedStrategy(strategy);
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    endGate.countDown();
                }
            });

            // Thread B: request suggestion (reads strategy)
            pool.submit(() -> {
                try {
                    startGate.await();
                    String word = session.suggestWord();
                    // null is acceptable (empty dictionary), but NPE is not
                } catch (NullPointerException npe) {
                    unexpectedErrors.add(npe);
                } catch (Throwable t) {
                    unexpectedErrors.add(t);
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertTrue(endGate.await(10, TimeUnit.SECONDS), "Threads did not finish in time");
        pool.shutdown();

        assertTrue(unexpectedErrors.isEmpty(),
                "Unexpected exception(s) from concurrent strategy change + suggestion: "
                        + unexpectedErrors);
    }
}

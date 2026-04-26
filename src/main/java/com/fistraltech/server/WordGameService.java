package com.fistraltech.server;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fistraltech.analysis.AnalysisResponse;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Filter;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;
import com.fistraltech.web.ApiResourceNotFoundException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;

/**
 * Service layer for creating and managing WordAI game sessions.
 *
 * <p>This class is the core of the Spring Boot API implementation:
 * it owns the in-memory session map, creates {@link WordGame} instances,
 * applies guesses, and exposes a server-side full-dictionary analysis entry point.
 *
 * <p><strong>Session persistence</strong>: When a {@link SessionPersistenceService} bean
 * is present (production), game sessions for authenticated users are persisted to the
 * database and can be reconstructed after a server restart. Guest sessions are never
 * persisted.
 *
 * <p><strong>Conceptual model</strong>
 * <ul>
 *   <li>A {@link GameSession} represents one active game identified by a UUID-like {@code gameId}.</li>
 *   <li>Sessions are stored in-memory in a Caffeine cache; restarting the server evicts them.</li>
 *   <li>Authenticated-user sessions are mirrored to the DB and reconstructed on cache miss.</li>
 * </ul>
 *
 * <p><strong>Thread safety</strong>
 * <ul>
 *   <li>The session store is a Caffeine {@link Cache} which is thread-safe for concurrent
 *       reads and writes across different game IDs.</li>
 *   <li>Individual sessions ({@link WordGame}/{@link Filter}) are not designed for concurrent
 *       mutation; see {@link GameSession#suggestWord()} for per-session locking.</li>
 * </ul>
 *
 * @author Fistral Technologies
 * @see com.fistraltech.server.controller.WordGameController
 * @see DictionaryService
 */
@Service
public class WordGameService {
    private static final Logger logger = Logger.getLogger(WordGameService.class.getName());

    private final Cache<String, GameSession> activeSessions;
    private final DictionaryService dictionaryService;
    private final Config config;
    private final AlgorithmRegistry algorithmRegistry;
    private final SessionReconstructor sessionReconstructor;

    /**
     * Optional: injected only when persistence is configured (always in production,
     * null in unit tests that use the package-private constructor).
     */
    @Autowired(required = false)
    private SessionPersistenceService sessionPersistenceService;

    /**
     * Production constructor — Spring injects the dictionary service, algorithm registry, and TTL setting.
     */
    @Autowired
    public WordGameService(DictionaryService dictionaryService,
                           AlgorithmRegistry algorithmRegistry,
                                                   SessionReconstructor sessionReconstructor,
                           @Value("${wordai.session.ttl-minutes:30}") int sessionTtlMinutes) {
        logger.info("Initializing WordGameService...");
        this.dictionaryService = dictionaryService;
        this.algorithmRegistry = algorithmRegistry;
            this.sessionReconstructor = sessionReconstructor;
        this.config = dictionaryService.getConfig();
        this.activeSessions = Caffeine.newBuilder()
                .expireAfterAccess(sessionTtlMinutes, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .removalListener((String gameId, GameSession session, RemovalCause cause) -> {
                    if (cause.wasEvicted()) {
                        logger.info(() -> "Game session evicted due to inactivity: "
                                + gameId + " (cause=" + cause + ")");
                    }
                })
                .build();
        logger.info(() -> "WordGameService initialised — session TTL: "
                + sessionTtlMinutes + " min, max sessions: 10 000");
    }

    /**
     * Package-private constructor for unit tests.
     *
     * <p>Accepts a pre-built {@link Cache} so that tests can inject a
     * {@code FakeTicker}-backed cache and advance time without {@code Thread.sleep}.
     * {@code sessionPersistenceService} is left {@code null} — persistence is not
     * exercised in unit tests.
     */
    WordGameService(DictionaryService dictionaryService, Cache<String, GameSession> sessionCache) {
        this.dictionaryService = dictionaryService;
        this.config = dictionaryService.getConfig();
        this.activeSessions = sessionCache;
        this.algorithmRegistry = AlgorithmRegistry.withDefaults();
        this.sessionReconstructor = new SessionReconstructor(
            dictionaryService,
            this.algorithmRegistry,
            null);
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    /**
     * Creates a new game session for a guest (no persistence).
     * Delegates to {@link #createGame(String, Integer, String, Long)} with {@code null} userId.
     */
    public String createGame(String targetWord, Integer wordLength, String dictionaryId) throws InvalidWordException {
        return createGame(targetWord, wordLength, dictionaryId, null, null, false);
    }

    /**
     * Creates a new game session (backward compatibility).
     */
    public String createGame(String targetWord, Integer wordLength) throws InvalidWordException {
        return createGame(targetWord, wordLength, null, null, null, false);
    }

    public String createGame(String targetWord, Integer wordLength, String dictionaryId, Long userId)
            throws InvalidWordException {
        return createGame(targetWord, wordLength, dictionaryId, userId, null, false);
    }

    public String createGame(String targetWord, Integer wordLength, String dictionaryId,
            Long userId, String browserSessionId)
            throws InvalidWordException {
        return createGame(targetWord, wordLength, dictionaryId, userId, browserSessionId, false);
    }

    /**
     * Creates (or restores) a game session for the given user.
     *
    * <p>If {@code resumeExisting} is {@code true}, {@code userId} is non-null, and an ACTIVE
    * session for this user + dictionary + browser session already exists in the DB, the existing
    * session is reconstructed and returned instead of creating a new game. Normal create-game
    * calls do not resume implicitly, which keeps manual play, autoplay, and analysis isolated.
     *
     * @param targetWord   optional explicit target (lower-cased). If {@code null}, a random target is chosen.
     * @param wordLength   optional word length (only used when {@code dictionaryId} is not supplied).
     * @param dictionaryId optional dictionary identifier from configuration.
         * @param userId       the authenticated player's numeric user ID; {@code null} for guests.
         * @param browserSessionId per-browser-window identifier; when absent, no persisted-session reuse occurs.
             * @param resumeExisting when {@code true}, reuse an ACTIVE session for the same browser context.
     * @return the game session ID (new or existing).
     * @throws InvalidWordException if the dictionary is invalid, cannot be loaded, or the target word
     *                              is not in the dictionary.
     */
        public String createGame(String targetWord, Integer wordLength, String dictionaryId,
                 Long userId, String browserSessionId, boolean resumeExisting)
            throws InvalidWordException {

        String effectiveDictionaryId = (dictionaryId != null && !dictionaryId.isEmpty())
                ? dictionaryId : "default";
        String effectiveBrowserSessionId = StringUtils.hasText(browserSessionId)
            ? browserSessionId.trim()
            : null;

        // Resumption is explicit so different game flows in the same browser do not collide.
        if (resumeExisting && userId != null && sessionPersistenceService != null
            && effectiveBrowserSessionId != null) {
            String existingGameId = sessionReconstructor.findAndReconstructActiveSession(
                userId, effectiveDictionaryId, effectiveBrowserSessionId);
            if (existingGameId != null) {
                return existingGameId;
            }
        }

        String gameId = UUID.randomUUID().toString();
        logger.info(() -> "Creating new game session with ID: " + gameId);

        // Get a cloned dictionary from the cache (no file I/O!)
        Dictionary gameDictionary = dictionaryService.getDictionaryForGame(effectiveDictionaryId);
        if (gameDictionary == null) {
            logger.warning(() -> "Dictionary not found for ID: " + effectiveDictionaryId);
            throw new InvalidWordException("Dictionary not found: " + effectiveDictionaryId);
        }

        int actualWordLength = gameDictionary.getWordLength();
        logger.info(() -> "Using cached dictionary '" + effectiveDictionaryId + "' with "
                   + gameDictionary.getWordCount() + " words");

        Config gameConfig = new Config();
        gameConfig.setWordLength(actualWordLength);
        gameConfig.setMaxAttempts(config.getMaxAttempts());

        WordGame wordGame = new WordGame(gameDictionary, gameConfig);

        if (targetWord != null) {
            String normalizedTarget = targetWord.toLowerCase();
            if (!gameDictionary.contains(normalizedTarget)) {
                throw new InvalidWordException("Target word not in dictionary: " + targetWord);
            }
            wordGame.setTargetWord(normalizedTarget);
        } else {
            wordGame.setRandomTargetWord();
        }

        GameSession session = new GameSession(gameId, wordGame, gameConfig, gameDictionary, algorithmRegistry);
        session.setDictionaryId(effectiveDictionaryId);
        session.setUserId(userId);
        session.setBrowserSessionId(effectiveBrowserSessionId);

        // Set cached WordEntropy for fast entropy-based suggestions
        com.fistraltech.core.WordEntropy cachedEntropy = dictionaryService.getWordEntropy(effectiveDictionaryId);
        if (cachedEntropy != null) {
            session.setCachedWordEntropy(cachedEntropy);
            logger.fine(() -> "Set cached WordEntropy for session " + gameId);
        }

        activeSessions.put(gameId, session);

        // Persist for authenticated users
        if (userId != null && sessionPersistenceService != null) {
            try {
                sessionPersistenceService.save(session, userId);
            } catch (RuntimeException e) {
                activeSessions.invalidate(gameId);
                throw e;
            }
        }

        logger.info(() -> "Created new game session: " + gameId);
        return gameId;
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /**
     * Gets the game session by ID.  On a cache miss, attempts to reconstruct the session
     * from the database if {@link SessionPersistenceService} is available.
     *
     * @param gameId the game session ID
     * @return the session, or {@code null} if not found locally or in the DB
     */
    public GameSession getGameSession(String gameId) {
        GameSession session = activeSessions.getIfPresent(gameId);
        if (session != null || sessionPersistenceService == null) {
            return session;
        }
        // Cache miss — try to reconstruct from DB
        return sessionPersistenceService.findById(gameId)
                .map(entity -> {
                    GameSession reconstructed = sessionReconstructor.reconstructFromEntity(entity);
                    if (reconstructed != null) {
                        activeSessions.put(gameId, reconstructed);
                    }
                    return reconstructed;
                })
                .orElse(null);
    }

    // ------------------------------------------------------------------
    // Guess
    // ------------------------------------------------------------------

    /**
     * Applies a guess to the specified session and updates its filter state.
     *
     * <p>The entire check-then-act sequence is executed inside a {@code synchronized(session)}
     * block to prevent concurrent races. The DB update (for authenticated sessions) is
     * performed outside the synchronized block for performance.
     *
     * @throws InvalidWordException if the session does not exist, the game already ended, or the
     *                              guess is invalid.
     */
    public Response makeGuess(String gameId, String word) throws InvalidWordException {
        GameSession session = activeSessions.getIfPresent(gameId);
        if (session == null) {
            throw new ApiResourceNotFoundException("Game not found",
                    "Game session " + gameId + " does not exist");
        }

        Response response;
        synchronized (session) {
            if (session.isGameEnded()) {
                throw new InvalidWordException("Game has already ended");
            }

            response = session.getWordGame().guess(word.toLowerCase());
            session.getWordFilter().update(response);
            response.setRemainingWordsCount(session.getRemainingWordsCount());

            if (response.getWinner() || session.isMaxAttemptsReached()) {
                session.setGameEnded(true);
                logger.info(() -> "Game session ended: " + gameId + " (Winner: " + response.getWinner() + ")");
            }
        }

        // Persist updated state for authenticated sessions (outside sync block)
        if (session.getUserId() != null && sessionPersistenceService != null) {
            sessionPersistenceService.update(session);
        }

        return response;
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    /**
     * Removes a game session from the in-memory cache and (for authenticated users)
     * deletes the persisted DB row.
     *
     * @param gameId the game session ID
     */
    public void removeGameSession(String gameId) {
        GameSession session = activeSessions.getIfPresent(gameId);
        activeSessions.invalidate(gameId);
        logger.info(() -> "Removed game session: " + gameId);
        if (session != null && session.getUserId() != null && sessionPersistenceService != null) {
            sessionPersistenceService.delete(gameId);
        }
    }

    // ------------------------------------------------------------------
    // Misc
    // ------------------------------------------------------------------

    /**
     * Gets the number of active game sessions.
     */
    public int getActiveSessionCount() {
        return (int) activeSessions.estimatedSize();
    }

    /**
     * Loads a dictionary by id from the cache.
     *
     * @param dictionaryId the dictionary identifier
     * @return the cached Dictionary
     * @throws InvalidWordException if the dictionary is not found
     */
    public Dictionary loadDictionary(String dictionaryId) throws InvalidWordException {
        String effectiveId = (dictionaryId != null && !dictionaryId.isEmpty()) ? dictionaryId : "default";
        Dictionary dictionary = dictionaryService.getMasterDictionary(effectiveId);
        if (dictionary == null) {
            throw new InvalidWordException("Dictionary not found: " + effectiveId);
        }
        logger.info(() -> "Retrieved cached dictionary '" + effectiveId + "' with "
                   + dictionary.getWordCount() + " words");
        return dictionary;
    }

    /**
     * Runs a complete dictionary analysis using the specified algorithm.
     */
    public AnalysisResponse runAnalysis(
            String algorithm, String dictionaryId, Integer maxGames) throws Exception {

        String effectiveId = (dictionaryId != null && !dictionaryId.isEmpty()) ? dictionaryId : "default";

        Dictionary analysisDictionary = dictionaryService.getDictionaryForGame(effectiveId);
        if (analysisDictionary == null) {
            throw new InvalidWordException("Dictionary not found for analysis: " + effectiveId);
        }

        logger.info(() -> "Running analysis with " + analysisDictionary.getWordCount()
                   + " words using algorithm: " + algorithm);

        WordGame game = new WordGame(analysisDictionary, analysisDictionary, config);
        com.fistraltech.bot.selection.SelectionAlgo selectionAlgo = algorithmRegistry.create(algorithm, analysisDictionary);
        com.fistraltech.bot.WordGamePlayer player = new com.fistraltech.bot.WordGamePlayer(game, selectionAlgo);

        com.fistraltech.analysis.PlayerAnalyser analyser =
            new com.fistraltech.analysis.PlayerAnalyser(player, false, null);

        return analyser.analyseGamePlay(maxGames);
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------
}

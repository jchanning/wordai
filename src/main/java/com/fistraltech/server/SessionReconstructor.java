package com.fistraltech.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Filter;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ActiveGameSessionEntity;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;

/**
 * Encapsulates session reconstruction logic for cache-miss scenarios.
 *
 * <p>Responsibility: Reconstruct a {@link GameSession} from a persisted
 * {@link ActiveGameSessionEntity} by replaying all stored guesses, or find an
 * existing active session for a given user + dictionary + browser context.
 * This improves {@link WordGameService} clarity by isolating reconstruction
 * and DB-lookup seams from game lifecycle management.
 *
 * <p><strong>Design note</strong>: This service is stateless except for its
 * injected dependencies. Reconstruction is expensive (involves guess replay),
 * so it is only used on cache misses.
 *
 * @author Fistral Technologies
 * @see WordGameService#getGameSession
 */
@Service
public class SessionReconstructor {
    private static final Logger logger = Logger.getLogger(SessionReconstructor.class.getName());

    private final DictionaryService dictionaryService;
    private final AlgorithmRegistry algorithmRegistry;
    private final SessionPersistenceService sessionPersistenceService;

    /**
     * Constructor.
     *
     * @param dictionaryService provides cached dictionaries for reconstruction
     * @param algorithmRegistry provides selection algorithms for game sessions
     * @param sessionPersistenceService provides DB access for session lookup
     */
    public SessionReconstructor(DictionaryService dictionaryService,
                                 AlgorithmRegistry algorithmRegistry,
                                 SessionPersistenceService sessionPersistenceService) {
        this.dictionaryService = dictionaryService;
        this.algorithmRegistry = algorithmRegistry;
        this.sessionPersistenceService = sessionPersistenceService;
    }

    /**
     * Reconstructs a {@link GameSession} from a persisted {@link ActiveGameSessionEntity}
     * by replaying all stored guesses.
     *
     * <p>This method is called when a cache miss occurs in {@link WordGameService#getGameSession}.
     * It loads the original dictionary, restores the WordGame state, and re-applies each guess
     * to restore the filter state.
     *
     * @param entity the persisted entity
     * @return the reconstructed session, or throws if the dictionary is unavailable
     */
    public GameSession reconstructFromEntity(ActiveGameSessionEntity entity) {
        try {
            String dictId = entity.getDictionaryId();
            Dictionary dict = dictionaryService.getDictionaryForGame(dictId);
            if (dict == null) {
                throw new IllegalStateException("Cannot reconstruct session " + entity.getGameId()
                        + " because dictionary '" + dictId + "' is unavailable");
            }

            Config gameConfig = new Config();
            gameConfig.setWordLength(dict.getWordLength());
            gameConfig.setMaxAttempts(dictionaryService.getConfig().getMaxAttempts());

            WordGame wordGame = new WordGame(dict, gameConfig);
            wordGame.setTargetWord(entity.getTargetWord());

            GameSession session = new GameSession(
                    entity.getGameId(), wordGame, gameConfig, dict, algorithmRegistry);
            session.setDictionaryId(dictId);
            session.setUserId(entity.getUserId());
            session.setBrowserSessionId(entity.getBrowserSessionId());
            session.setSelectedStrategy(entity.getStrategy());

            com.fistraltech.core.WordEntropy cachedEntropy = dictionaryService.getWordEntropy(dictId);
            if (cachedEntropy != null) {
                session.setCachedWordEntropy(cachedEntropy);
            }

            // Replay stored guesses to restore in-memory filter and WordGame state
            String guessWordsStr = entity.getGuessWords();
            if (guessWordsStr != null && !guessWordsStr.isEmpty()) {
                for (String guess : guessWordsStr.split(",")) {
                    if (!guess.isEmpty()) {
                        Response response = wordGame.guess(guess);
                        updateFilterBasedOnResponse(session.getWordFilter(), response);
                        response.setRemainingWordsCount(session.getRemainingWordsCount());
                        if (response.getWinner() || session.isMaxAttemptsReached()) {
                            session.setGameEnded(true);
                        }
                    }
                }
            }

            logger.info(() -> "Reconstructed session " + entity.getGameId()
                    + " with " + session.getCurrentAttempts() + " guesses");
            return session;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reconstruct session " + entity.getGameId(), e);
            throw new IllegalStateException("Failed to reconstruct session " + entity.getGameId(), e);
        }
    }

    /**
     * Looks for an existing ACTIVE session for the given user + dictionary + browser session in the DB.
     * If found, reconstructs and returns its game ID.
     *
     * <p>This method is called when the user requests session resumption ({@code resumeExisting=true}).
     *
     * @param userId the authenticated user ID
     * @param dictionaryId the target dictionary
     * @param browserSessionId the browser window identifier
     * @return the game ID of the reconstructed session, or empty if none found
     */
    public String findAndReconstructActiveSession(Long userId, String dictionaryId, String browserSessionId) {
        return sessionPersistenceService.findActiveForUser(userId, dictionaryId, browserSessionId)
                .map(entity -> {
                    String existingId = entity.getGameId();
                    logger.info(() -> "Found active session " + existingId + " for user " + userId);

                    GameSession reconstructed = reconstructFromEntity(entity);
                    if (reconstructed != null && !reconstructed.isGameEnded()) {
                        logger.info(() -> "Reconstructed active session " + existingId + " for user " + userId);
                        return existingId;
                    }
                    return null;
                })
                .orElse(null);
    }

    /**
     * Updates the filter based on a guess response.
     *
     * @param filter the filter to update
     * @param response the guess response
     */
    private void updateFilterBasedOnResponse(Filter filter, Response response) {
        filter.update(response);
    }
}

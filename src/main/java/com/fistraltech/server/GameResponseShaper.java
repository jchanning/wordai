package com.fistraltech.server;

import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.server.dto.CreateGameResponse;
import com.fistraltech.server.dto.GameResponse;
import com.fistraltech.server.model.GameSession;

/**
 * Encapsulates response DTO assembly for game endpoints.
 *
 * <p>Responsibility: Convert {@link GameSession} and game-play responses to HTTP response
 * DTOs (CreateGameResponse, GameResponse, DictionaryMetrics). This improves controller
 * clarity by isolating response-shaping logic from HTTP handler code.
 *
 * <p><strong>Design note</strong>: Each method is stateless and delegates to
 * {@link DictionaryAnalytics} for metrics computation, so no state is retained.
 *
 * @author Fistral Technologies
 */
@Service
public class GameResponseShaper {
    private static final Logger logger = Logger.getLogger(GameResponseShaper.class.getName());

    /**
     * Builds the response DTO for a newly created game.
     *
     * <p>Includes the game ID, word length, max attempts, and initial dictionary metrics.
     *
     * @param session the newly created game session
     * @return the response DTO suitable for HTTP 201 response
     */
    public CreateGameResponse buildCreateGameResponse(GameSession session) {
        String gameId = session.getGameId();
        int wordLength = session.getWordGame().getDictionary().getWordLength();
        int maxAttempts = session.getMaxAttempts();

        CreateGameResponse response = new CreateGameResponse(gameId, wordLength, maxAttempts);

        // Compute and attach dictionary metrics for the initial state
        Dictionary filteredDict = session.getFilteredDictionary();
        DictionaryAnalytics analyser = new DictionaryAnalytics(filteredDict);

        CreateGameResponse.DictionaryMetrics metrics = new CreateGameResponse.DictionaryMetrics(
                filteredDict.getWordCount(),
                filteredDict.getLetterCount(),
                filteredDict.getUniqueCharacters().size(),
                filteredDict.getColumnLengths());
        metrics.setOccurrenceCountByPosition(analyser.getOccurrenceCountByPosition());
        metrics.setMostFrequentCharByPosition(analyser.getMostFrequentCharByPosition());

        response.setDictionaryMetrics(metrics);

        logger.info("Built CreateGameResponse for " + gameId);
        return response;
    }

    /**
     * Builds the response DTO after a guess is made.
     *
     * <p>Includes the game response, current/max attempts, and updated dictionary metrics.
     *
     * @param gameId the game session ID
     * @param gameResponse the result of the guess (from WordGame.guess)
     * @param session the current game session (after guess application)
     * @return the response DTO suitable for HTTP 200 response
     */
    public GameResponse buildGameResponse(String gameId, Response gameResponse, GameSession session) {
        GameResponse response = new GameResponse(
                gameId,
                gameResponse,
                session.getCurrentAttempts(),
                session.getMaxAttempts());

        // Compute and attach updated dictionary metrics
        Dictionary filteredDict = session.getFilteredDictionary();
        DictionaryAnalytics analyser = new DictionaryAnalytics(filteredDict);

        GameResponse.DictionaryMetrics metrics = new GameResponse.DictionaryMetrics(
                filteredDict.getLetterCount(),
                filteredDict.getUniqueCharacters().size(),
                filteredDict.getColumnLengths());
        metrics.setOccurrenceCountByPosition(analyser.getOccurrenceCountByPosition());
        metrics.setMostFrequentCharByPosition(analyser.getMostFrequentCharByPosition());

        response.setDictionaryMetrics(metrics);

        logger.fine("Built GameResponse for " + gameId);
        return response;
    }
}

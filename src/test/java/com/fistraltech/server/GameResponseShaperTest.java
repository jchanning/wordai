package com.fistraltech.server;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.dto.CreateGameResponse;
import com.fistraltech.server.dto.GameResponse;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;

/**
 * Tests for {@link GameResponseShaper}.
 */
@DisplayName("GameResponseShaper Tests")
class GameResponseShaperTest {

    private GameResponseShaper shaper;
    private Dictionary testDictionary;
    private Config testConfig;

    @BeforeEach
    void setUp() {
        shaper = new GameResponseShaper();

        // Build a small, deterministic test dictionary
        testDictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        words.add("stare");
        words.add("crane");
        words.add("slate");
        words.add("raise");
        testDictionary.addWords(words);

        testConfig = new Config();
        testConfig.setMaxAttempts(6);
    }

    @Test
    @DisplayName("buildCreateGameResponse includes game ID, word length, and metrics")
    void testBuildCreateGameResponse() throws InvalidWordException {
        // Arrange
        String gameId = "test-game-123";
        WordGame wordGame = new WordGame(testDictionary, testConfig);
        wordGame.setRandomTargetWord();
        GameSession session = new GameSession(gameId, wordGame, testConfig, testDictionary,
                AlgorithmRegistry.withDefaults());

        // Act
        CreateGameResponse response = shaper.buildCreateGameResponse(session);

        // Assert
        assertEquals(gameId, response.getGameId());
        assertEquals(5, response.getWordLength());
        assertEquals(6, response.getMaxAttempts());
        assertNotNull(response.getDictionaryMetrics());
        assertEquals(5, response.getDictionaryMetrics().getTotalWords());
        assertTrue(response.getDictionaryMetrics().getUniqueCharacters() > 0);
    }

    @Test
    @DisplayName("buildGameResponse includes attempt count and updated metrics")
    void testBuildGameResponse() throws InvalidWordException {
        // Arrange
        String gameId = "test-game-456";
        WordGame wordGame = new WordGame(testDictionary, testConfig);
        wordGame.setTargetWord("raise");
        GameSession session = new GameSession(gameId, wordGame, testConfig, testDictionary,
                AlgorithmRegistry.withDefaults());

        // Make a guess to generate a Response
        Response gameResponse = wordGame.guess("stare");
        assertNotNull(gameResponse);

        // Act
        GameResponse response = shaper.buildGameResponse(gameId, gameResponse, session);

        // Assert
        assertEquals(gameId, response.getGameId());
        assertEquals(1, response.getAttemptNumber());
        assertEquals(6, response.getMaxAttempts());
        assertNotNull(response.getDictionaryMetrics());
        assertTrue(response.getDictionaryMetrics().getUniqueCharacters() > 0);
    }

    @Test
    @DisplayName("buildGameResponse reflects remaining word count after filtering")
    void testBuildGameResponseAfterFiltering() throws InvalidWordException {
        // Arrange
        String gameId = "test-game-789";
        WordGame wordGame = new WordGame(testDictionary, testConfig);
        wordGame.setTargetWord("raise");
        GameSession session = new GameSession(gameId, wordGame, testConfig, testDictionary,
                AlgorithmRegistry.withDefaults());

        // Make two guesses (filter gets applied)
        Response firstGuess = wordGame.guess("stare");
        session.getWordFilter().update(firstGuess);
        firstGuess.setRemainingWordsCount(session.getRemainingWordsCount());

        Response secondGuess = wordGame.guess("crane");
        session.getWordFilter().update(secondGuess);
        secondGuess.setRemainingWordsCount(session.getRemainingWordsCount());

        // Act
        GameResponse response = shaper.buildGameResponse(gameId, secondGuess, session);

        // Assert
        assertEquals(2, response.getAttemptNumber());
        assertTrue(response.getDictionaryMetrics().getLetterCount() > 0,
            "After filtering, letter count should still be positive");
    }
}

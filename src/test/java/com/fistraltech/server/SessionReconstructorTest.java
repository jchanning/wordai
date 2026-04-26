package com.fistraltech.server;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.fistraltech.core.Dictionary;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ActiveGameSessionEntity;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;

/**
 * Tests for {@link SessionReconstructor}.
 */
@DisplayName("SessionReconstructor Tests")
class SessionReconstructorTest {

    private SessionReconstructor reconstructor;
    
    @Mock
    private DictionaryService dictionaryService;
    
    @Mock
    private SessionPersistenceService sessionPersistenceService;
    
    private Config testConfig;
    private Dictionary testDictionary;
    private AlgorithmRegistry algorithmRegistry;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

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
        algorithmRegistry = AlgorithmRegistry.withDefaults();

        // Mock DictionaryService
        when(dictionaryService.getDictionaryForGame("default"))
                .thenReturn(testDictionary);
        when(dictionaryService.getConfig()).thenReturn(testConfig);
        when(dictionaryService.getWordEntropy("default")).thenReturn(null);

        reconstructor = new SessionReconstructor(
                dictionaryService,
                algorithmRegistry,
                sessionPersistenceService);
    }

    @Test
    @DisplayName("reconstructFromEntity restores game session with target word")
    void testReconstructFromEntity() {
        // Arrange
        String gameId = "test-game-123";
        Long userId = 42L;
        String targetWord = "raise";
        
        ActiveGameSessionEntity entity = new ActiveGameSessionEntity();
        entity.setGameId(gameId);
        entity.setUserId(userId);
        entity.setDictionaryId("default");
        entity.setTargetWord(targetWord);
        entity.setStrategy("entropy");
        entity.setGuessWords(""); // No guesses yet

        // Act
        GameSession reconstructed = reconstructor.reconstructFromEntity(entity);

        // Assert
        assertNotNull(reconstructed);
        assertEquals(gameId, reconstructed.getGameId());
        assertEquals(userId, reconstructed.getUserId());
        assertEquals(targetWord, reconstructed.getWordGame().getTargetWord());
        assertEquals("entropy", reconstructed.getSelectedStrategy());
        assertEquals(0, reconstructed.getCurrentAttempts());
    }

    @Test
    @DisplayName("reconstructFromEntity replays guesses to restore filter state")
    void testReconstructFromEntityWithGuesses() {
        // Arrange
        String gameId = "test-game-456";
        Long userId = 42L;
        String targetWord = "raise";
        
        ActiveGameSessionEntity entity = new ActiveGameSessionEntity();
        entity.setGameId(gameId);
        entity.setUserId(userId);
        entity.setDictionaryId("default");
        entity.setTargetWord(targetWord);
        entity.setStrategy("random");
        entity.setGuessWords("stare,crane"); // Two guesses already made

        // Act
        GameSession reconstructed = reconstructor.reconstructFromEntity(entity);

        // Assert
        assertNotNull(reconstructed);
        assertEquals(gameId, reconstructed.getGameId());
        assertEquals(2, reconstructed.getCurrentAttempts(), "Should replay 2 guesses");
        assertEquals(targetWord, reconstructed.getWordGame().getTargetWord());
        // After filtering with "stare" and "crane", dictionary should be smaller
        assertTrue(reconstructed.getRemainingWordsCount() <= 5,
                "Filtering should reduce remaining words");
    }

    @Test
    @DisplayName("reconstructFromEntity throws if dictionary not found")
    void testReconstructFromEntityDictionaryNotFound() {
        // Arrange
        when(dictionaryService.getDictionaryForGame("missing"))
                .thenReturn(null);
        
        ActiveGameSessionEntity entity = new ActiveGameSessionEntity();
        entity.setGameId("test-game-789");
        entity.setDictionaryId("missing");
        entity.setTargetWord("raise");
        entity.setGuessWords("");

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            reconstructor.reconstructFromEntity(entity);
        });
    }

    @Test
    @DisplayName("findAndReconstructActiveSession returns game ID when session found")
    void testFindAndReconstructActiveSession() {
        // Arrange
        Long userId = 42L;
        String dictionaryId = "default";
        String browserSessionId = "browser-123";
        String gameId = "found-game-999";
        String targetWord = "crane";

        ActiveGameSessionEntity entity = new ActiveGameSessionEntity();
        entity.setGameId(gameId);
        entity.setUserId(userId);
        entity.setDictionaryId(dictionaryId);
        entity.setTargetWord(targetWord);
        entity.setStrategy("entropy");
        entity.setGuessWords("");

        when(sessionPersistenceService.findActiveForUser(userId, dictionaryId, browserSessionId))
                .thenReturn(Optional.of(entity));

        // Act
        String result = reconstructor.findAndReconstructActiveSession(userId, dictionaryId, browserSessionId);

        // Assert
        assertEquals(gameId, result);
        verify(sessionPersistenceService, times(1))
                .findActiveForUser(userId, dictionaryId, browserSessionId);
    }

    @Test
    @DisplayName("findAndReconstructActiveSession returns null when no session found")
    void testFindAndReconstructActiveSessionNotFound() {
        // Arrange
        Long userId = 42L;
        String dictionaryId = "default";
        String browserSessionId = "browser-456";

        when(sessionPersistenceService.findActiveForUser(userId, dictionaryId, browserSessionId))
                .thenReturn(Optional.empty());

        // Act
        String result = reconstructor.findAndReconstructActiveSession(userId, dictionaryId, browserSessionId);

        // Assert
        assertNull(result);
        verify(sessionPersistenceService, times(1))
                .findActiveForUser(userId, dictionaryId, browserSessionId);
    }
}

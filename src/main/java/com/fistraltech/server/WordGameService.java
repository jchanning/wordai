package com.fistraltech.server;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.fistraltech.bot.filter.Filter;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.core.WordSource;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

/**
 * Service class for managing WordAI game sessions
 */
@Service
public class WordGameService {
    private static final Logger logger = Logger.getLogger(WordGameService.class.getName());
    
    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final Dictionary dictionary;
    private final Config config;
    
    public WordGameService() throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();
        this.config = configManager.createGameConfig();
        
        if (!configManager.validateConfiguration()) {
            throw new IOException("Configuration validation failed");
        }
        
        String fileName = config.getPathToDictionaryOfAllWords();
        int wordLength = config.getWordLength();
        this.dictionary = new Dictionary(wordLength);
        
        // Filter words to only include those with correct length
        Set<String> allWords = WordSource.getWordsFromFile(fileName);
        Set<String> validWords = allWords.stream()
            .filter(word -> word.length() == wordLength)
            .collect(java.util.stream.Collectors.toSet());
            
        logger.info("Loaded " + validWords.size() + " valid words of length " + wordLength + 
                   " (filtered from " + allWords.size() + " total words)");
        
        this.dictionary.addWords(validWords);
        logger.info("WordGameService initialized with dictionary: " + fileName);
    }
    
    /**
     * Creates a new game session
     * @param targetWord Optional target word. If null, a random word will be selected
     * @param wordLength Optional word length. If null, uses default from config
     * @param dictionaryId Optional dictionary ID. If null, uses default dictionary
     * @return The game session ID
     */
    public String createGame(String targetWord, Integer wordLength, String dictionaryId) throws InvalidWordException {
        String gameId = UUID.randomUUID().toString();
        
        // Determine dictionary path and word length
        String dictionaryPath;
        int actualWordLength;
        
        if (dictionaryId != null && !dictionaryId.isEmpty()) {
            // Use specified dictionary
            dictionaryPath = config.getDictionaryPathById(dictionaryId);
            if (dictionaryPath == null) {
                logger.warning("Dictionary not found for ID: " + dictionaryId);
                throw new InvalidWordException("Dictionary not found: " + dictionaryId);
            }
            actualWordLength = config.getWordLengthForDictionary(dictionaryId);
        } else {
            // Use default dictionary
            dictionaryPath = config.getPathToDictionaryOfAllWords();
            actualWordLength = wordLength != null ? wordLength : config.getWordLength();
        }
        
        // Create dictionary for this word length
        Dictionary gameDictionary = new Dictionary(actualWordLength);
        try {
            Set<String> allWords = WordSource.getWordsFromFile(dictionaryPath);
            Set<String> validWords = allWords.stream()
                .filter(word -> word.length() == actualWordLength)
                .collect(java.util.stream.Collectors.toSet());
            gameDictionary.addWords(validWords);
            
            logger.info("Loaded " + validWords.size() + " words for game from: " + dictionaryPath);
        } catch (IOException e) {
            logger.warning("Failed to load dictionary from " + dictionaryPath);
            throw new InvalidWordException("Failed to create game with dictionary: " + dictionaryId);
        }
        
        Config gameConfig = new Config();
        gameConfig.setWordLength(actualWordLength);
        gameConfig.setMaxAttempts(config.getMaxAttempts());
        gameConfig.setPathToDictionaryOfAllWords(dictionaryPath);
        gameConfig.setPathToDictionaryOfGameWords(dictionaryPath);
        WordGame wordGame = new WordGame(gameDictionary, gameConfig);
        
        // Set target word
        if (targetWord != null) {
            wordGame.setTargetWord(targetWord.toLowerCase());
        } else {
            wordGame.setRandomTargetWord();
        }
        
        GameSession session = new GameSession(gameId, wordGame, gameConfig, gameDictionary);
        activeSessions.put(gameId, session);
        
        logger.info("Created new game session: " + gameId);
        return gameId;
    }
    
    /**
     * Creates a new game session (backward compatibility)
     * @param targetWord Optional target word. If null, a random word will be selected
     * @param wordLength Optional word length. If null, uses default from config
     * @return The game session ID
     */
    public String createGame(String targetWord, Integer wordLength) throws InvalidWordException {
        return createGame(targetWord, wordLength, null);
    }
    
    /**
     * Makes a guess for the specified game session
     * @param gameId The game session ID
     * @param word The guessed word
     * @return The response containing the game state
     */
    public Response makeGuess(String gameId, String word) throws InvalidWordException {
        GameSession session = activeSessions.get(gameId);
        if (session == null) {
            throw new InvalidWordException("Game session not found: " + gameId);
        }
        
        if (session.isGameEnded()) {
            throw new InvalidWordException("Game has already ended");
        }
        
        Response response = session.getWordGame().guess(word.toLowerCase());
        
        // Update the filter based on the response
        updateFilterBasedOnResponse(session.getWordFilter(), response);
        
        // Set remaining words count in response
        response.setRemainingWordsCount(session.getRemainingWordsCount());
        
        // Check if game should end
        if (response.getWinner() || session.isMaxAttemptsReached()) {
            session.setGameEnded(true);
            logger.info("Game session ended: " + gameId + " (Winner: " + response.getWinner() + ")");
        }
        
        return response;
    }
    
    /**
     * Gets the game session by ID
     * @param gameId The game session ID
     * @return The game session or null if not found
     */
    public GameSession getGameSession(String gameId) {
        return activeSessions.get(gameId);
    }
    
    /**
     * Removes a game session (cleanup)
     * @param gameId The game session ID
     */
    public void removeGameSession(String gameId) {
        activeSessions.remove(gameId);
        logger.info("Removed game session: " + gameId);
    }
    
    /**
     * Gets the number of active game sessions
     * @return The number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Updates the filter based on the response from a guess
     * @param filter The filter to update
     * @param guessedWord The word that was guessed
     * @param response The response from the game
     */
    private void updateFilterBasedOnResponse(Filter filter, Response response) {
        // Use the Filter's built-in update method which handles all the complex logic
        // including duplicate letters, excess markers, and proper counting
        filter.update(response);
    }
}

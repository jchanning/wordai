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
 * Service layer for creating and managing WordAI game sessions.
 *
 * <p>This class is the core of the Spring Boot API implementation:
 * it owns the in-memory session map, creates {@link WordGame} instances, loads dictionaries,
 * applies guesses, and exposes a server-side full-dictionary analysis entry point.
 *
 * <p><strong>Conceptual model</strong>
 * <ul>
 *   <li>A {@link GameSession} represents one active game identified by a UUID-like {@code gameId}.</li>
 *   <li>Sessions are stored in-memory in {@code activeSessions}; restarting the server clears them.</li>
 *   <li>Each session owns its own {@link WordGame}, a {@link Filter}, and its dictionary/config context.</li>
 * </ul>
 *
 * <p><strong>Thread safety</strong>
 * <ul>
 *   <li>The session map uses {@link java.util.concurrent.ConcurrentHashMap} for safe concurrent access.</li>
 *   <li>Individual sessions ({@link WordGame}/{@link Filter}) are not designed for concurrent mutation.
 *       The REST API assumes a single caller at a time per {@code gameId}.</li>
 * </ul>
 *
 * <p><strong>Error handling</strong>: service methods throw {@link InvalidWordException} for invalid
 * input and {@link IOException} when dictionaries/config cannot be loaded.
 *
 * @author Fistral Technologies
 * @see com.fistraltech.server.controller.WordGameController
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
     * Creates a new game session.
     *
     * <p>If {@code dictionaryId} is provided, the dictionary path and word length are derived from
     * configuration. Otherwise the default dictionary and/or requested word length is used.
     *
     * @param targetWord optional explicit target word (lower-cased before use). If {@code null}, a random
     *                  target is chosen from the dictionary.
     * @param wordLength optional word length. Only used when {@code dictionaryId} is not supplied.
     * @param dictionaryId optional dictionary identifier from configuration.
     * @return the newly created session id.
     * @throws InvalidWordException if the dictionary id is invalid, the dictionary cannot be loaded,
     *                              or the provided target word is invalid.
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
     * Loads a full dictionary by id.
     *
     * <p>This is used by API endpoints that need the raw word list (e.g. analysis) rather than a game session.
     */
    public Dictionary loadDictionary(String dictionaryId) throws IOException, InvalidWordException {
        // Determine dictionary path and word length
        String dictionaryPath;
        int wordLength;
        
        if (dictionaryId != null && !dictionaryId.isEmpty()) {
            // Use specified dictionary
            dictionaryPath = config.getDictionaryPathById(dictionaryId);
            if (dictionaryPath == null) {
                throw new InvalidWordException("Dictionary not found: " + dictionaryId);
            }
            wordLength = config.getWordLengthForDictionary(dictionaryId);
        } else {
            // Use default dictionary
            dictionaryPath = config.getPathToDictionaryOfAllWords();
            wordLength = config.getWordLength();
        }
        
        // Create and load dictionary
        Dictionary dictionary = new Dictionary(wordLength);
        Set<String> allWords = WordSource.getWordsFromFile(dictionaryPath);
        Set<String> validWords = allWords.stream()
            .filter(word -> word.length() == wordLength)
            .collect(java.util.stream.Collectors.toSet());
        dictionary.addWords(validWords);
        
        logger.info("Loaded dictionary with " + validWords.size() + " words from: " + dictionaryPath);
        return dictionary;
    }
    
    /**
     * Applies a guess to the specified session and updates its filter state.
     *
     * <p>The returned {@link Response} includes remaining-words count derived from the session filter.
     *
     * @throws InvalidWordException if the session does not exist, the game already ended, or the guess is invalid.
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
     * Runs a complete dictionary analysis using the specified algorithm.
     *
     * <p>This endpoint is server-side (one request performs the full run). It is distinct from the
     * client-driven analysis loop used by the web UI.
     *
     * @param algorithm algorithm id (e.g. {@code RANDOM}, {@code ENTROPY}).
     * @param dictionaryId dictionary id to analyse.
     * @param maxGames optional cap on games to run; {@code null} means "all".
     * @return an {@link com.fistraltech.server.dto.AnalysisResponse} containing summary and per-game detail.
     */
    public com.fistraltech.server.dto.AnalysisResponse runAnalysis(
            String algorithm, String dictionaryId, Integer maxGames) throws Exception {
        
        // Load dictionary
        String dictionaryPath = config.getDictionaryPathById(dictionaryId != null ? dictionaryId : "default");
        int wordLength = config.getWordLengthForDictionary(dictionaryId != null ? dictionaryId : "default");
        
        Dictionary analysisDictionary = new Dictionary(wordLength);
        try {
            Set<String> allWords = WordSource.getWordsFromFile(dictionaryPath);
            Set<String> validWords = allWords.stream()
                .filter(word -> word.length() == wordLength)
                .collect(java.util.stream.Collectors.toSet());
            analysisDictionary.addWords(validWords);
            logger.info("Loaded " + validWords.size() + " words for analysis");
        } catch (IOException e) {
            throw new Exception("Failed to load dictionary for analysis: " + e.getMessage(), e);
        }
        
        // Create game and player with specified algorithm
        WordGame game = new WordGame(analysisDictionary, analysisDictionary, config);
        com.fistraltech.bot.selection.SelectionAlgo selectionAlgo = createSelectionAlgorithm(algorithm, analysisDictionary);
        com.fistraltech.bot.WordGamePlayer player = new com.fistraltech.bot.WordGamePlayer(game, selectionAlgo);
        
        // Run analysis
        com.fistraltech.analysis.PlayerAnalyser analyser = 
            new com.fistraltech.analysis.PlayerAnalyser(player, false, null);
        
        return analyser.analyseGamePlay(maxGames);
    }
    
    /**
     * Factory for mapping API algorithm ids to concrete {@link com.fistraltech.bot.selection.SelectionAlgo}.
     */
    private com.fistraltech.bot.selection.SelectionAlgo createSelectionAlgorithm(String algorithmId, Dictionary dictionary) {
        if (algorithmId == null) {
            algorithmId = "RANDOM";
        }
        
        switch (algorithmId.toUpperCase()) {
            case "ENTROPY":
                return new com.fistraltech.bot.selection.SelectMaximumEntropy(dictionary);
            case "MOST_COMMON_LETTERS":
                return new com.fistraltech.bot.selection.SelectMostCommonLetters(dictionary);
            case "MINIMISE_COLUMN_LENGTHS":
                return new com.fistraltech.bot.selection.MinimiseColumnLengths(dictionary);
            case "DICTIONARY_REDUCTION":
                return new com.fistraltech.bot.selection.SelectMaximumDictionaryReduction(dictionary);
            case "RANDOM":
            default:
                return new com.fistraltech.bot.selection.SelectRandom(dictionary);
        }
    }
    
    /**
     * Updates the filter based on a guess response.
     *
     * <p>The filter implementation handles duplicate letters, excess markers, and occurrence rules.
     */
    private void updateFilterBasedOnResponse(Filter filter, Response response) {
        // Use the Filter's built-in update method which handles all the complex logic
        // including duplicate letters, excess markers, and proper counting
        filter.update(response);
    }
}

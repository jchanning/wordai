package com.fistraltech.server.model;

import java.util.logging.Logger;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.bot.filter.Filter;
import com.fistraltech.bot.selection.SelectMaximumDictionaryReduction;
import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * In-memory representation of a single active game.
 *
 * <p>A {@link GameSession} binds together:
 * <ul>
 *   <li>the active {@link WordGame} instance (target word + guess history)</li>
 *   <li>a cumulative {@link Filter} that encodes constraints derived from guesses</li>
 *   <li>the original dictionary/config context used to create the game</li>
 * </ul>
 *
 * <p><strong>Lifecycle</strong>
 * <ol>
 *   <li>Created by {@link com.fistraltech.server.WordGameService#createGame(String, Integer, String)}.</li>
 *   <li>Mutated by guesses via {@link com.fistraltech.server.WordGameService#makeGuess(String, String)}.</li>
 *   <li>Deleted via {@link com.fistraltech.server.WordGameService#removeGameSession(String)}.</li>
 * </ol>
 *
 * <p><strong>Strategy</strong>
 * <ul>
 *   <li>The API stores a strategy id string (default {@code RANDOM}).</li>
 *   <li>{@link #suggestWord()} uses the current strategy to propose a next guess based on the filtered dictionary.</li>
 * </ul>
 *
 * <p><strong>Thread safety</strong>: sessions are not thread-safe; treat each {@code gameId} as single-writer.
 *
 * @author Fistral Technologies
 */
public class GameSession {
    private static final Logger logger = Logger.getLogger(GameSession.class.getName());
    
    private final String gameId;
    private final WordGame wordGame;
    private final Config config;
    private final Dictionary originalDictionary;
    private final Filter wordFilter;
    private boolean gameEnded = false;
    // Only valid in auto-play mode. In user interactive mode, strategy is chosen per guess.
    private String selectedStrategy = "RANDOM"; // Default strategy
    
    // Cached WordEntropy from DictionaryService for fast entropy-based suggestions
    private WordEntropy cachedWordEntropy;
    
    public GameSession(String gameId, WordGame wordGame, Config config, Dictionary dictionary) {
        this.gameId = gameId;
        this.wordGame = wordGame;
        this.config = config;
        this.originalDictionary = dictionary;
        this.wordFilter = new Filter(dictionary.getWordLength());
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public WordGame getWordGame() {
        return wordGame;
    }
    
    public Config getConfig() {
        return config;
    }
    
    public boolean isGameEnded() {
        return gameEnded;
    }
    
    public void setGameEnded(boolean gameEnded) {
        this.gameEnded = gameEnded;
    }
    
    public int getCurrentAttempts() {
        return wordGame.getNoOfAttempts();
    }
    
    public int getMaxAttempts() {
        return config.getMaxAttempts();
    }
    
    public boolean isMaxAttemptsReached() {
        return getCurrentAttempts() >= getMaxAttempts();
    }
    
    public Filter getWordFilter() {
        return wordFilter;
    }
    
    public int getRemainingWordsCount() {
        Dictionary filteredDictionary = wordFilter.apply(originalDictionary);
        return filteredDictionary.getWordCount();
    }
    
    public int getTotalWordsCount() {
        return originalDictionary.getWordCount();
    }
    
    public String getSelectedStrategy() {
        return selectedStrategy;
    }
    
    public void setSelectedStrategy(String strategy) {
        this.selectedStrategy = strategy;
    }
    
    /**
     * Sets the cached WordEntropy from DictionaryService.
     * This enables fast entropy-based suggestions using pre-computed values.
     * 
     * @param wordEntropy the pre-computed WordEntropy instance
     */
    public void setCachedWordEntropy(WordEntropy wordEntropy) {
        this.cachedWordEntropy = wordEntropy;
    }
    
    /**
     * Gets the current filtered dictionary based on all guesses made so far.
     */
    public Dictionary getFilteredDictionary() {
        return wordFilter.apply(originalDictionary);
    }
    
    /**
     * Suggests a next word based on {@link #selectedStrategy}.
     *
     * <p>For ENTROPY, DICTIONARY_REDUCTION, and MINIMISE_COLUMN_LENGTHS strategies:
     * <ul>
     *   <li>First guess (unfiltered dictionary): uses pre-computed cached values</li>
     *   <li>Subsequent guesses (filtered dictionary): recomputes values based on remaining words</li>
     * </ul>
     *
     * <p><strong>Why recomputation is required:</strong> Entropy (and similar metrics) measures
     * how a candidate word partitions the <em>current</em> dictionary into response buckets.
     * When the dictionary changes (words are filtered out after each guess), the bucket
     * distributions change, so the metric values must be recomputed. Cached values are only
     * valid for the original unfiltered dictionary.
     *
     * @return a suggested word, or {@code null} if no valid words remain
     */
    public String suggestWord() {
        Dictionary filteredDictionary = getFilteredDictionary();
        
        if (filteredDictionary.getWordCount() == 0) {
            return null;
        }
        
        String strategyUpper = selectedStrategy.toUpperCase();
        
        // Cached values are only valid when the dictionary is unfiltered (first guess).
        // After filtering, the dictionary composition changes, so entropy/reduction/column-length
        // values must be recomputed - they depend on how the word partitions the CURRENT dictionary.
        boolean isUnfilteredDictionary = filteredDictionary.getWordCount() == originalDictionary.getWordCount();
        
        if (cachedWordEntropy != null && isUnfilteredDictionary) {
            switch (strategyUpper) {
                case "ENTROPY":
                case "MAXIMUM_ENTROPY":
                    logger.fine(() -> "Using cached WordEntropy for first ENTROPY suggestion (full dict)");
                    return cachedWordEntropy.getMaximumEntropyWord(filteredDictionary.getMasterSetOfWords());
                    
                case "DICTIONARY_REDUCTION":
                    logger.fine(() -> "Using cached WordEntropy for first DICTIONARY_REDUCTION suggestion (full dict)");
                    return cachedWordEntropy.getWordWithMaximumReduction(filteredDictionary.getMasterSetOfWords());
                    
                case "MINIMISE_COLUMN_LENGTHS":
                    logger.fine(() -> "Using cached WordEntropy for first MINIMISE_COLUMN_LENGTHS suggestion (full dict)");
                    return cachedWordEntropy.getWordWithMinimumColumnLength(filteredDictionary.getMasterSetOfWords());
            }
        }
        
        // Dictionary has been filtered - must recompute values based on remaining words.
        // This is required for correctness: the metrics depend on the current dictionary.
        SelectionAlgo algo;
        switch (strategyUpper) {
            case "ENTROPY":
            case "MAXIMUM_ENTROPY":
                logger.fine(() -> "Recomputing entropy for dictionary of " + filteredDictionary.getWordCount() + " words");
                algo = new SelectMaximumEntropy(filteredDictionary);
                break;
            case "MINIMISE_COLUMN_LENGTHS":
                logger.fine(() -> "Recomputing column lengths for dictionary of " + filteredDictionary.getWordCount() + " words");
                algo = new com.fistraltech.bot.selection.MinimiseColumnLengths(filteredDictionary);
                break;
            case "DICTIONARY_REDUCTION":
                logger.fine(() -> "Recomputing dictionary reduction for dictionary of " + filteredDictionary.getWordCount() + " words");
                algo = new SelectMaximumDictionaryReduction(filteredDictionary);
                break;
            case "RANDOM":
            default:
                algo = new SelectRandom(filteredDictionary);
                break;
        }
        
        // Create an empty response for the first guess
        Response emptyResponse = new Response("");
        return algo.selectWord(emptyResponse);
    }
}
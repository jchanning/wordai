package com.fistraltech.server.model;

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
    private final String gameId;
    private final WordGame wordGame;
    private final Config config;
    private final Dictionary originalDictionary;
    private final Filter wordFilter;
    private boolean gameEnded = false;
    // Only valid in auto-play mode. In user interactive mode, strategy is chosen per guess.
    private String selectedStrategy = "RANDOM"; // Default strategy
    
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
     * Gets the current filtered dictionary based on all guesses made so far.
     */
    public Dictionary getFilteredDictionary() {
        return wordFilter.apply(originalDictionary);
    }
    
    /**
     * Suggests a next word based on {@link #selectedStrategy}.
     *
     * @return a suggested word, or {@code null} if no valid words remain
     */
    public String suggestWord() {
        Dictionary filteredDictionary = getFilteredDictionary();
        
        if (filteredDictionary.getWordCount() == 0) {
            return null;
        }
        
        SelectionAlgo algo;
        switch (selectedStrategy.toUpperCase()) {
            case "ENTROPY":
            case "MAXIMUM_ENTROPY":
                algo = new SelectMaximumEntropy(filteredDictionary);
                break;
            case "MINIMISE_COLUMN_LENGTHS":
                algo = new com.fistraltech.bot.selection.MinimiseColumnLengths(filteredDictionary);
                break;
            case "DICTIONARY_REDUCTION":
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
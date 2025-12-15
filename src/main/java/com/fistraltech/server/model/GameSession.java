package com.fistraltech.server.model;

import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * Represents a game session that can be stored and managed by the server
 */
public class GameSession {
    private final String gameId;
    private final WordGame wordGame;
    private final Config config;
    private final Dictionary originalDictionary;
    private final com.fistraltech.bot.filter.Filter wordFilter;
    private boolean gameEnded = false;
    private String selectedStrategy = "RANDOM"; // Default strategy
    
    public GameSession(String gameId, WordGame wordGame, Config config, Dictionary dictionary) {
        this.gameId = gameId;
        this.wordGame = wordGame;
        this.config = config;
        this.originalDictionary = dictionary;
        this.wordFilter = new com.fistraltech.bot.filter.Filter(dictionary.getWordLength());
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
    
    public com.fistraltech.bot.filter.Filter getWordFilter() {
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
     * Get the current filtered dictionary based on guesses made so far
     */
    public Dictionary getFilteredDictionary() {
        return wordFilter.apply(originalDictionary);
    }
    
    /**
     * Suggests a word based on the selected strategy
     * @return A suggested word, or null if no valid words remain
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
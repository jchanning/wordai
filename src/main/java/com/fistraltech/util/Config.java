package com.fistraltech.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fistraltech.server.dto.DictionaryOption;

public class Config {

    private int wordLength;
    private int maxAttempts = 6;
    private int minWordLength = 4;
    private int maxWordLength = 7;
    private boolean hardMode;
    private boolean showHints;
    private String pathToDictionaryOfAllWords;
    private String pathToDictionaryOfGameWords;
    
    // Additional configuration properties
    private String outputBasePath;
    private int simulationIterations = 1;
    private List<DictionaryOption> availableDictionaries;
    private Map<String, DictionaryOption> dictionaryOptionsMap = new HashMap<>();

    public String getPathToDictionaryOfAllWords() {
        return pathToDictionaryOfAllWords;
    }

    public void setPathToDictionaryOfAllWords(String pathToDictionaryOfAllWords) {
        this.pathToDictionaryOfAllWords = pathToDictionaryOfAllWords;
    }

    public String getPathToDictionaryOfGameWords() {
        return pathToDictionaryOfGameWords;
    }

    public void setPathToDictionaryOfGameWords(String pathToDictionaryOfGameWords) {
        this.pathToDictionaryOfGameWords = pathToDictionaryOfGameWords;
    }

    public int getWordLength() {
        return wordLength;
    }

    public void setWordLength(int wordLength) {
        this.wordLength = wordLength;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
    }

    public boolean isShowHints() {
        return showHints;
    }

    public void setShowHints(boolean showHints) {
        this.showHints = showHints;
    }
    
    public int getMinWordLength() {
        return minWordLength;
    }
    
    public void setMinWordLength(int minWordLength) {
        this.minWordLength = minWordLength;
    }
    
    public int getMaxWordLength() {
        return maxWordLength;
    }
    
    public void setMaxWordLength(int maxWordLength) {
        this.maxWordLength = maxWordLength;
    }
    
    public String getOutputBasePath() {
        return outputBasePath;
    }
    
    public void setOutputBasePath(String outputBasePath) {
        this.outputBasePath = outputBasePath;
    }
    
    public int getSimulationIterations() {
        return simulationIterations;
    }
    
    public void setSimulationIterations(int simulationIterations) {
        this.simulationIterations = simulationIterations;
    }
    
    public List<DictionaryOption> getAvailableDictionaries() {
        return availableDictionaries;
    }
    
    public void setAvailableDictionaries(List<DictionaryOption> availableDictionaries) {
        this.availableDictionaries = availableDictionaries;
        // Build map for quick lookup by ID
        dictionaryOptionsMap.clear();
        if (availableDictionaries != null) {
            for (DictionaryOption option : availableDictionaries) {
                dictionaryOptionsMap.put(option.getId(), option);
            }
        }
    }
    
    /**
     * Get dictionary path by ID
     */
    public String getDictionaryPathById(String id) {
        DictionaryOption option = dictionaryOptionsMap.get(id);
        return option != null ? option.getResolvedPath() : null;
    }
    
    /**
     * Get word length for a specific dictionary
     */
    public int getWordLengthForDictionary(String dictionaryId) {
        DictionaryOption option = dictionaryOptionsMap.get(dictionaryId);
        return option != null ? option.getWordLength() : wordLength;
    }
}

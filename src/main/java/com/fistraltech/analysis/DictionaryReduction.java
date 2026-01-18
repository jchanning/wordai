package com.fistraltech.analysis;

import java.util.Set;

import com.fistraltech.core.Dictionary;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

/**
 * Calculates dictionary reduction metrics for word selection strategies.
 * 
 * <p>This class helps identify words that, when guessed, are expected to 
 * eliminate the most possible target words from consideration.
 * 
 * <p><strong>Phase 4 Optimization:</strong>
 * <ul>
 *   <li>Uses pre-computed ResponseMatrix instead of runtime response calculation</li>
 *   <li>Replaces HashMap/HashSet bucket grouping with array-based counting</li>
 *   <li>Supports filtered target sets for efficient late-game computation</li>
 * </ul>
 */
public class DictionaryReduction {
    
    /** Pre-computed WordEntropy with ResponseMatrix */
    private final WordEntropy wordEntropy;
    
    /** Direct access to the response matrix */
    private final ResponseMatrix responseMatrix;
    
    /** The master dictionary */
    private final Dictionary dictionary;
    
    /** Threshold for when to use lazy computation vs cached values */
    private static final double LAZY_THRESHOLD = 0.8;

    public DictionaryReduction(Dictionary dictionary) {
        this.dictionary = dictionary;
        Config config = ConfigManager.getInstance().createGameConfig();
        this.wordEntropy = new WordEntropy(dictionary, config, true);
        this.responseMatrix = wordEntropy.getResponseMatrix();
    }
    
    /**
     * Creates a DictionaryReduction instance sharing a WordEntropy instance.
     * More efficient when WordEntropy is already available.
     * 
     * @param dictionary the dictionary
     * @param wordEntropy pre-computed WordEntropy to share
     */
    public DictionaryReduction(Dictionary dictionary, WordEntropy wordEntropy) {
        this.dictionary = dictionary;
        this.wordEntropy = wordEntropy;
        this.responseMatrix = wordEntropy.getResponseMatrix();
    }

    /**
     * Finds the word with maximum dictionary reduction (minimum expected remaining words).
     * Uses pre-computed values for efficiency.
     * 
     * @return the word that minimizes expected remaining dictionary size
     */
    public String getWordWithMaximumReduction() {
        return wordEntropy.getWordWithMaximumReduction();
    }
    
    /**
     * Finds the word with maximum dictionary reduction from a filtered set.
     * Uses cached values when the filter hasn't reduced the dictionary significantly.
     * 
     * @param filteredWords the set of remaining valid target words
     * @return the word that minimizes expected remaining dictionary size
     */
    public String getWordWithMaximumReduction(Set<String> filteredWords) {
        if (filteredWords == null || filteredWords.isEmpty()) {
            return null;
        }
        
        // If only one word left, return it
        if (filteredWords.size() == 1) {
            return filteredWords.iterator().next();
        }
        
        // If the filtered dictionary is still most of the original, use cached values
        double filterRatio = (double) filteredWords.size() / dictionary.getWordCount();
        if (filterRatio >= LAZY_THRESHOLD) {
            return wordEntropy.getWordWithMaximumReduction(filteredWords);
        }
        
        // For significantly filtered dictionaries, use lazy computation
        return findMaxReductionWordLazy(filteredWords);
    }
    
    /**
     * Finds the word with maximum reduction using lazy computation.
     * Computes reduction against the actual filtered target set.
     * 
     * @param targetWords the remaining possible target words
     * @return the word with maximum dictionary reduction
     */
    private String findMaxReductionWordLazy(Set<String> targetWords) {
        // Convert target words to IDs
        int[] targetIds = new int[targetWords.size()];
        int targetCount = 0;
        for (String word : targetWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                targetIds[targetCount++] = id;
            }
        }
        
        // Use all words from master dictionary as candidates
        int wordCount = responseMatrix.getWordCount();
        int[] candidateIds = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            candidateIds[i] = i;
        }
        
        // Find maximum reduction word
        int bestId = responseMatrix.findMaxReductionWordId(
            candidateIds, wordCount, targetIds, targetCount);
        
        return responseMatrix.getWord(bestId);
    }
    
    /**
     * Calculates the expected (average) dictionary size after using the given guess word.
     * Uses the pre-computed ResponseMatrix for efficiency.
     * 
     * @param guessWord the word to evaluate
     * @return expected remaining dictionary size (lower is better)
     */
    public double calculateAverageDictionarySize(String guessWord) {
        int guessId = responseMatrix.getWordId(guessWord);
        if (guessId < 0) {
            return Double.MAX_VALUE;
        }
        return responseMatrix.computeDictionaryReduction(guessId);
    }
    
    /**
     * Calculates the expected dictionary size for a guess against a filtered target set.
     * 
     * @param guessWord the word to evaluate
     * @param targetWords the set of remaining possible targets
     * @return expected remaining dictionary size (lower is better)
     */
    public double calculateAverageDictionarySize(String guessWord, Set<String> targetWords) {
        int guessId = responseMatrix.getWordId(guessWord);
        if (guessId < 0) {
            return Double.MAX_VALUE;
        }
        
        // Convert target words to IDs
        int[] targetIds = new int[targetWords.size()];
        int targetCount = 0;
        for (String word : targetWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                targetIds[targetCount++] = id;
            }
        }
        
        return responseMatrix.computeDictionaryReduction(guessId, targetIds, targetCount);
    }
}

package com.fistraltech.bot.selection;

import java.util.Set;

import com.fistraltech.analysis.ResponseMatrix;
import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

/**
 * This is a variation of the SelectMaximumEntropy algorithm. The target function looks to 
 * minimise the column length (i.e. number of valid letter choices, across the positions in
 * the word) rather than maximise entropy. 
 * 
 * In many cases, a single column may have a very high number of remaining valid letters, which 
 * can lead to a loss. Eliminating this risk is the target of this algorithm. There are other 
 * techniques that can address this but it involves guessing words that are known to be
 * wrong.
 *  
 * This strategy calculates, for each guess word, the number of remaining possible
 * letters by positions for each target word in the dictionary. It then calculates the average across
 * the target words. Note variants on this strategy would focus on the minimum of maximum column 
 * lengths which would identify the best or worst case scenarios.
 * 
 * Although the total number of possible words is the multiple of the column count from p0 to pn,
 * the majority of combinations are invalid (e.g. ABAAB, DDXXA, etc. are not in the English language 
 * dictionary). Using a sum is assumed to give a better target to minimise, but this remains to be 
 * proven.
 *  
 * <p><strong>Phase 4 Optimization:</strong>
 * <ul>
 *   <li>Uses pre-computed ResponseMatrix from WordEntropy for column length calculation</li>
 *   <li>Replaces HashSet-based letter tracking with bit manipulation (26 bits per position)</li>
 *   <li>First guess: Uses cached column length from full dictionary pre-computation</li>
 *   <li>Subsequent guesses: Uses ResponseMatrix.findMinColumnLengthWordId() against filtered targets</li>
 * </ul>
 */
public class MinimiseColumnLengths extends SelectionAlgo {
    
    /** Pre-computed entropy analyzer with ResponseMatrix for the full dictionary */
    private final WordEntropy wordEntropy;
    
    /** Direct access to the response matrix for optimized operations */
    private final ResponseMatrix responseMatrix;
    
    /** The master dictionary for comparison */
    private final Dictionary masterDictionary;
    
    /** Threshold for when to use lazy computation vs cached values */
    private static final double LAZY_THRESHOLD = 0.8;

    public MinimiseColumnLengths(Dictionary dictionary) {
        super(dictionary);
        this.masterDictionary = dictionary;
        // Pre-compute WordEntropy which includes column length cache
        Config config = ConfigManager.getInstance().createGameConfig();
        this.wordEntropy = new WordEntropy(dictionary, config, true);
        this.responseMatrix = wordEntropy.getResponseMatrix();
        setAlgoName("MinimiseColumnLengths");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary filteredDictionary) {
        // Get the filtered word set
        Set<String> filteredWords = filteredDictionary.getMasterSetOfWords();
        
        // If only one word left, return it
        if (filteredWords.size() == 1) {
            return filteredWords.iterator().next();
        }
        
        // If this is the first guess (empty response), use pre-computed min column length
        if (lastResponse == null || lastResponse.getWord() == null || lastResponse.getWord().isEmpty()) {
            return wordEntropy.getWordWithMinimumColumnLength(filteredWords);
        }
        
        // If the filtered dictionary is still most of the original, use cached values
        double filterRatio = (double) filteredWords.size() / masterDictionary.getWordCount();
        if (filterRatio >= LAZY_THRESHOLD) {
            // Use cached column length values - just find min among remaining words
            return wordEntropy.getWordWithMinimumColumnLength(filteredWords);
        }
        
        // For significantly filtered dictionaries, use lazy computation with ResponseMatrix
        return findMinColumnLengthWordLazy(filteredWords);
    }
    
    /**
     * Finds the word with minimum expected column length using lazy computation.
     * Computes column length against the actual filtered target set.
     * 
     * @param targetWords the remaining possible target words
     * @return the word with minimum expected column length
     */
    private String findMinColumnLengthWordLazy(Set<String> targetWords) {
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
        // (we might want to guess outside the remaining targets)
        int wordCount = responseMatrix.getWordCount();
        int[] candidateIds = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            candidateIds[i] = i;
        }
        
        // Find minimum column length word
        int bestId = responseMatrix.findMinColumnLengthWordId(
            candidateIds, wordCount, targetIds, targetCount);
        
        return responseMatrix.getWord(bestId);
    }
}

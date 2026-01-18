package com.fistraltech.bot.selection;

import java.util.Set;

import com.fistraltech.analysis.DictionaryReduction;
import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

/**
 * Selection algorithm that chooses the next guess by maximizing dictionary reduction.
 * 
 * <p>This strategy selects the word that minimizes the expected number of remaining
 * valid words after the response is received. Lower expected remaining words means
 * faster convergence to the solution.
 * 
 * <p><strong>Phase 4 Optimization:</strong>
 * <ul>
 *   <li>Pre-computes dictionary reduction for the full dictionary once at construction</li>
 *   <li>First guess: Uses pre-computed maximum reduction word</li>
 *   <li>Subsequent guesses: Uses lazy computation against filtered target set</li>
 *   <li>Shares WordEntropy/ResponseMatrix to avoid redundant computation</li>
 * </ul>
 * 
 * @see DictionaryReduction
 */
public class SelectMaximumDictionaryReduction extends SelectionAlgo {
    
    /** Pre-computed WordEntropy for the full dictionary */
    private final WordEntropy wordEntropy;
    
    /** Dictionary reduction calculator sharing the WordEntropy */
    private final DictionaryReduction dictionaryReduction;
    
    /** The master dictionary for comparison */
    private final Dictionary masterDictionary;
    
    public SelectMaximumDictionaryReduction(Dictionary dictionary) {
        super(dictionary);
        this.masterDictionary = dictionary;
        Config config = ConfigManager.getInstance().createGameConfig();
        this.wordEntropy = new WordEntropy(dictionary, config, true);
        this.dictionaryReduction = new DictionaryReduction(dictionary, wordEntropy);
        setAlgoName("SelectMaximumDictionaryReduction");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary filteredDictionary) {
        // Get the filtered word set
        Set<String> filteredWords = filteredDictionary.getMasterSetOfWords();
        
        // If only one word left, return it
        if (filteredWords.size() == 1) {
            return filteredWords.iterator().next();
        }
        
        // If this is the first guess (empty response), use pre-computed max reduction
        if (lastResponse == null || lastResponse.getWord() == null || lastResponse.getWord().isEmpty()) {
            return dictionaryReduction.getWordWithMaximumReduction();
        }
        
        // Use filtered computation for subsequent guesses
        return dictionaryReduction.getWordWithMaximumReduction(filteredWords);
    }
}

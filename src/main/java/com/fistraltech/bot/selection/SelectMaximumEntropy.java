package com.fistraltech.bot.selection;

import java.util.Set;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Selection algorithm that chooses the next guess by maximizing expected information gain (entropy).
 *
 * <p>This strategy relies on {@link WordEntropy}, which computes response buckets for candidate guesses
 * against the current dictionary and selects the word with the highest entropy.
 *
 * <p><strong>Conceptual model</strong>
 * <ul>
 *   <li>For a candidate guess, partition remaining targets into buckets by response pattern.</li>
 *   <li>Compute entropy $-\sum p \log_2 p$ over bucket probabilities.</li>
 *   <li>Select the guess with maximum entropy (most informative on average).</li>
 * </ul>
 *
 * <p><strong>Usage</strong>
 * <pre>{@code
 * Dictionary dictionary = ...;
 * SelectionAlgo algo = new SelectMaximumEntropy(dictionary);
 * String guess = algo.selectWord(new Response(""));
 * }</pre>
 *
 * <p><strong>Phase 4 Optimization:</strong>
 * <ul>
 *   <li>Pre-computes entropy for the full dictionary once at construction</li>
 *   <li>First guess: Uses pre-computed maximum entropy word</li>
 *   <li>Subsequent guesses: Uses lazy computation against filtered target set</li>
 *   <li>Falls back to cached values when filter hasn't reduced dictionary much</li>
 * </ul>
 *
 * <p><strong>Performance</strong>: typically the most expensive strategy; it may evaluate many candidate
 * words and bucket distributions. Phase 4 optimizations reduce this by using pre-computed values
 * and lazy evaluation.
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 * @see WordEntropy
 */
public class SelectMaximumEntropy extends SelectionAlgo {
    
    /** Pre-computed entropy analyzer for the full dictionary */
    private final WordEntropy wordEntropy;
    
    /** The master dictionary for comparison */
    private final Dictionary masterDictionary;
    
    /** Threshold for when to use lazy computation vs cached values */
    private static final double LAZY_THRESHOLD = 0.8;

    public SelectMaximumEntropy(Dictionary dictionary) {
        super(dictionary);
        this.masterDictionary = dictionary;
        // Pre-compute entropy for the full dictionary once
        this.wordEntropy = new WordEntropy(dictionary);
        setAlgoName("SelectMaximumEntropy");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary filteredDictionary) {
        // If this is the first guess (empty response), use pre-computed max entropy
        if (lastResponse == null || lastResponse.getWord() == null || lastResponse.getWord().isEmpty()) {
            return wordEntropy.getMaximumEntropyWord();
        }
        
        // Get the filtered word set
        Set<String> filteredWords = filteredDictionary.getMasterSetOfWords();
        
        // If only one word left, return it
        if (filteredWords.size() == 1) {
            return filteredWords.iterator().next();
        }
        
        // If the filtered dictionary is still most of the original, use cached values
        double filterRatio = (double) filteredWords.size() / masterDictionary.getWordCount();
        if (filterRatio >= LAZY_THRESHOLD) {
            // Use cached entropy values - just find max among remaining words
            return wordEntropy.getMaximumEntropyWord(filteredWords);
        }
        
        // For significantly filtered dictionaries, use lazy computation
        // This computes entropy against the actual filtered target set
        Set<String> candidateWords = masterDictionary.getMasterSetOfWords();
        return wordEntropy.getMaximumEntropyWordLazy(candidateWords, filteredWords);
    }
}

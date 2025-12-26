package com.fistraltech.analysis;
/** Calculation of entropy is an expensive operation, particularly for large dictionaries at the start of the game. Given the starting dictionary is always 
 * the same, it is worth calculating this once and makeing it available in a cache. This will significantly speed up bot selection algorithms that rely on 
 * entropy calculations.
 * 
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.ConfigManager;

public class WordEntropy {
    private Dictionary dictionary;
    //Stores computed entropy values for words in the context of this dictionary
    private Map<String, Float> entropyCache = new HashMap<>();
    private Map<String, Map<String, Set<String>>> responseBucketCache = new HashMap<>();
    private WordGame wordGame;
    /**
     * Global cache of response patterns for word pairs. Key format: "guessWord:targetWord" -> response pattern.
     * Since responses are deterministic based solely on the two words (independent of dictionary or game state),
     * this cache is shared across all DictionaryAnalytics instances and persists for the application lifetime.
     * This dramatically reduces computation in entropy analysis across multiple games and sessions.
     * Thread-safe: uses ConcurrentHashMap for safe concurrent access.
     */
    private static final Map<String, String> responseCache = new ConcurrentHashMap<>();
    
    public WordEntropy(Dictionary dictionary) {
        this.dictionary = dictionary;
        // Create Config and WordGame once, reuse for all targets
        wordGame = new WordGame(dictionary, ConfigManager.getInstance().createGameConfig());
        for(String word: dictionary.getMasterSetOfWords()){
            float entropy = calculateEntropy(word); // Pre-compute and cache entropy for all words
            entropyCache.put(word, entropy);
        }
    }

    /**
     * Groups every word in the dictionary into buckets keyed by the response pattern produced
     * when comparing the candidate {@code word} against each target word.
     * The response pattern string encodes per-position feedback (e.g. Greens, Ambers, Reds, Excess).
     * Uses response caching to avoid recomputing responses for previously seen word pairs.
     * @param word candidate guess word used to produce response patterns
     * @return map from response pattern string to set of words generating that pattern
     * Complexity: O(N * C) first call per guess word, O(N) on cache hits where C = cost of computing response.
     */
    public Map<String, Set<String>> getResponseBuckets(String word) {
        // Check if we have already computed buckets for this word
        if (responseBucketCache.containsKey(word)) {
            return responseBucketCache.get(word);
        }

        // Compute buckets and store in cache
        Map<String, Set<String>> buckets = computeResponseBuckets(word);
        responseBucketCache.put(word, buckets);
        return buckets;
    }
    
    private Map<String, Set<String>> computeResponseBuckets(String word) {
    Map<String, Set<String>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
                
        for (String w : words) {
            try {
                // Check cache first using composite key
                String cacheKey = word + ":" + w;
                String bucket = responseCache.get(cacheKey);
                
                if (bucket == null) {
                    // Not in cache - compute and store
                    wordGame.setTargetWord(w);
                    Response r = wordGame.evaluate(word);
                    bucket = r.toString();
                    responseCache.put(cacheKey, bucket);
                }
                
                result.computeIfAbsent(bucket, k -> new HashSet<>()).add(w);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

          /**
     * Finds the word with maximal Shannon entropy relative to the current dictionary state.
     * Higher entropy implies greater expected reduction of remaining search space on next guess.
     * @return word yielding highest entropy (first encountered in case of ties)
     * Complexity: O(N * (B + response cost)) due to repeated entropy computations.
     */
    public String getMaximumEntropyWord(){
        String result = entropyCache.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return result;
    }

    public float getEntropy(String word){
        // Check cache first
        if(entropyCache.containsKey(word)){
            return entropyCache.get(word);
        }
        // Compute entropy and store in cache
        float entropy = calculateEntropy(word);
        entropyCache.put(word, entropy);
        return entropy;
    }

    /**
     * Calculates Shannon entropy (in bits) for a candidate guess based on distribution of
     * response pattern buckets: -Σ p * log2(p). Higher values indicate greater expected information gain.
     * @param word candidate guess word
     * @return entropy value in bits (0 if dictionary empty)
     */
    private float calculateEntropy(String word) {
        int dictionarySize = dictionary.getWordCount();
        
        // Early exit for edge cases
        if (dictionarySize == 0) {
            return 0f;
        }
        
        Map<String, Set<String>> buckets = getResponseBuckets(word);
        float entropy = 0f;
        
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            
            // Skip empty buckets (shouldn't happen but safe to check)
            if (bucketSize == 0) {
                continue;
            }
            
            // Calculate probability and entropy contribution
            double probability = (double) bucketSize / dictionarySize;
            
            // Shannon entropy formula: -sum(p * log2(p))
            double logProbability = Math.log(probability) / Math.log(2);
            entropy += -(probability * logProbability);
        }
        return entropy;
    }
}

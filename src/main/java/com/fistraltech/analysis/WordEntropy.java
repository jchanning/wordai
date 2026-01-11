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
import java.util.logging.Logger;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

public class WordEntropy {
    private Dictionary dictionary;
   
    //Stores computed entropy values for words in the context of this dictionary
    private Map<String, Float> entropyCache = new HashMap<>();
    private Map<String, Map<String, Set<String>>> responseBucketCache = new HashMap<>();
    
    // Additional caches for other selection algorithms
    private Map<String, Double> dictionaryReductionCache = new HashMap<>();
    private Map<String, Float> columnLengthCache = new HashMap<>();

    private WordGame wordGame;

    /**
     * Global cache of response patterns for word pairs. Key format: "guessWord:targetWord" -> response pattern.
     * Since responses are deterministic based solely on the two words (independent of dictionary or game state),
     * this cache is shared across all DictionaryAnalytics instances and persists for the application lifetime.
     * This dramatically reduces computation in entropy analysis across multiple games and sessions.
     * Thread-safe: uses ConcurrentHashMap for safe concurrent access.
     */
    private static final Map<String, String> responseCache = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(WordEntropy.class.getName());
    
    /**
     * Creates a WordEntropy instance with pre-computed entropy for all words.
     * Uses ConfigManager to get configuration - suitable for startup caching.
     * 
     * @param dictionary the dictionary to compute entropy for
     */
    public WordEntropy(Dictionary dictionary) {
        this(dictionary, ConfigManager.getInstance().createGameConfig(), true);
    }
    
    /**
     * Creates a WordEntropy instance with provided Config.
     * Pre-computes entropy for all words in the dictionary.
     * 
     * @param dictionary the dictionary to compute entropy for
     * @param config the game configuration to use
     */
    public WordEntropy(Dictionary dictionary, Config config) {
        this(dictionary, config, true);
    }
    
    /**
     * Creates a WordEntropy instance with optional pre-computation.
     * 
     * @param dictionary the dictionary to compute entropy for
     * @param config the game configuration to use
     * @param precompute if true, computes entropy for all words immediately
     */
    public WordEntropy(Dictionary dictionary, Config config, boolean precompute) {
        this.dictionary = dictionary;
        this.wordGame = new WordGame(dictionary, config);
        
        if (precompute) {
            logger.info(() -> "Pre-computing entropy for " + dictionary.getWordCount() + " words...");
            long startTime = System.currentTimeMillis();
            int wordLength = dictionary.getWordLength();
            
            for (String word : dictionary.getMasterSetOfWords()) {
                // Pre-compute response buckets (used by all algorithms)
                Map<String, Set<String>> buckets = getResponseBuckets(word);
                
                // Compute entropy
                float entropy = calculateEntropyFromBuckets(buckets, dictionary.getWordCount());
                entropyCache.put(word, entropy);
                
                // Compute dictionary reduction score (lower is better)
                double reductionScore = calculateDictionaryReductionFromBuckets(buckets, dictionary.getWordCount());
                dictionaryReductionCache.put(word, reductionScore);
                
                // Compute column length score (lower is better)
                float columnLength = calculateColumnLengthFromBuckets(buckets, dictionary.getWordCount(), wordLength);
                columnLengthCache.put(word, columnLength);
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.info(() -> "Entropy pre-computation complete in " + duration + "ms");
        }
    }

    /**
     * Groups every word in the dictionary into buckets keyed by the response pattern produced
     * key = response, value = set of target words producing that response
     * when comparing the candidate {@code word} against each target word.
     * The response pattern string encodes per-position feedback (e.g. Greens, Ambers, Reds, Excess).
     * Uses response caching to avoid recomputing responses for previously seen word pairs.
     * @param word candidate guess word used to produce response patterns
     * @return map from response pattern string to set of words generating that pattern
     * Complexity: O(N * C) first call per guess word, O(N) on cache hits where C = cost of computing response.
     */
    public Map<String, Set<String>> getResponseBuckets(String guessWord) {
        // Check if we have already computed buckets for this word
        if (responseBucketCache.containsKey(guessWord)) {
            return responseBucketCache.get(guessWord);
        }

        // Compute buckets and store in cache
        Map<String, Set<String>> buckets = computeResponseBuckets(guessWord);
        responseBucketCache.put(guessWord, buckets);
        return buckets;
    }
    
    private Map<String, Set<String>> computeResponseBuckets(String guessWord) {
    Map<String, Set<String>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
                
        for (String w : words) {
            try {
                // Check cache first using composite key
                String cacheKey = guessWord + ":" + w;
                String bucket = responseCache.get(cacheKey);
                
                if (bucket == null) {
                    // Not in cache - compute and store
                    wordGame.setTargetWord(w);
                    Response r = wordGame.evaluate(guessWord);
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
    
    /**
     * Finds the word with maximum entropy from a subset of words.
     * Uses the cached entropy values from the master dictionary.
     * 
     * @param candidateWords the set of words to consider (typically a filtered dictionary)
     * @return the word with highest entropy among the candidates, or null if empty
     */
    public String getMaximumEntropyWord(Set<String> candidateWords) {
        if (candidateWords == null || candidateWords.isEmpty()) {
            return null;
        }
        
        String bestWord = null;
        float bestEntropy = Float.NEGATIVE_INFINITY;
        
        for (String word : candidateWords) {
            Float entropy = entropyCache.get(word);
            if (entropy != null && entropy > bestEntropy) {
                bestEntropy = entropy;
                bestWord = word;
            }
        }
        
        if (bestWord != null) {
            final String finalBestWord = bestWord;
            final float finalBestEntropy = bestEntropy;
            logger.fine(() -> "Max entropy word from " + candidateWords.size() + " candidates: " + finalBestWord + " (entropy=" + finalBestEntropy + ")");
        }
        return bestWord;
    }
    
    /**
     * Finds the word with maximum dictionary reduction (minimum average remaining words) from a subset.
     * Uses the cached dictionary reduction values from the master dictionary.
     * 
     * @param candidateWords the set of words to consider (typically a filtered dictionary)
     * @return the word with best reduction score among the candidates, or null if empty
     */
    public String getWordWithMaximumReduction(Set<String> candidateWords) {
        if (candidateWords == null || candidateWords.isEmpty()) {
            return null;
        }
        
        String bestWord = null;
        double bestScore = Double.MAX_VALUE; // Lower is better
        
        for (String word : candidateWords) {
            Double score = dictionaryReductionCache.get(word);
            if (score != null && score < bestScore) {
                bestScore = score;
                bestWord = word;
            }
        }
        
        if (bestWord != null) {
            final String finalBestWord = bestWord;
            final double finalBestScore = bestScore;
            logger.fine(() -> "Max reduction word from " + candidateWords.size() + " candidates: " + finalBestWord + " (score=" + finalBestScore + ")");
        }
        return bestWord;
    }
    
    /**
     * Finds the word with minimum expected column length from a subset.
     * Uses the cached column length values from the master dictionary.
     * 
     * @param candidateWords the set of words to consider (typically a filtered dictionary)
     * @return the word with minimum column length among the candidates, or null if empty
     */
    public String getWordWithMinimumColumnLength(Set<String> candidateWords) {
        if (candidateWords == null || candidateWords.isEmpty()) {
            return null;
        }
        
        String bestWord = null;
        float bestScore = Float.MAX_VALUE; // Lower is better
        
        for (String word : candidateWords) {
            Float score = columnLengthCache.get(word);
            if (score != null && score < bestScore) {
                bestScore = score;
                bestWord = word;
            }
        }
        
        if (bestWord != null) {
            final String finalBestWord = bestWord;
            final float finalBestScore = bestScore;
            logger.fine(() -> "Min column length word from " + candidateWords.size() + " candidates: " + finalBestWord + " (score=" + finalBestScore + ")");
        }
        return bestWord;
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
        return calculateEntropyFromBuckets(buckets, dictionarySize);
    }
    
    /**
     * Calculates Shannon entropy from pre-computed response buckets.
     */
    private float calculateEntropyFromBuckets(Map<String, Set<String>> buckets, int dictionarySize) {
        if (dictionarySize == 0) {
            return 0f;
        }
        
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
    
    /**
     * Calculates expected dictionary size after guessing a word.
     * Lower values mean better dictionary reduction.
     * Uses the same logic as DictionaryReduction.calculateAverageDictionarySize().
     */
    private double calculateDictionaryReductionFromBuckets(Map<String, Set<String>> buckets, int dictionarySize) {
        if (dictionarySize == 0) {
            return 0.0;
        }
        
        double totalSize = 0;
        
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            double probability = (double) bucketSize / dictionarySize;
            totalSize += probability * bucketSize;
        }
        
        return totalSize;
    }
    
    /**
     * Calculates expected column length (product of unique letters per position) after guessing a word.
     * Lower values indicate better column length minimization.
     * Uses the same logic as MinimiseColumnLengths.calculateExpectedColumnLength().
     */
    private float calculateColumnLengthFromBuckets(Map<String, Set<String>> buckets, int dictionarySize, int wordLength) {
        if (dictionarySize == 0) {
            return 0f;
        }
        
        float expectedLength = 0f;
        
        for (Set<String> bucketWords : buckets.values()) {
            int bucketSize = bucketWords.size();
            
            if (bucketSize == 0) {
                continue;
            }
            
            double probability = (double) bucketSize / dictionarySize;
            int columnLength = calculateColumnLengthForWords(bucketWords, wordLength);
            expectedLength += probability * columnLength;
        }
        
        return expectedLength;
    }
    
    /**
     * Calculates the total column length (product of unique letters per position) for a set of words.
     */
    private int calculateColumnLengthForWords(Set<String> words, int wordLength) {
        if (words.isEmpty()) {
            return 0;
        }
        
        int totalLength = 0;
        for (int pos = 0; pos < wordLength; pos++) {
            Set<Character> uniqueLetters = new HashSet<>();
            for (String word : words) {
                uniqueLetters.add(word.charAt(pos));
            }
            if (totalLength == 0) {
                totalLength = uniqueLetters.size();
            } else {
                totalLength *= uniqueLetters.size();
            }
        }
        
        return totalLength;
    }
}

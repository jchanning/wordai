package com.fistraltech.analysis;

/**
 * Calculation of entropy is an expensive operation, particularly for large dictionaries at the start of the game.
 * Given the starting dictionary is always the same, it is worth calculating this once and making it available
 * in a cache. This will significantly speed up bot selection algorithms that rely on entropy calculations.
 * 
 * <p><strong>Memory Optimization:</strong>
 * The response cache uses memory-efficient structures:
 * <ul>
 *   <li>{@link WordPairKey}: Stores word pair references instead of concatenated strings (~28 bytes vs ~56 bytes)</li>
 *   <li>Short-encoded response patterns: 2 bytes vs ~48 bytes for String representation</li>
 *   <li>Interned word strings: Dictionary words are interned to ensure reference sharing across caches</li>
 * </ul>
 * This achieves ~80% memory reduction in the response cache (from ~800MB to ~170MB for 5-letter dictionaries).
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.ResponseEntry;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

public class WordEntropy {
    private Dictionary dictionary;
    
    /** Interned word set - ensures all words share the same String instances for memory efficiency */
    private Set<String> internedWords;
   
    // Stores computed entropy values for words in the context of this dictionary
    private Map<String, Float> entropyCache = new HashMap<>();
    private Map<String, Map<Short, Set<String>>> responseBucketCache = new HashMap<>();
    
    // Additional caches for other selection algorithms
    private Map<String, Double> dictionaryReductionCache = new HashMap<>();
    private Map<String, Float> columnLengthCache = new HashMap<>();

    private WordGame wordGame;
    
    /** Word length for response pattern encoding/decoding */
    private byte wordLength;

    /**
     * Global cache of response patterns for word pairs.
     * 
     * <p><strong>Memory-efficient design:</strong>
     * <ul>
     *   <li>Key: {@link WordPairKey} holding references to interned word strings (~28 bytes)</li>
     *   <li>Value: Short-encoded response pattern via {@link ResponsePattern} (2 bytes)</li>
     * </ul>
     * 
     * Since responses are deterministic based solely on the two words (independent of dictionary or game state),
     * this cache is shared across all WordEntropy instances and persists for the application lifetime.
     * This dramatically reduces computation in entropy analysis across multiple games and sessions.
     * 
     * Thread-safe: uses ConcurrentHashMap for safe concurrent access.
     */
    private static final Map<WordPairKey, Short> responseCache = new ConcurrentHashMap<>();
    private static final Logger logger = Logger.getLogger(WordEntropy.class.getName());
    
    /**
     * Returns the number of entries currently in the global response cache.
     * Useful for monitoring memory usage and debugging.
     * 
     * @return the number of cached response patterns
     */
    public static int getResponseCacheSize() {
        return responseCache.size();
    }
    
    /**
     * Clears the global response cache.
     * Use with caution - this will force recomputation of all response patterns.
     * Primarily useful for testing or when memory pressure is critical.
     */
    public static void clearResponseCache() {
        int size = responseCache.size();
        responseCache.clear();
        logger.info(() -> "Cleared response cache (" + size + " entries)");
    }
    
    /**
     * Returns an estimate of the memory used by the global response cache in bytes.
     * 
     * <p>Memory calculation:
     * <ul>
     *   <li>WordPairKey: ~28 bytes (object header + 2 references + cached hashCode)</li>
     *   <li>Short value: ~2 bytes (but boxed to Short which is ~16 bytes)</li>
     *   <li>ConcurrentHashMap entry overhead: ~48 bytes</li>
     * </ul>
     * Estimated ~92 bytes per entry.
     * 
     * @return estimated memory usage in bytes
     */
    public static long getEstimatedCacheMemoryBytes() {
        return (long) responseCache.size() * 92L;
    }
    
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
        this.wordLength = (byte) dictionary.getWordLength();
        
        // Intern all dictionary words to ensure reference sharing across caches
        // This is critical for memory efficiency of WordPairKey
        this.internedWords = dictionary.getMasterSetOfWords().stream()
            .map(String::intern)
            .collect(Collectors.toSet());
        
        if (precompute) {
            logger.info(() -> "Pre-computing entropy for " + dictionary.getWordCount() + " words...");
            long startTime = System.currentTimeMillis();
            
            for (String word : internedWords) {
                // Pre-compute response buckets (used by all algorithms)
                Map<Short, Set<String>> buckets = getResponseBuckets(word);
                
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
            logger.info(() -> "Response cache size: " + responseCache.size() + " entries");
        }
    }

    /**
     * Groups every word in the dictionary into buckets keyed by the response pattern produced
     * key = encoded response pattern (short), value = set of target words producing that response
     * when comparing the candidate {@code word} against each target word.
     * The response pattern is encoded as a short for memory efficiency.
     * Uses response caching to avoid recomputing responses for previously seen word pairs.
     * @param guessWord candidate guess word used to produce response patterns
     * @return map from encoded response pattern to set of words generating that pattern
     * Complexity: O(N * C) first call per guess word, O(N) on cache hits where C = cost of computing response.
     */
    public Map<Short, Set<String>> getResponseBuckets(String guessWord) {
        // Ensure we use the interned version of the guess word
        String internedGuess = guessWord.intern();
        
        // Check if we have already computed buckets for this word
        if (responseBucketCache.containsKey(internedGuess)) {
            return responseBucketCache.get(internedGuess);
        }

        // Compute buckets and store in cache
        Map<Short, Set<String>> buckets = computeResponseBuckets(internedGuess);
        responseBucketCache.put(internedGuess, buckets);
        return buckets;
    }
    
    private Map<Short, Set<String>> computeResponseBuckets(String guessWord) {
        Map<Short, Set<String>> result = new HashMap<>();
        Set<String> words = internedWords != null ? internedWords : dictionary.getMasterSetOfWords();
                
        for (String targetWord : words) {
            try {
                // Ensure target word is interned for WordPairKey efficiency
                String internedTarget = targetWord.intern();
                
                // Check cache first using composite key
                WordPairKey cacheKey = new WordPairKey(guessWord, internedTarget);
                Short encodedBucket = responseCache.get(cacheKey);
                
                if (encodedBucket == null) {
                    // Not in cache - compute and store
                    wordGame.setTargetWord(internedTarget);
                    Response r = wordGame.evaluate(guessWord);
                    encodedBucket = encodeResponse(r);
                    responseCache.put(cacheKey, encodedBucket);
                }
                
                result.computeIfAbsent(encodedBucket, k -> new HashSet<>()).add(internedTarget);
            } catch (Exception ex) {
                logger.warning(() -> "Error computing response for " + guessWord + " vs " + targetWord + ": " + ex.getMessage());
            }
        }
        return result;
    }
    
    /**
     * Encodes a Response object into a short value using the ResponsePattern encoding.
     * @param response the response to encode
     * @return the encoded short value
     */
    private short encodeResponse(Response response) {
        short encoded = 0;
        int position = 0;
        for (ResponseEntry entry : response.getStatuses()) {
            int code = switch (entry.status) {
                case 'G' -> 0;
                case 'A' -> 1;
                case 'R' -> 2;
                case 'X' -> 3;
                default -> 2; // Default to RED for unknown
            };
            encoded |= (short) (code << (position * 2));
            position++;
        }
        return encoded;
    }
    
    /**
     * Decodes a short-encoded response pattern back to a string for display/debugging.
     * @param encoded the encoded response pattern
     * @return the decoded string (e.g., "GARXR")
     */
    private String decodeResponse(short encoded) {
        char[] chars = new char[wordLength];
        char[] codeToChar = {'G', 'A', 'R', 'X'};
        for (int i = 0; i < wordLength; i++) {
            int code = (encoded >> (i * 2)) & 0b11;
            chars[i] = codeToChar[code];
        }
        return new String(chars);
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
        
        Map<Short, Set<String>> buckets = getResponseBuckets(word);
        return calculateEntropyFromBuckets(buckets, dictionarySize);
    }
    
    /**
     * Calculates Shannon entropy from pre-computed response buckets.
     */
    private float calculateEntropyFromBuckets(Map<Short, Set<String>> buckets, int dictionarySize) {
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
    private double calculateDictionaryReductionFromBuckets(Map<Short, Set<String>> buckets, int dictionarySize) {
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
    private float calculateColumnLengthFromBuckets(Map<Short, Set<String>> buckets, int dictionarySize, int wordLength) {
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

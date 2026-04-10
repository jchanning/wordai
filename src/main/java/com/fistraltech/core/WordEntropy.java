package com.fistraltech.core;

/**
 * Calculation of entropy is an expensive operation, particularly for large dictionaries at the start of the game.
 * Given the starting dictionary is always the same, it is worth calculating this once and making it available
 * in a cache. This will significantly speed up bot selection algorithms that rely on entropy calculations.
 * 
 * <p><strong>Memory Optimization (Phase 1 - ResponseMatrix):</strong>
 * The response cache now uses a pre-computed 2D matrix approach:
 * <ul>
 *   <li>{@link ResponseMatrix}: Pre-computes all word-pair response patterns in a dense short[][] array</li>
 *   <li>Memory usage: ~5 MB for 2,315 words vs ~500 MB for HashMap approach</li>
 *   <li>Lookup: O(1) array access vs O(1) HashMap with object allocation overhead</li>
 * </ul>
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fistraltech.util.Config;
import com.fistraltech.util.ConfigManager;

public class WordEntropy {
    private Dictionary dictionary;
    
    /** Interned word set - ensures all words share the same String instances for memory efficiency */
    private Set<String> internedWords;
   
    // Stores computed entropy values for words in the context of this dictionary
    private final Map<String, Float> entropyCache = new HashMap<>();
    private final Map<String, Map<Short, Set<String>>> responseBucketCache = new HashMap<>();
    
    // Additional caches for other selection algorithms
    private final Map<String, Double> dictionaryReductionCache = new HashMap<>();
    private final Map<String, Float> columnLengthCache = new HashMap<>();

    private WordGame wordGame;
    
    /** Word length for response pattern encoding/decoding */
    private byte wordLength;
    
    /** 
     * Memory-efficient response matrix for this dictionary.
     * Replaces the global ConcurrentHashMap approach with a dense 2D array.
     */
    private ResponseMatrix responseMatrix;

    private static final Logger logger = Logger.getLogger(WordEntropy.class.getName());
    
    /**
     * Returns the ResponseMatrix for direct access to pattern lookups.
     *
     * @return the response matrix
     */
    public ResponseMatrix getResponseMatrix() {
        return responseMatrix;
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
        this.internedWords = dictionary.getMasterSetOfWords().stream()
            .map(String::intern)
            .collect(Collectors.toSet());
        
        // Build the memory-efficient response matrix
        logger.info(() -> "Building ResponseMatrix for " + dictionary.getWordCount() + " words...");
        this.responseMatrix = new ResponseMatrix(dictionary, config);
        
        if (precompute) {
            logger.info(() -> "Pre-computing entropy for " + dictionary.getWordCount() + " words...");
            long startTime = System.currentTimeMillis();
            
            precomputeWithMatrix();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info(() -> "Entropy pre-computation complete in " + duration + "ms");
            logger.info(() -> "ResponseMatrix memory: " + (responseMatrix.getEstimatedMemoryBytes() / 1024 / 1024) + " MB");
        }
    }
    
    /**
     * Pre-computes entropy using the new matrix-based approach.
     * This is significantly faster and uses less memory.
     * Uses parallel computation for larger dictionaries.
     */
    private void precomputeWithMatrix() {
        int wordCount = dictionary.getWordCount();
        
        // Convert to array for indexed parallel access
        String[] wordArray = internedWords.toArray(new String[0]);
        
        // Use parallel computation for larger dictionaries
        if (wordCount >= 100) {
            precomputeWithMatrixParallel(wordArray, wordCount);
        } else {
            precomputeWithMatrixSequential(wordArray, wordCount);
        }
    }
    
    /**
     * Sequential precomputation for small dictionaries.
     */
    private void precomputeWithMatrixSequential(String[] wordArray, int wordCount) {
        for (String word : wordArray) {
            int wordId = responseMatrix.getWordId(word);
            
            // Compute entropy directly from matrix (no HashSet allocation)
            float entropy = responseMatrix.computeEntropy(wordId);
            entropyCache.put(word, entropy);
            
            // Compute dictionary reduction score from matrix
            int[] counts = responseMatrix.getBucketCounts(wordId);
            double reductionScore = calculateDictionaryReductionFromCounts(counts, wordCount);
            dictionaryReductionCache.put(word, reductionScore);
            
            // Compute column length score directly from matrix (Phase 2 optimization - no HashSet allocation)
            float columnLength = responseMatrix.computeExpectedColumnLength(wordId);
            columnLengthCache.put(word, columnLength);
        }
    }
    
    /**
     * Parallel precomputation for larger dictionaries.
     * Uses ConcurrentHashMap for thread-safe cache population.
     */
    private void precomputeWithMatrixParallel(String[] wordArray, int wordCount) {
        // Use concurrent maps for thread-safe parallel writes
        Map<String, Float> parallelEntropyCache = new ConcurrentHashMap<>();
        Map<String, Double> parallelReductionCache = new ConcurrentHashMap<>();
        Map<String, Float> parallelColumnCache = new ConcurrentHashMap<>();
        
        IntStream.range(0, wordArray.length)
            .parallel()
            .forEach(i -> {
                String word = wordArray[i];
                int wordId = responseMatrix.getWordId(word);
                
                // Compute entropy directly from matrix
                float entropy = responseMatrix.computeEntropy(wordId);
                parallelEntropyCache.put(word, entropy);
                
                // Compute dictionary reduction score from matrix
                int[] counts = responseMatrix.getBucketCounts(wordId);
                double reductionScore = calculateDictionaryReductionFromCounts(counts, wordCount);
                parallelReductionCache.put(word, reductionScore);
                
                // Compute column length score directly from matrix
                float columnLength = responseMatrix.computeExpectedColumnLength(wordId);
                parallelColumnCache.put(word, columnLength);
            });
        
        // Copy to main caches
        entropyCache.putAll(parallelEntropyCache);
        dictionaryReductionCache.putAll(parallelReductionCache);
        columnLengthCache.putAll(parallelColumnCache);
    }
    
    /**
     * Calculates dictionary reduction score from bucket counts array.
     */
    private double calculateDictionaryReductionFromCounts(int[] counts, int total) {
        if (total == 0) {
            return 0.0;
        }
        
        double totalSize = 0;
        for (int count : counts) {
            if (count > 0) {
                double probability = (double) count / total;
                totalSize += probability * count;
            }
        }
        return totalSize;
    }

    /**
     * Groups every word in the dictionary into buckets keyed by the response pattern produced when
     * {@code guessWord} is evaluated against each target word in the dictionary.
     * Key = encoded response pattern (short), value = set of target words producing that pattern.
     * Results are cached so repeated calls for the same guess word are O(1).
     *
     * @param guessWord candidate guess word used to produce response patterns
     * @return map from encoded response pattern to set of words generating that pattern
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
        int guessId = responseMatrix.getWordId(guessWord);
        Map<Short, Set<String>> result = new HashMap<>();
        int wordCount = responseMatrix.getWordCount();

        if (guessId >= 0) {
            // Fast path: guess word is in the matrix — O(1) lookup per target
            for (int targetId = 0; targetId < wordCount; targetId++) {
                short pattern = responseMatrix.getPattern(guessId, targetId);
                String targetWord = responseMatrix.getWord(targetId);
                result.computeIfAbsent(pattern, k -> new HashSet<>()).add(targetWord);
            }
        } else {
            // Fallback: guess word is outside this dictionary (e.g. full-dictionary guess
            // evaluated against a filtered remaining-dictionary matrix). Compute responses
            // directly via WordGame.
            for (int targetId = 0; targetId < wordCount; targetId++) {
                String targetWord = responseMatrix.getWord(targetId);
                try {
                    wordGame.setTargetWord(targetWord);
                    Response r = wordGame.evaluate(guessWord);
                    short pattern = encodeResponse(r);
                    result.computeIfAbsent(pattern, k -> new HashSet<>()).add(targetWord);
                } catch (Exception ex) {
                    logger.warning(() -> "Error computing response for " + guessWord + " vs " + targetWord + ": " + ex.getMessage());
                }
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
     * Finds the word with maximum dictionary reduction (minimum average remaining words).
     * Uses the cached dictionary reduction values from the master dictionary.
     * 
     * @return the word with best reduction score, or null if cache is empty
     */
    public String getWordWithMaximumReduction() {
        return dictionaryReductionCache.entrySet().stream()
            .min(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(null);
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
    
    // ========================================================================
    // Phase 3: Lazy and Parallel Computation Methods
    // ========================================================================
    
    /**
     * Finds the word with maximum entropy using lazy computation.
     * Computes entropy on-demand for filtered candidates against filtered targets.
     * This is useful when the filtered dictionary is much smaller than the master dictionary.
     * 
     * @param candidateWords words that can be guessed (typically full dictionary)
     * @param targetWords remaining possible targets (filtered dictionary)
     * @return the word with highest entropy against the target set
     */
    public String getMaximumEntropyWordLazy(Set<String> candidateWords, Set<String> targetWords) {
        if (candidateWords == null || candidateWords.isEmpty() || 
            targetWords == null || targetWords.isEmpty()) {
            return null;
        }
        
        // If targets are most of the dictionary, use cached values
        if (targetWords.size() >= dictionary.getWordCount() * 0.8) {
            return getMaximumEntropyWord(candidateWords);
        }
        
        // Convert to arrays for matrix operations
        int[] candidateIds = new int[candidateWords.size()];
        int[] targetIds = new int[targetWords.size()];
        
        int idx = 0;
        for (String word : candidateWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                candidateIds[idx++] = id;
            }
        }
        int candidateCount = idx;
        
        idx = 0;
        for (String word : targetWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                targetIds[idx++] = id;
            }
        }
        int targetCount = idx;
        
        // Use parallel computation for large sets
        int bestId;
        if (candidateCount >= 100) {
            bestId = responseMatrix.findMaxEntropyWordIdParallel(
                candidateIds, candidateCount, targetIds, targetCount);
        } else {
            bestId = responseMatrix.findMaxEntropyWordId(
                candidateIds, candidateCount, targetIds, targetCount);
        }
        
        return responseMatrix.getWord(bestId);
    }
    
    /**
     * Finds the top N words by entropy using lazy parallel computation.
     * Only computes entropy for candidate words against the target set.
     * 
     * @param candidateWords words that can be guessed
     * @param targetWords remaining possible targets
     * @param topN number of top words to return
     * @return array of top N words sorted by entropy (highest first)
     */
    public String[] getTopNEntropyWords(Set<String> candidateWords, Set<String> targetWords, int topN) {
        if (candidateWords == null || candidateWords.isEmpty() || 
            targetWords == null || targetWords.isEmpty()) {
            return new String[0];
        }
        
        // Convert to arrays for matrix operations
        int[] candidateIds = new int[candidateWords.size()];
        int[] targetIds = new int[targetWords.size()];
        
        int idx = 0;
        for (String word : candidateWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                candidateIds[idx++] = id;
            }
        }
        int candidateCount = idx;
        
        idx = 0;
        for (String word : targetWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                targetIds[idx++] = id;
            }
        }
        int targetCount = idx;
        
        // Compute top N using matrix
        int[][] topResults = responseMatrix.computeTopNEntropy(
            candidateIds, candidateCount, targetIds, targetCount, topN);
        
        // Convert IDs back to words
        String[] result = new String[topResults.length];
        for (int i = 0; i < topResults.length; i++) {
            result[i] = responseMatrix.getWord(topResults[i][0]);
        }
        
        return result;
    }
    
    /**
     * Computes entropy for a specific word against a filtered target set.
     * This is a lazy evaluation that doesn't use the cached values.
     * 
     * @param guessWord the guess word
     * @param targetWords the set of possible targets
     * @return entropy value for this guess against the targets
     */
    public float computeEntropyAgainstTargets(String guessWord, Set<String> targetWords) {
        if (targetWords == null || targetWords.isEmpty()) {
            return 0f;
        }
        
        int guessId = responseMatrix.getWordId(guessWord);
        if (guessId < 0) {
            return 0f;
        }
        
        // Convert target words to IDs
        int[] targetIds = new int[targetWords.size()];
        int idx = 0;
        for (String word : targetWords) {
            int id = responseMatrix.getWordId(word);
            if (id >= 0) {
                targetIds[idx++] = id;
            }
        }
        
        return responseMatrix.computeEntropy(guessId, targetIds, idx);
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
}

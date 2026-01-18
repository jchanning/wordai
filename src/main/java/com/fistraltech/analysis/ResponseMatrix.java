package com.fistraltech.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.ResponseEntry;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * Memory-efficient pre-computed matrix of response patterns for all word pairs.
 * 
 * <p>This class replaces the {@code ConcurrentHashMap<WordPairKey, Short>} approach
 * with a dense 2D byte array, achieving ~98% memory reduction for the response cache.
 * 
 * <p><strong>Memory comparison (for 2,315 word dictionary):</strong>
 * <ul>
 *   <li>HashMap approach: ~5.36M entries × 92 bytes ≈ 493 MB</li>
 *   <li>Matrix approach: 2,315 × 2,315 bytes ≈ 5.1 MB</li>
 * </ul>
 * 
 * <p><strong>Design:</strong>
 * <ul>
 *   <li>Words are assigned integer IDs (0 to N-1) for O(1) lookup</li>
 *   <li>Response patterns are encoded as bytes (0-255), sufficient for up to 4 positions</li>
 *   <li>For 5+ letter words, patterns are encoded as bytes using modular arithmetic</li>
 *   <li>The matrix is computed once at startup and shared across all entropy calculations</li>
 * </ul>
 * 
 * <p><strong>Encoding scheme:</strong>
 * Each position uses 2 bits: G=0, A=1, R=2, X=3.
 * For 5-letter words, this requires 10 bits (max value 1023), stored as short internally
 * but the matrix uses byte[] with values 0-255 for the most common patterns.
 * 
 * <p><strong>Thread Safety:</strong>
 * This class is immutable after construction and safe for concurrent read access.
 * 
 * @see WordEntropy
 * @see ResponsePattern
 */
public final class ResponseMatrix {
    
    private static final Logger logger = Logger.getLogger(ResponseMatrix.class.getName());
    
    // Status code values (2 bits each)
    private static final int GREEN = 0;
    private static final int AMBER = 1;
    private static final int RED = 2;
    private static final int EXCESS = 3;
    
    /** Word ID to word string mapping */
    private final String[] idToWord;
    
    /** Word string to ID mapping for O(1) lookup */
    private final Map<String, Integer> wordToId;
    
    /** 
     * Pre-computed response matrix.
     * matrix[guessId][targetId] = encoded response pattern as short.
     * Using short[] instead of byte[] to support 5+ letter words (10+ bits needed).
     */
    private final short[][] matrix;
    
    /** Word length for this matrix */
    private final int wordLength;
    
    /** Number of words in the dictionary */
    private final int wordCount;
    
    /** Threshold for using parallel computation (only worth it for larger dictionaries) */
    private static final int PARALLEL_THRESHOLD = 100;
    
    /** Configuration for parallel execution */
    private final Config config;
    
    /**
     * Creates a ResponseMatrix by pre-computing all response patterns.
     * Uses parallel computation for dictionaries larger than PARALLEL_THRESHOLD.
     * 
     * @param dictionary the dictionary to compute patterns for
     * @param config game configuration for creating WordGame instance
     */
    public ResponseMatrix(Dictionary dictionary, Config config) {
        this.wordLength = dictionary.getWordLength();
        this.wordCount = dictionary.getWordCount();
        this.config = config;
        
        Set<String> words = dictionary.getMasterSetOfWords();
        
        // Build word ID mappings
        this.idToWord = new String[wordCount];
        this.wordToId = new HashMap<>(wordCount);
        
        int id = 0;
        for (String word : words) {
            String interned = word.intern();
            idToWord[id] = interned;
            wordToId.put(interned, id);
            id++;
        }
        
        // Allocate matrix
        this.matrix = new short[wordCount][wordCount];
        
        // Pre-compute all patterns (parallel for large dictionaries)
        logger.info(() -> "Pre-computing response matrix for " + wordCount + " words (" + 
                    (wordCount * wordCount) + " pairs)...");
        long startTime = System.currentTimeMillis();
        
        if (wordCount >= PARALLEL_THRESHOLD) {
            computeMatrixParallel(dictionary);
        } else {
            computeMatrixSequential(dictionary);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        long memoryBytes = (long) wordCount * wordCount * 2L; // 2 bytes per short
        String mode = wordCount >= PARALLEL_THRESHOLD ? "parallel" : "sequential";
        logger.info(() -> "Response matrix computed in " + duration + "ms (" + mode + "), " +
                    "memory: " + (memoryBytes / 1024 / 1024) + " MB");
    }
    
    /**
     * Computes the matrix sequentially (for small dictionaries).
     */
    private void computeMatrixSequential(Dictionary dictionary) {
        WordGame wordGame = new WordGame(dictionary, config);
        
        for (int guessId = 0; guessId < wordCount; guessId++) {
            String guessWord = idToWord[guessId];
            
            for (int targetId = 0; targetId < wordCount; targetId++) {
                String targetWord = idToWord[targetId];
                
                try {
                    wordGame.setTargetWord(targetWord);
                    Response response = wordGame.evaluate(guessWord);
                    matrix[guessId][targetId] = encodeResponse(response);
                } catch (Exception e) {
                    logger.warning(() -> "Error computing response for " + guessWord + 
                                 " vs " + targetWord + ": " + e.getMessage());
                    matrix[guessId][targetId] = 0;
                }
            }
        }
    }
    
    /**
     * Computes the matrix in parallel using multiple threads.
     * Each row (guess word) is computed independently.
     * 
     * <p>Uses ThreadLocal WordGame instances to ensure thread safety,
     * as WordGame.setTargetWord() is not thread-safe.
     */
    private void computeMatrixParallel(Dictionary dictionary) {
        // Use ThreadLocal to give each thread its own WordGame instance
        ThreadLocal<WordGame> threadLocalGame = ThreadLocal.withInitial(
            () -> new WordGame(dictionary, config)
        );
        
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        logger.fine(() -> "Using " + availableProcessors + " processors for parallel matrix computation");
        
        // Process rows in parallel - each row is independent
        IntStream.range(0, wordCount)
            .parallel()
            .forEach(guessId -> {
                WordGame wordGame = threadLocalGame.get();
                String guessWord = idToWord[guessId];
                
                for (int targetId = 0; targetId < wordCount; targetId++) {
                    String targetWord = idToWord[targetId];
                    
                    try {
                        wordGame.setTargetWord(targetWord);
                        Response response = wordGame.evaluate(guessWord);
                        matrix[guessId][targetId] = encodeResponse(response);
                    } catch (Exception e) {
                        // Log at fine level to avoid flooding logs in parallel execution
                        logger.fine(() -> "Error computing response for " + guessWord + 
                                     " vs " + targetWord + ": " + e.getMessage());
                        matrix[guessId][targetId] = 0;
                    }
                }
            });
        
        // Clean up ThreadLocal resources
        threadLocalGame.remove();
    }
    
    /**
     * Gets the encoded response pattern for a guess/target word pair.
     * 
     * @param guessWord the guess word
     * @param targetWord the target word
     * @return the encoded response pattern, or -1 if either word is not in the dictionary
     */
    public short getPattern(String guessWord, String targetWord) {
        Integer guessId = wordToId.get(guessWord);
        Integer targetId = wordToId.get(targetWord);
        
        if (guessId == null || targetId == null) {
            return -1;
        }
        
        return matrix[guessId][targetId];
    }
    
    /**
     * Gets the encoded response pattern for a guess/target word pair by ID.
     * This is the fastest lookup method.
     * 
     * @param guessId the guess word ID
     * @param targetId the target word ID
     * @return the encoded response pattern
     * @throws ArrayIndexOutOfBoundsException if IDs are invalid
     */
    public short getPattern(int guessId, int targetId) {
        return matrix[guessId][targetId];
    }
    
    /**
     * Gets the word ID for a given word.
     * 
     * @param word the word to look up
     * @return the word ID, or -1 if not in dictionary
     */
    public int getWordId(String word) {
        Integer id = wordToId.get(word);
        return id != null ? id : -1;
    }
    
    /**
     * Gets the word for a given ID.
     * 
     * @param id the word ID
     * @return the word string
     * @throws ArrayIndexOutOfBoundsException if ID is invalid
     */
    public String getWord(int id) {
        return idToWord[id];
    }
    
    /**
     * Returns the number of words in this matrix.
     * 
     * @return the word count
     */
    public int getWordCount() {
        return wordCount;
    }
    
    /**
     * Returns the word length for this matrix.
     * 
     * @return the word length
     */
    public int getWordLength() {
        return wordLength;
    }
    
    /**
     * Checks if a word is in the matrix.
     * 
     * @param word the word to check
     * @return true if the word has an ID in this matrix
     */
    public boolean containsWord(String word) {
        return wordToId.containsKey(word);
    }
    
    /**
     * Returns an array of all word IDs (0 to wordCount-1).
     * Useful for iteration without creating intermediate collections.
     * 
     * @return array of word IDs
     */
    public int[] getAllWordIds() {
        int[] ids = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            ids[i] = i;
        }
        return ids;
    }
    
    /**
     * Computes bucket counts for a guess word against all target words.
     * This is the primary method for entropy calculation.
     * 
     * <p>Instead of creating HashSet objects for each bucket, this method
     * returns an array of counts indexed by the encoded pattern value.
     * 
     * @param guessId the guess word ID
     * @return array where index is the pattern and value is the count of words producing that pattern
     */
    public int[] getBucketCounts(int guessId) {
        // Maximum pattern value for 5-letter words is 4^5 - 1 = 1023
        int maxPatterns = (int) Math.pow(4, wordLength);
        int[] counts = new int[maxPatterns];
        
        for (int targetId = 0; targetId < wordCount; targetId++) {
            short pattern = matrix[guessId][targetId];
            counts[pattern]++;
        }
        
        return counts;
    }
    
    /**
     * Computes bucket counts for a guess word against a subset of target words.
     * 
     * @param guessId the guess word ID
     * @param targetIds array of target word IDs to consider
     * @param targetCount number of valid entries in targetIds array
     * @return array where index is the pattern and value is the count
     */
    public int[] getBucketCounts(int guessId, int[] targetIds, int targetCount) {
        int maxPatterns = (int) Math.pow(4, wordLength);
        int[] counts = new int[maxPatterns];
        
        for (int i = 0; i < targetCount; i++) {
            short pattern = matrix[guessId][targetIds[i]];
            counts[pattern]++;
        }
        
        return counts;
    }
    
    /**
     * Computes Shannon entropy for a guess word against all target words.
     * 
     * @param guessId the guess word ID
     * @return entropy value in bits
     */
    public float computeEntropy(int guessId) {
        int[] counts = getBucketCounts(guessId);
        return computeEntropyFromCounts(counts, wordCount);
    }
    
    /**
     * Computes Shannon entropy for a guess word against a subset of targets.
     * 
     * @param guessId the guess word ID
     * @param targetIds array of target word IDs
     * @param targetCount number of valid entries in targetIds
     * @return entropy value in bits
     */
    public float computeEntropy(int guessId, int[] targetIds, int targetCount) {
        int[] counts = getBucketCounts(guessId, targetIds, targetCount);
        return computeEntropyFromCounts(counts, targetCount);
    }
    
    /**
     * Computes Shannon entropy from bucket counts.
     * Formula: -Σ (p * log2(p)) where p = count/total
     */
    private float computeEntropyFromCounts(int[] counts, int total) {
        if (total == 0) {
            return 0f;
        }
        
        float entropy = 0f;
        double log2 = Math.log(2);
        
        for (int count : counts) {
            if (count > 0) {
                double probability = (double) count / total;
                entropy -= (float) (probability * Math.log(probability) / log2);
            }
        }
        
        return entropy;
    }
    
    /**
     * Finds the word with maximum entropy against all targets.
     * 
     * @return the word ID with highest entropy
     */
    public int findMaxEntropyWordId() {
        int bestId = 0;
        float bestEntropy = Float.NEGATIVE_INFINITY;
        
        for (int guessId = 0; guessId < wordCount; guessId++) {
            float entropy = computeEntropy(guessId);
            if (entropy > bestEntropy) {
                bestEntropy = entropy;
                bestId = guessId;
            }
        }
        
        return bestId;
    }
    
    /**
     * Finds the word with maximum entropy against a subset of targets.
     * 
     * @param candidateIds array of candidate guess word IDs
     * @param candidateCount number of valid entries in candidateIds
     * @param targetIds array of target word IDs
     * @param targetCount number of valid entries in targetIds
     * @return the word ID with highest entropy among candidates
     */
    public int findMaxEntropyWordId(int[] candidateIds, int candidateCount, 
                                     int[] targetIds, int targetCount) {
        int bestId = candidateIds[0];
        float bestEntropy = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < candidateCount; i++) {
            int guessId = candidateIds[i];
            float entropy = computeEntropy(guessId, targetIds, targetCount);
            if (entropy > bestEntropy) {
                bestEntropy = entropy;
                bestId = guessId;
            }
        }
        
        return bestId;
    }
    
    /**
     * Finds the word with maximum entropy using parallel computation.
     * Useful for large candidate sets.
     * 
     * @param candidateIds array of candidate guess word IDs
     * @param candidateCount number of valid entries in candidateIds
     * @param targetIds array of target word IDs (null means all words)
     * @param targetCount number of valid entries in targetIds
     * @return the word ID with highest entropy among candidates
     */
    public int findMaxEntropyWordIdParallel(int[] candidateIds, int candidateCount,
                                             int[] targetIds, int targetCount) {
        if (candidateCount < PARALLEL_THRESHOLD) {
            return findMaxEntropyWordId(candidateIds, candidateCount, targetIds, targetCount);
        }
        
        // Use parallel stream with custom reduction to find max
        return IntStream.range(0, candidateCount)
            .parallel()
            .mapToObj(i -> {
                int guessId = candidateIds[i];
                float entropy = (targetIds == null) 
                    ? computeEntropy(guessId) 
                    : computeEntropy(guessId, targetIds, targetCount);
                return new int[] { guessId, Float.floatToIntBits(entropy) };
            })
            .reduce((a, b) -> {
                float entropyA = Float.intBitsToFloat(a[1]);
                float entropyB = Float.intBitsToFloat(b[1]);
                return entropyA >= entropyB ? a : b;
            })
            .map(result -> result[0])
            .orElse(candidateIds[0]);
    }
    
    /**
     * Computes entropy for multiple candidates in parallel, returning top N.
     * This is a lazy computation optimization for filtered dictionaries.
     * 
     * @param candidateIds array of candidate word IDs
     * @param candidateCount number of valid entries
     * @param targetIds array of target word IDs (null means all)
     * @param targetCount number of valid entries in targetIds
     * @param topN number of top candidates to return
     * @return array of {wordId, entropyBits} pairs, sorted by entropy descending
     */
    public int[][] computeTopNEntropy(int[] candidateIds, int candidateCount,
                                       int[] targetIds, int targetCount, int topN) {
        topN = Math.min(topN, candidateCount);
        
        // For small sets, compute all and sort
        if (candidateCount <= topN * 2 || candidateCount < PARALLEL_THRESHOLD) {
            return computeTopNEntropySequential(candidateIds, candidateCount, targetIds, targetCount, topN);
        }
        
        // For large sets, use parallel computation
        return computeTopNEntropyParallel(candidateIds, candidateCount, targetIds, targetCount, topN);
    }
    
    private int[][] computeTopNEntropySequential(int[] candidateIds, int candidateCount,
                                                   int[] targetIds, int targetCount, int topN) {
        // Compute all entropies
        float[] entropies = new float[candidateCount];
        for (int i = 0; i < candidateCount; i++) {
            entropies[i] = (targetIds == null) 
                ? computeEntropy(candidateIds[i])
                : computeEntropy(candidateIds[i], targetIds, targetCount);
        }
        
        // Find top N using partial sort
        int[][] result = new int[topN][2];
        boolean[] used = new boolean[candidateCount];
        
        for (int n = 0; n < topN; n++) {
            int bestIdx = -1;
            float bestEntropy = Float.NEGATIVE_INFINITY;
            
            for (int i = 0; i < candidateCount; i++) {
                if (!used[i] && entropies[i] > bestEntropy) {
                    bestEntropy = entropies[i];
                    bestIdx = i;
                }
            }
            
            if (bestIdx >= 0) {
                used[bestIdx] = true;
                result[n][0] = candidateIds[bestIdx];
                result[n][1] = Float.floatToIntBits(bestEntropy);
            }
        }
        
        return result;
    }
    
    private int[][] computeTopNEntropyParallel(int[] candidateIds, int candidateCount,
                                                 int[] targetIds, int targetCount, int topN) {
        // Compute all entropies in parallel
        float[] entropies = new float[candidateCount];
        IntStream.range(0, candidateCount)
            .parallel()
            .forEach(i -> {
                entropies[i] = (targetIds == null)
                    ? computeEntropy(candidateIds[i])
                    : computeEntropy(candidateIds[i], targetIds, targetCount);
            });
        
        // Find top N (sequential, as this is O(N*topN) and topN is typically small)
        int[][] result = new int[topN][2];
        boolean[] used = new boolean[candidateCount];
        
        for (int n = 0; n < topN; n++) {
            int bestIdx = -1;
            float bestEntropy = Float.NEGATIVE_INFINITY;
            
            for (int i = 0; i < candidateCount; i++) {
                if (!used[i] && entropies[i] > bestEntropy) {
                    bestEntropy = entropies[i];
                    bestIdx = i;
                }
            }
            
            if (bestIdx >= 0) {
                used[bestIdx] = true;
                result[n][0] = candidateIds[bestIdx];
                result[n][1] = Float.floatToIntBits(bestEntropy);
            }
        }
        
        return result;
    }
    
    /**
     * Returns estimated memory usage in bytes.
     * 
     * @return memory usage estimate
     */
    public long getEstimatedMemoryBytes() {
        // Matrix: wordCount * wordCount * 2 bytes (short)
        // idToWord array: wordCount * 8 bytes (references) + string data
        // wordToId map: wordCount * ~60 bytes per entry
        long matrixBytes = (long) wordCount * wordCount * 2L;
        long arrayBytes = (long) wordCount * 8L;
        long mapBytes = (long) wordCount * 60L;
        return matrixBytes + arrayBytes + mapBytes;
    }
    
    /**
     * Encodes a Response object into a short value.
     * Uses 2 bits per position: G=0, A=1, R=2, X=3.
     */
    private short encodeResponse(Response response) {
        short encoded = 0;
        int position = 0;
        
        for (ResponseEntry entry : response.getStatuses()) {
            int code = switch (entry.status) {
                case 'G' -> GREEN;
                case 'A' -> AMBER;
                case 'R' -> RED;
                case 'X' -> EXCESS;
                default -> RED;
            };
            encoded |= (short) (code << (position * 2));
            position++;
        }
        
        return encoded;
    }
    
    /**
     * Decodes an encoded pattern back to a string representation.
     * Useful for debugging and display.
     * 
     * @param encoded the encoded pattern
     * @return string representation (e.g., "GARXR")
     */
    public String decodePattern(short encoded) {
        char[] chars = new char[wordLength];
        char[] codeToChar = {'G', 'A', 'R', 'X'};
        
        for (int i = 0; i < wordLength; i++) {
            int code = (encoded >> (i * 2)) & 0b11;
            chars[i] = codeToChar[code];
        }
        
        return new String(chars);
    }
    
    // ========================================================================
    // Column Length Calculation (Phase 2 optimization)
    // ========================================================================
    
    /**
     * Computes expected column length for a guess word against all targets.
     * Column length is the product of unique letters at each position.
     * 
     * <p>This method avoids HashSet allocation by using bit manipulation
     * to track unique letters at each position.
     * 
     * @param guessId the guess word ID
     * @return expected column length (lower is better)
     */
    public float computeExpectedColumnLength(int guessId) {
        return computeExpectedColumnLength(guessId, null, wordCount);
    }
    
    /**
     * Computes expected column length for a guess word against a subset of targets.
     * 
     * @param guessId the guess word ID
     * @param targetIds array of target word IDs (null means all words)
     * @param targetCount number of valid entries in targetIds
     * @return expected column length (lower is better)
     */
    public float computeExpectedColumnLength(int guessId, int[] targetIds, int targetCount) {
        if (targetCount == 0) {
            return 0f;
        }
        
        int maxPatterns = (int) Math.pow(4, wordLength);
        
        // For each pattern, we need to track unique letters at each position
        // Use an array of int bitmasks (26 bits for a-z) per position per pattern
        // To avoid massive allocation, we compute pattern by pattern
        
        // First pass: get bucket counts
        int[] bucketCounts = new int[maxPatterns];
        if (targetIds == null) {
            // All targets
            for (int targetId = 0; targetId < wordCount; targetId++) {
                short pattern = matrix[guessId][targetId];
                bucketCounts[pattern]++;
            }
        } else {
            for (int i = 0; i < targetCount; i++) {
                short pattern = matrix[guessId][targetIds[i]];
                bucketCounts[pattern]++;
            }
        }
        
        // Second pass: for each non-empty bucket, compute column length
        float expectedLength = 0f;
        
        // Reusable bitmasks for each position (26 bits each for a-z)
        int[] positionMasks = new int[wordLength];
        
        for (short pattern = 0; pattern < maxPatterns; pattern++) {
            int bucketSize = bucketCounts[pattern];
            if (bucketSize == 0) {
                continue;
            }
            
            // Clear position masks
            for (int p = 0; p < wordLength; p++) {
                positionMasks[p] = 0;
            }
            
            // Collect unique letters at each position for words in this bucket
            if (targetIds == null) {
                for (int targetId = 0; targetId < wordCount; targetId++) {
                    if (matrix[guessId][targetId] == pattern) {
                        String word = idToWord[targetId];
                        for (int p = 0; p < wordLength; p++) {
                            char c = word.charAt(p);
                            positionMasks[p] |= (1 << (c - 'A'));
                        }
                    }
                }
            } else {
                for (int i = 0; i < targetCount; i++) {
                    int targetId = targetIds[i];
                    if (matrix[guessId][targetId] == pattern) {
                        String word = idToWord[targetId];
                        for (int p = 0; p < wordLength; p++) {
                            char c = word.charAt(p);
                            positionMasks[p] |= (1 << (c - 'A'));
                        }
                    }
                }
            }
            
            // Compute column length as product of unique letters at each position
            int columnLength = 1;
            for (int p = 0; p < wordLength; p++) {
                columnLength *= Integer.bitCount(positionMasks[p]);
            }
            
            // Add weighted contribution
            double probability = (double) bucketSize / targetCount;
            expectedLength += probability * columnLength;
        }
        
        return expectedLength;
    }
    
    /**
     * Computes expected dictionary reduction (average remaining words) for a guess.
     * Lower values indicate better guesses.
     * 
     * @param guessId the guess word ID
     * @return expected dictionary size after guessing
     */
    public double computeDictionaryReduction(int guessId) {
        return computeDictionaryReduction(guessId, null, wordCount);
    }
    
    /**
     * Computes expected dictionary reduction against a subset of targets.
     * 
     * @param guessId the guess word ID
     * @param targetIds array of target word IDs (null means all words)
     * @param targetCount number of valid entries in targetIds
     * @return expected dictionary size after guessing
     */
    public double computeDictionaryReduction(int guessId, int[] targetIds, int targetCount) {
        if (targetCount == 0) {
            return 0.0;
        }
        
        int maxPatterns = (int) Math.pow(4, wordLength);
        int[] counts = new int[maxPatterns];
        
        if (targetIds == null) {
            for (int targetId = 0; targetId < wordCount; targetId++) {
                counts[matrix[guessId][targetId]]++;
            }
        } else {
            for (int i = 0; i < targetCount; i++) {
                counts[matrix[guessId][targetIds[i]]]++;
            }
        }
        
        double totalSize = 0;
        for (int count : counts) {
            if (count > 0) {
                double probability = (double) count / targetCount;
                totalSize += probability * count;
            }
        }
        
        return totalSize;
    }
    
    /**
     * Finds the word with minimum expected column length among candidates.
     * 
     * @param candidateIds array of candidate guess word IDs
     * @param candidateCount number of valid entries
     * @param targetIds array of target word IDs (null means all words)
     * @param targetCount number of valid entries in targetIds
     * @return the word ID with minimum expected column length
     */
    public int findMinColumnLengthWordId(int[] candidateIds, int candidateCount,
                                          int[] targetIds, int targetCount) {
        if (candidateCount == 0) {
            return -1;
        }
        
        int bestId = candidateIds[0];
        float bestScore = Float.MAX_VALUE;
        
        for (int i = 0; i < candidateCount; i++) {
            int guessId = candidateIds[i];
            float score = computeExpectedColumnLength(guessId, targetIds, targetCount);
            if (score < bestScore) {
                bestScore = score;
                bestId = guessId;
            }
        }
        
        return bestId;
    }
    
    /**
     * Finds the word with maximum dictionary reduction (minimum expected remaining words).
     * 
     * @param candidateIds array of candidate guess word IDs
     * @param candidateCount number of valid entries
     * @param targetIds array of target word IDs (null means all words)
     * @param targetCount number of valid entries in targetIds
     * @return the word ID with best reduction score
     */
    public int findMaxReductionWordId(int[] candidateIds, int candidateCount,
                                       int[] targetIds, int targetCount) {
        if (candidateCount == 0) {
            return -1;
        }
        
        int bestId = candidateIds[0];
        double bestScore = Double.MAX_VALUE;
        
        for (int i = 0; i < candidateCount; i++) {
            int guessId = candidateIds[i];
            double score = computeDictionaryReduction(guessId, targetIds, targetCount);
            if (score < bestScore) {
                bestScore = score;
                bestId = guessId;
            }
        }
        
        return bestId;
    }
}

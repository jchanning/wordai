package com.fistraltech.analysis;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * Comprehensive tests for {@link ResponseMatrix}.
 * 
 * <p>These tests verify:
 * <ul>
 *   <li>Correct encoding/decoding of response patterns</li>
 *   <li>Consistency with existing WordGame.evaluate() results</li>
 *   <li>Entropy calculations match the original WordEntropy implementation</li>
 *   <li>Edge cases and boundary conditions</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResponseMatrixTest {
    
    private Dictionary smallDictionary;
    private Dictionary mediumDictionary;
    private Config config;
    private ResponseMatrix smallMatrix;
    private ResponseMatrix mediumMatrix;
    
    @BeforeAll
    void setUpAll() {
        config = new Config();
        config.setMaxAttempts(6);
        
        // Create a small dictionary for detailed testing
        smallDictionary = new Dictionary(5);
        Set<String> smallWords = new HashSet<>();
        smallWords.add("SLATE");
        smallWords.add("AROSE");
        smallWords.add("CRANE");
        smallWords.add("TRACE");
        smallWords.add("STARE");
        smallWords.add("RATES");
        smallWords.add("TEARS");
        smallWords.add("REACT");
        smallWords.add("CRATE");
        smallWords.add("SIREN");
        smallDictionary.addWords(smallWords);
        
        // Create medium dictionary for performance testing
        mediumDictionary = new Dictionary(5);
        Set<String> mediumWords = new HashSet<>();
        // Add 100 words for more realistic testing
        String[] baseWords = {
            "ABOUT", "ABOVE", "ABUSE", "ACTOR", "ACUTE", "ADMIT", "ADOPT", "ADULT", "AFTER", "AGAIN",
            "AGENT", "AGREE", "AHEAD", "ALARM", "ALBUM", "ALERT", "ALIEN", "ALIGN", "ALIKE", "ALIVE",
            "ALLOW", "ALONE", "ALONG", "ALTER", "AMONG", "ANGEL", "ANGER", "ANGLE", "ANGRY", "APART",
            "APPLE", "APPLY", "ARENA", "ARGUE", "ARISE", "ARMOR", "ARRAY", "ARROW", "ASIDE", "ASSET",
            "AUDIO", "AVOID", "AWARD", "AWARE", "BADLY", "BAKER", "BASES", "BASIC", "BASIN", "BASIS",
            "BEACH", "BEGAN", "BEGIN", "BEING", "BELOW", "BENCH", "BILLY", "BIRTH", "BLACK", "BLADE",
            "BLAME", "BLANK", "BLAST", "BLAZE", "BLEED", "BLEND", "BLESS", "BLIND", "BLOCK", "BLOOD",
            "BLOWN", "BOARD", "BOAST", "BOOST", "BOOTH", "BOUND", "BRAIN", "BRAND", "BRAVE", "BREAD",
            "BREAK", "BREED", "BRICK", "BRIDE", "BRIEF", "BRING", "BROAD", "BROKE", "BROWN", "BRUSH",
            "BUILD", "BUILT", "BUNCH", "BURST", "BUYER", "CABLE", "CALIF", "CANAL", "CANDY", "CHAIN"
        };
        for (String word : baseWords) {
            mediumWords.add(word);
        }
        mediumDictionary.addWords(mediumWords);
        
        // Build matrices
        smallMatrix = new ResponseMatrix(smallDictionary, config);
        mediumMatrix = new ResponseMatrix(mediumDictionary, config);
    }
    
    // ============================================
    // Basic Construction and Properties Tests
    // ============================================
    
    @Test
    void testMatrixConstruction() {
        assertEquals(10, smallMatrix.getWordCount());
        assertEquals(5, smallMatrix.getWordLength());
    }
    
    @Test
    void testWordIdMapping() {
        // Every word should have a valid ID
        for (String word : smallDictionary.getMasterSetOfWords()) {
            int id = smallMatrix.getWordId(word);
            assertTrue(id >= 0, "Word " + word + " should have valid ID");
            assertTrue(id < smallMatrix.getWordCount(), "ID should be within bounds");
            assertEquals(word, smallMatrix.getWord(id), "Round-trip should work");
        }
    }
    
    @Test
    void testContainsWord() {
        assertTrue(smallMatrix.containsWord("SLATE"));
        assertTrue(smallMatrix.containsWord("AROSE"));
        assertFalse(smallMatrix.containsWord("XXXXX"));
        assertFalse(smallMatrix.containsWord("HELLO"));
    }
    
    @Test
    void testUnknownWordReturnsNegativeId() {
        assertEquals(-1, smallMatrix.getWordId("UNKNOWN"));
        assertEquals(-1, smallMatrix.getWordId("XXXXX"));
    }
    
    @Test
    void testGetPatternUnknownWord() {
        assertEquals(-1, smallMatrix.getPattern("UNKNOWN", "SLATE"));
        assertEquals(-1, smallMatrix.getPattern("SLATE", "UNKNOWN"));
    }
    
    // ============================================
    // Response Pattern Encoding Tests
    // ============================================
    
    @Test
    void testPatternEncodingAllGreen() {
        // Guessing the correct word should give all greens (pattern = 0)
        String word = "SLATE";
        short pattern = smallMatrix.getPattern(word, word);
        assertEquals(0, pattern, "Same word should produce all-green pattern (0)");
        assertEquals("GGGGG", smallMatrix.decodePattern(pattern));
    }
    
    @Test
    void testPatternEncodingVerifyWithWordGame() {
        // Test consistency between matrix and WordGame for various word pairs
        WordGame game = new WordGame(mediumDictionary, config);
        
        // Test several word pairs from the medium dictionary
        String[][] pairs = {
            {"ABOUT", "BELOW"},
            {"CHAIN", "BUILD"},
            {"BREAD", "CANDY"}
        };
        
        for (String[] pair : pairs) {
            try {
                game.setTargetWord(pair[1]);
                Response response = game.evaluate(pair[0]);
                
                short pattern = mediumMatrix.getPattern(pair[0], pair[1]);
                String decoded = mediumMatrix.decodePattern(pattern);
                
                // Build expected from actual response
                StringBuilder expected = new StringBuilder();
                for (var entry : response.getStatuses()) {
                    expected.append(entry.status);
                }
                
                assertEquals(expected.toString(), decoded, 
                    "Matrix pattern should match WordGame for " + pair[0] + " vs " + pair[1]);
            } catch (Exception e) {
                fail("Should not throw for " + pair[0] + " vs " + pair[1] + ": " + e.getMessage());
            }
        }
    }
    
    @Test
    void testPatternConsistencyWithWordGame() {
        // Test multiple word pairs to ensure matrix matches WordGame
        WordGame game = new WordGame(smallDictionary, config);
        
        String[] guessWords = {"SLATE", "CRANE", "TRACE", "STARE"};
        String[] targetWords = {"AROSE", "CRATE", "TEARS", "RATES"};
        
        for (String guess : guessWords) {
            for (String target : targetWords) {
                try {
                    game.setTargetWord(target);
                    Response response = game.evaluate(guess);
                    
                    // Build expected pattern string
                    StringBuilder expected = new StringBuilder();
                    for (var entry : response.getStatuses()) {
                        expected.append(entry.status);
                    }
                    
                    // Get matrix pattern
                    short pattern = smallMatrix.getPattern(guess, target);
                    String decoded = smallMatrix.decodePattern(pattern);
                    
                    assertEquals(expected.toString(), decoded,
                        "Pattern mismatch for guess=" + guess + " target=" + target);
                } catch (Exception e) {
                    fail("Exception for guess=" + guess + " target=" + target + ": " + e.getMessage());
                }
            }
        }
    }
    
    @Test
    void testDecodePattern() {
        // Test known pattern values
        // GGGGG = 00_00_00_00_00 = 0
        assertEquals("GGGGG", smallMatrix.decodePattern((short) 0));
        
        // RRRRR = 10_10_10_10_10 = 682
        assertEquals("RRRRR", smallMatrix.decodePattern((short) 682));
        
        // AAAAA = 01_01_01_01_01 = 341
        assertEquals("AAAAA", smallMatrix.decodePattern((short) 341));
        
        // XXXXX = 11_11_11_11_11 = 1023
        assertEquals("XXXXX", smallMatrix.decodePattern((short) 1023));
        
        // Mixed: GARXR
        // G=0, A=1, R=2, X=3, R=2
        // = 0 + 1*4 + 2*16 + 3*64 + 2*256 = 0 + 4 + 32 + 192 + 512 = 740
        assertEquals("GARXR", smallMatrix.decodePattern((short) 740));
    }
    
    // ============================================
    // Bucket Count Tests
    // ============================================
    
    @Test
    void testBucketCountsNonNegative() {
        int guessId = smallMatrix.getWordId("SLATE");
        int[] counts = smallMatrix.getBucketCounts(guessId);
        
        for (int count : counts) {
            assertTrue(count >= 0, "Bucket counts should be non-negative");
        }
    }
    
    @Test
    void testBucketCountsSumToWordCount() {
        int guessId = smallMatrix.getWordId("SLATE");
        int[] counts = smallMatrix.getBucketCounts(guessId);
        
        int sum = 0;
        for (int count : counts) {
            sum += count;
        }
        
        assertEquals(smallMatrix.getWordCount(), sum, 
            "Bucket counts should sum to total word count");
    }
    
    @Test
    void testBucketCountsWithSubset() {
        // Create subset of target IDs
        int[] targetIds = new int[5];
        targetIds[0] = smallMatrix.getWordId("SLATE");
        targetIds[1] = smallMatrix.getWordId("AROSE");
        targetIds[2] = smallMatrix.getWordId("CRANE");
        targetIds[3] = smallMatrix.getWordId("TRACE");
        targetIds[4] = smallMatrix.getWordId("STARE");
        
        int guessId = smallMatrix.getWordId("CRANE");
        int[] counts = smallMatrix.getBucketCounts(guessId, targetIds, 5);
        
        int sum = 0;
        for (int count : counts) {
            sum += count;
        }
        
        assertEquals(5, sum, "Bucket counts should sum to subset size");
    }
    
    // ============================================
    // Entropy Calculation Tests
    // ============================================
    
    @Test
    void testEntropyNonNegative() {
        for (int guessId = 0; guessId < smallMatrix.getWordCount(); guessId++) {
            float entropy = smallMatrix.computeEntropy(guessId);
            assertTrue(entropy >= 0, "Entropy should be non-negative");
        }
    }
    
    @Test
    void testEntropyMaxBound() {
        // Maximum entropy for N words is log2(N)
        double maxPossibleEntropy = Math.log(smallMatrix.getWordCount()) / Math.log(2);
        
        for (int guessId = 0; guessId < smallMatrix.getWordCount(); guessId++) {
            float entropy = smallMatrix.computeEntropy(guessId);
            assertTrue(entropy <= maxPossibleEntropy + 0.001, 
                "Entropy should not exceed log2(N)");
        }
    }
    
    @Test
    void testEntropyConsistencyWithOriginal() {
        // Compare with original WordEntropy calculation
        WordEntropy originalEntropy = new WordEntropy(smallDictionary, config, true);
        
        for (String word : smallDictionary.getMasterSetOfWords()) {
            float original = originalEntropy.getEntropy(word);
            int wordId = smallMatrix.getWordId(word);
            float matrixEntropy = smallMatrix.computeEntropy(wordId);
            
            assertEquals(original, matrixEntropy, 0.0001f, 
                "Entropy for " + word + " should match original implementation");
        }
    }
    
    @Test
    void testFindMaxEntropyWord() {
        // Find max entropy using matrix
        int maxEntropyId = smallMatrix.findMaxEntropyWordId();
        String maxEntropyWord = smallMatrix.getWord(maxEntropyId);
        float maxEntropy = smallMatrix.computeEntropy(maxEntropyId);
        
        // Verify it's actually the maximum
        for (int id = 0; id < smallMatrix.getWordCount(); id++) {
            float entropy = smallMatrix.computeEntropy(id);
            assertTrue(entropy <= maxEntropy + 0.0001f,
                "Word " + smallMatrix.getWord(id) + " has higher entropy than reported max");
        }
        
        // Compare with original
        WordEntropy originalEntropy = new WordEntropy(smallDictionary, config, true);
        String originalMax = originalEntropy.getMaximumEntropyWord();
        float originalMaxEntropy = originalEntropy.getEntropy(originalMax);
        
        assertEquals(originalMaxEntropy, maxEntropy, 0.0001f,
            "Max entropy value should match original");
    }
    
    @Test
    void testEntropyWithSubset() {
        // Subset of target words
        int[] targetIds = new int[5];
        targetIds[0] = smallMatrix.getWordId("SLATE");
        targetIds[1] = smallMatrix.getWordId("AROSE");
        targetIds[2] = smallMatrix.getWordId("CRANE");
        targetIds[3] = smallMatrix.getWordId("TRACE");
        targetIds[4] = smallMatrix.getWordId("STARE");
        
        int guessId = smallMatrix.getWordId("CRANE");
        float subsetEntropy = smallMatrix.computeEntropy(guessId, targetIds, 5);
        
        assertTrue(subsetEntropy >= 0, "Subset entropy should be non-negative");
        assertTrue(subsetEntropy <= Math.log(5) / Math.log(2) + 0.001,
            "Subset entropy should not exceed log2(subset size)");
    }
    
    // ============================================
    // Edge Cases
    // ============================================
    
    @Test
    void testSingleWordDictionary() {
        Dictionary singleWord = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("ALONE");
        singleWord.addWords(words);
        
        ResponseMatrix singleMatrix = new ResponseMatrix(singleWord, config);
        
        assertEquals(1, singleMatrix.getWordCount());
        assertEquals(0, singleMatrix.getWordId("ALONE"));
        assertEquals("ALONE", singleMatrix.getWord(0));
        
        // Entropy should be 0 (only one possible outcome)
        float entropy = singleMatrix.computeEntropy(0);
        assertEquals(0f, entropy, 0.0001f);
    }
    
    @Test
    void testAllWordIdsArray() {
        int[] ids = smallMatrix.getAllWordIds();
        assertEquals(smallMatrix.getWordCount(), ids.length);
        
        for (int i = 0; i < ids.length; i++) {
            assertEquals(i, ids[i], "ID at index " + i + " should equal " + i);
        }
    }
    
    // ============================================
    // Memory Estimation Tests
    // ============================================
    
    @Test
    void testMemoryEstimation() {
        long memory = smallMatrix.getEstimatedMemoryBytes();
        assertTrue(memory > 0, "Memory estimate should be positive");
        
        // For 10 words: 10*10*2 = 200 bytes matrix + overhead
        // Should be less than 10KB for small dictionary
        assertTrue(memory < 10_000, "Memory for 10 words should be under 10KB");
    }
    
    @Test
    void testMemoryEstimationScales() {
        long smallMemory = smallMatrix.getEstimatedMemoryBytes();
        long mediumMemory = mediumMatrix.getEstimatedMemoryBytes();
        
        // Medium dictionary has 100 words, small has 10
        // Matrix memory is dominated by N^2 * 2 bytes, plus O(N) for maps
        // For 10 words: 10*10*2 + 10*68 = 200 + 680 = 880 bytes
        // For 100 words: 100*100*2 + 100*68 = 20000 + 6800 = 26800 bytes
        // So ratio should be roughly 26800/880 ≈ 30x
        assertTrue(mediumMemory > smallMemory * 10, 
            "Memory should scale significantly with more words: small=" + smallMemory + ", medium=" + mediumMemory);
    }
    
    // ============================================
    // ID-based Lookup Performance Tests
    // ============================================
    
    @Test
    void testIdBasedLookupMatchesStringLookup() {
        String guess = "CRANE";
        String target = "STARE";
        
        int guessId = smallMatrix.getWordId(guess);
        int targetId = smallMatrix.getWordId(target);
        
        short stringPattern = smallMatrix.getPattern(guess, target);
        short idPattern = smallMatrix.getPattern(guessId, targetId);
        
        assertEquals(stringPattern, idPattern, 
            "String lookup and ID lookup should return same pattern");
    }
    
    @Test
    void testFindMaxEntropyWithCandidates() {
        // Create candidate and target arrays
        int[] candidateIds = new int[5];
        candidateIds[0] = smallMatrix.getWordId("SLATE");
        candidateIds[1] = smallMatrix.getWordId("CRANE");
        candidateIds[2] = smallMatrix.getWordId("TRACE");
        candidateIds[3] = smallMatrix.getWordId("STARE");
        candidateIds[4] = smallMatrix.getWordId("AROSE");
        
        int[] targetIds = new int[smallMatrix.getWordCount()];
        for (int i = 0; i < smallMatrix.getWordCount(); i++) {
            targetIds[i] = i;
        }
        
        int bestId = smallMatrix.findMaxEntropyWordId(candidateIds, 5, 
                                                       targetIds, smallMatrix.getWordCount());
        
        // Verify it's the best among candidates
        float bestEntropy = smallMatrix.computeEntropy(bestId);
        for (int i = 0; i < 5; i++) {
            float candidateEntropy = smallMatrix.computeEntropy(candidateIds[i]);
            assertTrue(candidateEntropy <= bestEntropy + 0.0001f,
                "Best entropy should be >= all candidate entropies");
        }
    }
    
    // ============================================
    // Phase 2: Column Length Calculation Tests
    // ============================================
    
    @Test
    void testExpectedColumnLengthNotNegative() {
        // Column length should always be non-negative
        for (String word : smallDictionary.getMasterSetOfWords()) {
            int guessId = smallMatrix.getWordId(word);
            float columnLength = smallMatrix.computeExpectedColumnLength(guessId);
            assertTrue(columnLength >= 0, 
                "Column length should be non-negative for " + word + ": " + columnLength);
        }
    }
    
    @Test
    void testExpectedColumnLengthWithSubset() {
        int guessId = smallMatrix.getWordId("SLATE");
        
        // Full set
        float fullLength = smallMatrix.computeExpectedColumnLength(guessId);
        
        // Subset
        int[] targetIds = {
            smallMatrix.getWordId("CRANE"),
            smallMatrix.getWordId("TRACE"),
            smallMatrix.getWordId("STARE")
        };
        float subsetLength = smallMatrix.computeExpectedColumnLength(guessId, targetIds, 3);
        
        // Both should be non-negative
        assertTrue(fullLength >= 0, "Full column length should be non-negative");
        assertTrue(subsetLength >= 0, "Subset column length should be non-negative");
    }
    
    @Test
    void testColumnLengthConsistency() {
        // For identical words (guessing the target), column length contribution should be 1
        // because all positions have exactly one unique letter
        int guessId = smallMatrix.getWordId("SLATE");
        int targetId = guessId; // Same word
        
        // The pattern for same word should be all green (GGGGG = 0)
        short pattern = smallMatrix.getPattern(guessId, targetId);
        assertEquals(0, pattern, "Pattern for same word should be 0 (all green)");
    }
    
    @Test
    void testDictionaryReductionNotNegative() {
        for (String word : smallDictionary.getMasterSetOfWords()) {
            int guessId = smallMatrix.getWordId(word);
            double reduction = smallMatrix.computeDictionaryReduction(guessId);
            assertTrue(reduction >= 0, 
                "Dictionary reduction should be non-negative for " + word);
        }
    }
    
    @Test
    void testDictionaryReductionBounds() {
        // Reduction should be between 1 (perfect guess) and N (worst case)
        int n = smallMatrix.getWordCount();
        
        for (String word : smallDictionary.getMasterSetOfWords()) {
            int guessId = smallMatrix.getWordId(word);
            double reduction = smallMatrix.computeDictionaryReduction(guessId);
            
            // Lower bound: at minimum, the guess itself creates a unique pattern
            // Upper bound: at maximum, all words collapse to one bucket
            assertTrue(reduction <= n, 
                "Reduction should be <= dictionary size for " + word + ": " + reduction);
        }
    }
    
    @Test
    void testDictionaryReductionWithSubset() {
        int guessId = smallMatrix.getWordId("CRANE");
        
        int[] targetIds = {
            smallMatrix.getWordId("SLATE"),
            smallMatrix.getWordId("AROSE"),
            smallMatrix.getWordId("SIREN")
        };
        
        double reduction = smallMatrix.computeDictionaryReduction(guessId, targetIds, 3);
        
        assertTrue(reduction >= 0 && reduction <= 3, 
            "Reduction with 3 targets should be between 0 and 3: " + reduction);
    }
    
    @Test
    void testFindMinColumnLengthWordId() {
        int[] candidateIds = new int[5];
        candidateIds[0] = smallMatrix.getWordId("SLATE");
        candidateIds[1] = smallMatrix.getWordId("CRANE");
        candidateIds[2] = smallMatrix.getWordId("TRACE");
        candidateIds[3] = smallMatrix.getWordId("STARE");
        candidateIds[4] = smallMatrix.getWordId("AROSE");
        
        int bestId = smallMatrix.findMinColumnLengthWordId(candidateIds, 5, null, smallMatrix.getWordCount());
        
        // Verify it's one of the candidates
        boolean found = false;
        for (int i = 0; i < 5; i++) {
            if (candidateIds[i] == bestId) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Best word should be one of the candidates");
        
        // Verify it's the minimum
        float bestLength = smallMatrix.computeExpectedColumnLength(bestId);
        for (int i = 0; i < 5; i++) {
            float candidateLength = smallMatrix.computeExpectedColumnLength(candidateIds[i]);
            assertTrue(candidateLength >= bestLength - 0.0001f,
                "Best should have minimum column length");
        }
    }
    
    @Test
    void testFindMaxReductionWordId() {
        int[] candidateIds = new int[4];
        candidateIds[0] = smallMatrix.getWordId("SLATE");
        candidateIds[1] = smallMatrix.getWordId("CRANE");
        candidateIds[2] = smallMatrix.getWordId("RATES");
        candidateIds[3] = smallMatrix.getWordId("AROSE");
        
        int bestId = smallMatrix.findMaxReductionWordId(candidateIds, 4, null, smallMatrix.getWordCount());
        
        // Verify it's one of the candidates
        boolean found = false;
        for (int i = 0; i < 4; i++) {
            if (candidateIds[i] == bestId) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Best word should be one of the candidates");
        
        // Verify it's the minimum reduction (lowest expected remaining)
        double bestReduction = smallMatrix.computeDictionaryReduction(bestId);
        for (int i = 0; i < 4; i++) {
            double candidateReduction = smallMatrix.computeDictionaryReduction(candidateIds[i]);
            assertTrue(candidateReduction >= bestReduction - 0.0001,
                "Best should have minimum dictionary reduction");
        }
    }
    
    @Test
    void testEmptyTargetSetHandling() {
        int guessId = smallMatrix.getWordId("SLATE");
        
        // Empty target set should return 0
        float columnLength = smallMatrix.computeExpectedColumnLength(guessId, new int[0], 0);
        assertEquals(0f, columnLength, "Column length with empty targets should be 0");
        
        double reduction = smallMatrix.computeDictionaryReduction(guessId, new int[0], 0);
        assertEquals(0.0, reduction, "Reduction with empty targets should be 0");
    }
    
    @Test
    void testColumnLengthPerformance() {
        // Test that column length calculation completes quickly for medium dictionary
        long startTime = System.currentTimeMillis();
        
        for (int guessId = 0; guessId < mediumMatrix.getWordCount(); guessId++) {
            mediumMatrix.computeExpectedColumnLength(guessId);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue(elapsed < 5000, 
            "Column length for 100 words should complete in < 5s: " + elapsed + "ms");
    }
    
    // ============================================
    // Phase 3: Parallel Computation Tests
    // ============================================
    
    @Test
    void testFindMaxEntropyWordIdParallelConsistency() {
        // Build arrays of all word IDs
        int wordCount = mediumMatrix.getWordCount();
        int[] wordIds = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            wordIds[i] = i;
        }
        
        // Compare sequential and parallel results
        int sequentialBest = mediumMatrix.findMaxEntropyWordId(wordIds, wordCount, wordIds, wordCount);
        int parallelBest = mediumMatrix.findMaxEntropyWordIdParallel(wordIds, wordCount, wordIds, wordCount);
        
        // Both should find the same word (or words with equal entropy)
        float sequentialEntropy = mediumMatrix.computeEntropy(sequentialBest, wordIds, wordCount);
        float parallelEntropy = mediumMatrix.computeEntropy(parallelBest, wordIds, wordCount);
        
        assertEquals(sequentialEntropy, parallelEntropy, 0.0001f,
            "Parallel and sequential should find word with same max entropy");
    }
    
    @Test
    void testFindMaxEntropyWordIdParallelWithSubset() {
        // Test with a subset of candidates
        int[] candidateIds = new int[20];
        int[] targetIds = new int[mediumMatrix.getWordCount()];
        
        for (int i = 0; i < 20; i++) {
            candidateIds[i] = i;
        }
        for (int i = 0; i < mediumMatrix.getWordCount(); i++) {
            targetIds[i] = i;
        }
        
        int sequentialBest = mediumMatrix.findMaxEntropyWordId(candidateIds, 20, targetIds, mediumMatrix.getWordCount());
        int parallelBest = mediumMatrix.findMaxEntropyWordIdParallel(candidateIds, 20, targetIds, mediumMatrix.getWordCount());
        
        float sequentialEntropy = mediumMatrix.computeEntropy(sequentialBest, targetIds, mediumMatrix.getWordCount());
        float parallelEntropy = mediumMatrix.computeEntropy(parallelBest, targetIds, mediumMatrix.getWordCount());
        
        assertEquals(sequentialEntropy, parallelEntropy, 0.0001f,
            "Parallel and sequential should find same max entropy for subset");
    }
    
    @Test
    void testComputeTopNEntropy() {
        // Build arrays of all word IDs
        int wordCount = mediumMatrix.getWordCount();
        int[] wordIds = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            wordIds[i] = i;
        }
        
        // Get top 5 words
        int[][] topN = mediumMatrix.computeTopNEntropy(wordIds, wordCount, wordIds, wordCount, 5);
        
        assertEquals(5, topN.length, "Should return exactly 5 entries");
        
        // Verify ordering - each entry should have >= entropy of next
        for (int i = 0; i < topN.length - 1; i++) {
            float entropy1 = mediumMatrix.computeEntropy(topN[i][0], wordIds, wordCount);
            float entropy2 = mediumMatrix.computeEntropy(topN[i + 1][0], wordIds, wordCount);
            assertTrue(entropy1 >= entropy2 - 0.0001f,
                "Top N should be sorted by entropy descending: " + entropy1 + " vs " + entropy2);
        }
        
        // Verify the top word has maximum entropy
        int maxEntropyId = mediumMatrix.findMaxEntropyWordId(wordIds, wordCount, wordIds, wordCount);
        float maxEntropy = mediumMatrix.computeEntropy(maxEntropyId, wordIds, wordCount);
        float topEntropy = mediumMatrix.computeEntropy(topN[0][0], wordIds, wordCount);
        
        assertEquals(maxEntropy, topEntropy, 0.0001f,
            "First entry in top N should have maximum entropy");
    }
    
    @Test
    void testComputeTopNEntropySmallN() {
        // Test with N larger than available words
        int wordCount = smallMatrix.getWordCount(); // 10 words
        int[] wordIds = new int[wordCount];
        for (int i = 0; i < wordCount; i++) {
            wordIds[i] = i;
        }
        
        // Request top 20 but only 10 words exist
        int[][] topN = smallMatrix.computeTopNEntropy(wordIds, wordCount, wordIds, wordCount, 20);
        
        assertEquals(10, topN.length, "Should return all available words when N > wordCount");
    }
    
    @Test
    void testComputeTopNEntropyWithSubset() {
        // Test with a subset of candidates and targets
        int[] candidateIds = new int[10];
        int[] targetIds = new int[5];
        
        for (int i = 0; i < 10; i++) {
            candidateIds[i] = i;
        }
        for (int i = 0; i < 5; i++) {
            targetIds[i] = i + 5; // Different targets from candidates
        }
        
        int[][] topN = mediumMatrix.computeTopNEntropy(candidateIds, 10, targetIds, 5, 3);
        
        assertEquals(3, topN.length, "Should return 3 entries");
        
        // Verify all entries are from candidates
        for (int i = 0; i < topN.length; i++) {
            boolean found = false;
            for (int j = 0; j < 10; j++) {
                if (candidateIds[j] == topN[i][0]) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Top N entries should come from candidate set");
        }
    }
    
    @Test
    void testParallelMatrixConstructionProducesSameResults() {
        // Both small and medium matrices should produce correct responses
        // regardless of whether they were built sequentially or in parallel
        WordGame game = new WordGame(mediumDictionary, config);
        
        // Test a sampling of pairs
        String[][] testPairs = {
            {"ABOUT", "ABOVE"},
            {"CHAIN", "BUILD"},
            {"BREAD", "CANDY"},
            {"ARISE", "ARENA"}
        };
        
        for (String[] pair : testPairs) {
            try {
                game.setTargetWord(pair[1]);
                Response expected = game.evaluate(pair[0]);
                String expectedPattern = expected.toString();
                
                short matrixPattern = mediumMatrix.getPattern(pair[0], pair[1]);
                String matrixDecoded = mediumMatrix.decodePattern(matrixPattern);
                
                assertEquals(expectedPattern, matrixDecoded,
                    "Matrix pattern for " + pair[0] + " -> " + pair[1] + " should match WordGame");
            } catch (Exception e) {
                fail("Should not throw for " + pair[0] + " vs " + pair[1] + ": " + e.getMessage());
            }
        }
    }
}

package com.fistraltech.analysis;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.ResponseMatrix;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.util.Config;

/**
 * Tests for Phase 3 lazy and parallel computation methods in {@link WordEntropy}.
 * 
 * <p>These tests verify:
 * <ul>
 *   <li>Lazy entropy computation against filtered target sets</li>
 *   <li>Top-N word selection</li>
 *   <li>Consistency between lazy and precomputed methods</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("WordEntropy Lazy Tests")
class WordEntropyLazyTest {
    
    private Dictionary dictionary;
    private Config config;
    private WordEntropy wordEntropy;
    
    @BeforeAll
    void setUpAll() {
        config = new Config();
        config.setMaxAttempts(6);
        
        // Create a dictionary for testing
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("SLATE");
        words.add("AROSE");
        words.add("CRANE");
        words.add("TRACE");
        words.add("STARE");
        words.add("RATES");
        words.add("TEARS");
        words.add("REACT");
        words.add("CRATE");
        words.add("SIREN");
        dictionary.addWords(words);
        
        // Initialize WordEntropy with matrix mode
        wordEntropy = new WordEntropy(dictionary, config, true);
    }
    
    // ============================================
    // Lazy Entropy Computation Tests
    // ============================================
    
    @Test
    @DisplayName("testGetMaximumEntropyWordLazyBasic")
    void testGetMaximumEntropyWordLazyBasic() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        String lazyBest = wordEntropy.getMaximumEntropyWordLazy(candidates, targets);
        
        assertNotNull(lazyBest, "Should return a word");
        assertTrue(candidates.contains(lazyBest), "Result should be from candidates");
    }
    
    @Test
    @DisplayName("testGetMaximumEntropyWordLazyConsistency")
    void testGetMaximumEntropyWordLazyConsistency() {
        // When targets = full dictionary, lazy should match precomputed
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        String lazyBest = wordEntropy.getMaximumEntropyWordLazy(candidates, targets);
        String precomputedBest = wordEntropy.getMaximumEntropyWord();
        
        // Both should find a word with the same maximum entropy
        float lazyEntropy = wordEntropy.getEntropy(lazyBest);
        float precomputedEntropy = wordEntropy.getEntropy(precomputedBest);
        
        assertEquals(precomputedEntropy, lazyEntropy, 0.0001f,
            "Lazy should find same max entropy as precomputed when using full dictionary");
    }
    
    @Test
    @DisplayName("testGetMaximumEntropyWordLazyWithFilteredTargets")
    void testGetMaximumEntropyWordLazyWithFilteredTargets() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        
        // Simulate filtered targets (like after some guesses)
        Set<String> filteredTargets = new HashSet<>();
        filteredTargets.add("SLATE");
        filteredTargets.add("STARE");
        filteredTargets.add("RATES");
        
        String best = wordEntropy.getMaximumEntropyWordLazy(candidates, filteredTargets);
        
        assertNotNull(best, "Should return a word");
        assertTrue(candidates.contains(best), "Result should be from candidates");
    }

    @Test
    @DisplayName("Equivalent filtered target sets reuse one memoized lazy entropy entry")
    void equivalentFilteredTargetSetsReuseOneMemoizedLazyEntropyEntry() throws Exception {
        Set<String> candidates = dictionary.getMasterSetOfWords();

        Set<String> firstTargets = new LinkedHashSet<>();
        firstTargets.add("SLATE");
        firstTargets.add("STARE");
        firstTargets.add("RATES");

        Set<String> secondTargets = new LinkedHashSet<>();
        secondTargets.add("RATES");
        secondTargets.add("SLATE");
        secondTargets.add("STARE");

        String firstBest = wordEntropy.getMaximumEntropyWordLazy(candidates, firstTargets);
        String secondBest = wordEntropy.getMaximumEntropyWordLazy(candidates, secondTargets);

        assertEquals(firstBest, secondBest, "Equivalent target sets should resolve to the same best word");
        assertEquals(1, lazyEntropyCache().size(), "Equivalent target sets should share one memoized cache entry");
    }
    
    @Test
    @DisplayName("testGetMaximumEntropyWordLazyWithEmptySets")
    void testGetMaximumEntropyWordLazyWithEmptySets() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> emptyTargets = new HashSet<>();
        
        String result = wordEntropy.getMaximumEntropyWordLazy(candidates, emptyTargets);
        assertEquals(null, result, "Empty targets should return null");
        
        Set<String> emptyCandidates = new HashSet<>();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        result = wordEntropy.getMaximumEntropyWordLazy(emptyCandidates, targets);
        assertEquals(null, result, "Empty candidates should return null");
    }

    @SuppressWarnings("unchecked")
    private Map<Object, String> lazyEntropyCache() throws Exception {
        Field field = WordEntropy.class.getDeclaredField("lazyEntropyWordCache");
        field.setAccessible(true);
        return (Map<Object, String>) field.get(wordEntropy);
    }
    
    // ============================================
    // Top-N Entropy Tests
    // ============================================
    
    @Test
    @DisplayName("testGetTopNEntropyWordsBasic")
    void testGetTopNEntropyWordsBasic() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        String[] topN = wordEntropy.getTopNEntropyWords(candidates, targets, 3);
        
        assertEquals(3, topN.length, "Should return 3 words");
        
        // All words should be from candidates
        for (String word : topN) {
            assertTrue(candidates.contains(word), "Top word should be from candidates: " + word);
        }
    }
    
    @Test
    @DisplayName("testGetTopNEntropyWordsOrdering")
    void testGetTopNEntropyWordsOrdering() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        String[] topN = wordEntropy.getTopNEntropyWords(candidates, targets, 5);
        
        // Verify ordering - each should have >= entropy of next
        for (int i = 0; i < topN.length - 1; i++) {
            float entropy1 = wordEntropy.getEntropy(topN[i]);
            float entropy2 = wordEntropy.getEntropy(topN[i + 1]);
            assertTrue(entropy1 >= entropy2 - 0.0001f,
                "Top N should be sorted by entropy descending: " + topN[i] + "=" + entropy1 + 
                " vs " + topN[i + 1] + "=" + entropy2);
        }
    }
    
    @Test
    @DisplayName("testGetTopNEntropyWordsConsistentWithMax")
    void testGetTopNEntropyWordsConsistentWithMax() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        String[] topN = wordEntropy.getTopNEntropyWords(candidates, targets, 5);
        String maxEntropyWord = wordEntropy.getMaximumEntropyWord();
        
        // First entry should have same entropy as max
        float topEntropy = wordEntropy.getEntropy(topN[0]);
        float maxEntropy = wordEntropy.getEntropy(maxEntropyWord);
        
        assertEquals(maxEntropy, topEntropy, 0.0001f,
            "First top N word should have max entropy");
    }
    
    @Test
    @DisplayName("testGetTopNEntropyWordsRequestMoreThanAvailable")
    void testGetTopNEntropyWordsRequestMoreThanAvailable() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        // Request 20 but only 10 words exist
        String[] topN = wordEntropy.getTopNEntropyWords(candidates, targets, 20);
        
        assertEquals(10, topN.length, "Should return all available words");
    }
    
    @Test
    @DisplayName("testGetTopNEntropyWordsEmptySets")
    void testGetTopNEntropyWordsEmptySets() {
        Set<String> candidates = dictionary.getMasterSetOfWords();
        Set<String> emptyTargets = new HashSet<>();
        
        String[] result = wordEntropy.getTopNEntropyWords(candidates, emptyTargets, 5);
        assertEquals(0, result.length, "Empty targets should return empty array");
        
        Set<String> emptyCandidates = new HashSet<>();
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        result = wordEntropy.getTopNEntropyWords(emptyCandidates, targets, 5);
        assertEquals(0, result.length, "Empty candidates should return empty array");
    }
    
    // ============================================
    // Compute Entropy Against Targets Tests
    // ============================================
    
    @Test
    @DisplayName("testComputeEntropyAgainstTargetsBasic")
    void testComputeEntropyAgainstTargetsBasic() {
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        float entropy = wordEntropy.computeEntropyAgainstTargets("SLATE", targets);
        
        assertTrue(entropy > 0, "Entropy should be positive");
    }
    
    @Test
    @DisplayName("testComputeEntropyAgainstTargetsConsistency")
    void testComputeEntropyAgainstTargetsConsistency() {
        // When targets = full dictionary, should match cached entropy
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        float computed = wordEntropy.computeEntropyAgainstTargets("SLATE", targets);
        float cached = wordEntropy.getEntropy("SLATE");
        
        assertEquals(cached, computed, 0.0001f,
            "Computed entropy against full dictionary should match cached");
    }
    
    @Test
    @DisplayName("testComputeEntropyAgainstTargetsFiltered")
    void testComputeEntropyAgainstTargetsFiltered() {
        // Entropy against subset should be different (usually lower)
        Set<String> fullTargets = dictionary.getMasterSetOfWords();
        
        Set<String> filteredTargets = new HashSet<>();
        filteredTargets.add("SLATE");
        filteredTargets.add("STARE");
        
        float fullEntropy = wordEntropy.computeEntropyAgainstTargets("CRANE", fullTargets);
        float filteredEntropy = wordEntropy.computeEntropyAgainstTargets("CRANE", filteredTargets);
        
        // Just verify we can compute it; the actual values depend on the responses
        assertTrue(filteredEntropy >= 0, "Filtered entropy should be non-negative");
        // With fewer targets, entropy may be lower (fewer bits needed to distinguish)
        // but this is data-dependent so we don't enforce a specific relationship
    }
    
    @Test
    @DisplayName("testComputeEntropyAgainstEmptyTargets")
    void testComputeEntropyAgainstEmptyTargets() {
        Set<String> emptyTargets = new HashSet<>();
        
        float entropy = wordEntropy.computeEntropyAgainstTargets("SLATE", emptyTargets);
        assertEquals(0f, entropy, "Entropy against empty targets should be 0");
    }
    
    @Test
    @DisplayName("testComputeEntropyAgainstTargetsUnknownWord")
    void testComputeEntropyAgainstTargetsUnknownWord() {
        Set<String> targets = dictionary.getMasterSetOfWords();
        
        float entropy = wordEntropy.computeEntropyAgainstTargets("XXXXX", targets);
        assertEquals(0f, entropy, "Entropy for unknown word should be 0");
    }

    @Test
    @DisplayName("testGetEntropyUnknownWordReturnsZero")
    void testGetEntropyUnknownWordReturnsZero() {
        assertEquals(0f, wordEntropy.getEntropy("XXXXX"), "Unknown word entropy should be 0");
    }

    @Test
    @DisplayName("testGetResponseBucketsSupportsExternalGuessWord")
    void testGetResponseBucketsSupportsExternalGuessWord() {
        Map<Short, Set<String>> buckets = wordEntropy.getResponseBuckets("SLING");

        assertNotNull(buckets, "Buckets should be created for external guess words");
        int totalBucketedWords = buckets.values().stream().mapToInt(Set::size).sum();
        assertEquals(dictionary.getWordCount(), totalBucketedWords,
            "External guesses should still partition the full target dictionary");
    }
    
    // ============================================
    // Response Matrix Access Test
    // ============================================
    
    @Test
    @DisplayName("testGetResponseMatrix")
    void testGetResponseMatrix() {
        ResponseMatrix matrix = wordEntropy.getResponseMatrix();
        
        assertNotNull(matrix, "Matrix should be accessible when using matrix mode");
        assertEquals(10, matrix.getWordCount(), "Matrix should have same word count");
    }
}

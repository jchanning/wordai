package com.fistraltech.analysis;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fistraltech.core.Dictionary;
import com.fistraltech.util.Config;

/**
 * Tests for WordIdSet - memory-efficient integer set implementation.
 */
@DisplayName("WordIdSet Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WordIdSetTest {
    
    private Dictionary dictionary;
    private Config config;
    private ResponseMatrix matrix;
    
    @BeforeAll
    void setUpAll() {
        config = new Config();
        config.setMaxAttempts(6);
        
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("APPLE");
        words.add("BREAD");
        words.add("CRANE");
        words.add("DELTA");
        words.add("EAGLE");
        words.add("FLAME");
        words.add("GRAPE");
        words.add("HOUSE");
        words.add("IMAGE");
        words.add("JUICE");
        dictionary.addWords(words);
        
        matrix = new ResponseMatrix(dictionary, config);
    }
    
    @Test
    @DisplayName("Empty set has size 0")
    void testEmptySet() {
        WordIdSet set = WordIdSet.empty();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }
    
    @Test
    @DisplayName("All creates full set")
    void testAll() {
        WordIdSet all = WordIdSet.all(10);
        assertEquals(10, all.size());
        assertFalse(all.isEmpty());
        
        for (int i = 0; i < 10; i++) {
            assertTrue(all.contains(i));
        }
    }
    
    @Test
    @DisplayName("Of creates set from varargs")
    void testOf() {
        WordIdSet set = WordIdSet.of(0, 2, 4, 6, 8);
        assertEquals(5, set.size());
        
        assertTrue(set.contains(0));
        assertTrue(set.contains(2));
        assertTrue(set.contains(4));
        assertFalse(set.contains(1));
        assertFalse(set.contains(3));
    }
    
    @Test
    @DisplayName("Get returns correct ID at index")
    void testGet() {
        WordIdSet set = WordIdSet.of(3, 5, 7);
        
        assertEquals(3, set.get(0));
        assertEquals(5, set.get(1));
        assertEquals(7, set.get(2));
    }
    
    @Test
    @DisplayName("Get throws on invalid index")
    void testGetThrows() {
        WordIdSet set = WordIdSet.of(1, 2, 3);
        
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> set.get(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> set.get(3));
    }
    
    @Test
    @DisplayName("Iterator iterates over all IDs")
    void testIterator() {
        WordIdSet set = WordIdSet.of(1, 3, 5, 7, 9);
        
        Set<Integer> collected = new HashSet<>();
        for (Integer id : set) {
            collected.add(id);
        }
        
        assertEquals(Set.of(1, 3, 5, 7, 9), collected);
    }
    
    @Test
    @DisplayName("Iterator throws on exhausted")
    void testIteratorExhausted() {
        WordIdSet set = WordIdSet.of(0);
        
        var iter = set.iterator();
        iter.next(); // First element
        assertThrows(NoSuchElementException.class, iter::next);
    }
    
    @Test
    @DisplayName("Primitive iterator avoids boxing")
    void testPrimitiveIterator() {
        WordIdSet set = WordIdSet.of(2, 4, 6);
        
        var iter = set.primitiveIterator();
        int sum = 0;
        while (iter.hasNext()) {
            sum += iter.nextInt();
        }
        
        assertEquals(12, sum); // 2 + 4 + 6
    }
    
    @Test
    @DisplayName("Primitive iterator reset works")
    void testPrimitiveIteratorReset() {
        WordIdSet set = WordIdSet.of(1, 2);
        
        var iter = set.primitiveIterator();
        iter.nextInt();
        iter.nextInt();
        assertFalse(iter.hasNext());
        
        iter.reset();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
    }
    
    @Test
    @DisplayName("Get IDs returns internal array")
    void testGetIds() {
        WordIdSet set = WordIdSet.of(1, 2, 3);
        int[] ids = set.getIds();
        
        assertEquals(3, ids.length);
        assertEquals(1, ids[0]);
        assertEquals(2, ids[1]);
        assertEquals(3, ids[2]);
    }
    
    @Test
    @DisplayName("Filter with predicate")
    void testFilter() {
        WordIdSet set = WordIdSet.of(1, 2, 3, 4, 5, 6);
        
        // Keep only even numbers
        WordIdSet filtered = set.filter(id -> id % 2 == 0);
        
        assertEquals(3, filtered.size());
        assertTrue(filtered.contains(2));
        assertTrue(filtered.contains(4));
        assertTrue(filtered.contains(6));
        assertFalse(filtered.contains(1));
    }
    
    @Test
    @DisplayName("Filter by pattern removes non-matching words")
    void testFilterByPattern() {
        WordIdSet all = WordIdSet.all(matrix.getWordCount());
        
        int guessId = matrix.getWordId("CRANE");
        int targetId = matrix.getWordId("FLAME");
        short pattern = matrix.getPattern(guessId, targetId);
        
        WordIdSet filtered = all.filterByPattern(matrix, guessId, pattern);
        
        // Should contain at least the target
        assertTrue(filtered.size() > 0);
        assertTrue(filtered.contains(targetId));
    }
    
    @Test
    @DisplayName("Get bucket counts works correctly")
    void testGetBucketCounts() {
        WordIdSet all = WordIdSet.all(matrix.getWordCount());
        
        int guessId = matrix.getWordId("CRANE");
        int maxPatterns = (int) Math.pow(4, 5); // 1024 for 5-letter words
        
        int[] counts = all.getBucketCounts(matrix, guessId, maxPatterns);
        
        // Sum of counts should equal total words
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        assertEquals(matrix.getWordCount(), total);
    }
    
    @Test
    @DisplayName("Get bucket counts with pre-allocated array")
    void testGetBucketCountsPreallocated() {
        WordIdSet all = WordIdSet.all(matrix.getWordCount());
        
        int guessId = matrix.getWordId("CRANE");
        int maxPatterns = (int) Math.pow(4, 5);
        int[] counts = new int[maxPatterns];
        
        all.getBucketCounts(matrix, guessId, counts);
        
        int total = 0;
        for (int count : counts) {
            total += count;
        }
        assertEquals(matrix.getWordCount(), total);
    }
    
    @Test
    @DisplayName("FromStrings creates set from word set")
    void testFromStrings() {
        Set<String> words = Set.of("CRANE", "FLAME", "HOUSE");
        
        WordIdSet set = WordIdSet.fromStrings(words, matrix);
        
        assertEquals(3, set.size());
        assertTrue(set.contains(matrix.getWordId("CRANE")));
        assertTrue(set.contains(matrix.getWordId("FLAME")));
        assertTrue(set.contains(matrix.getWordId("HOUSE")));
    }
    
    @Test
    @DisplayName("FromStrings handles unknown words")
    void testFromStringsUnknownWords() {
        Set<String> words = Set.of("CRANE", "ZZZZZ", "HOUSE");
        
        WordIdSet set = WordIdSet.fromStrings(words, matrix);
        
        // Should only contain the two known words
        assertEquals(2, set.size());
    }
    
    @Test
    @DisplayName("Equals and hashCode")
    void testEqualsHashCode() {
        WordIdSet set1 = WordIdSet.of(1, 2, 3);
        WordIdSet set2 = WordIdSet.of(1, 2, 3);
        WordIdSet set3 = WordIdSet.of(1, 2, 4);
        
        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
        assertNotEquals(set1, set3);
    }
    
    @Test
    @DisplayName("ToString format")
    void testToString() {
        WordIdSet empty = WordIdSet.empty();
        assertEquals("WordIdSet[]", empty.toString());
        
        WordIdSet small = WordIdSet.of(1, 2, 3);
        assertTrue(small.toString().contains("1"));
        assertTrue(small.toString().contains("2"));
        assertTrue(small.toString().contains("3"));
    }
    
    @Test
    @DisplayName("Large set performance")
    void testLargeSetPerformance() {
        int size = 100000;
        int[] ids = new int[size];
        for (int i = 0; i < size; i++) {
            ids[i] = i;
        }
        
        long start = System.currentTimeMillis();
        WordIdSet set = WordIdSet.of(ids);
        
        // Test contains performance
        for (int i = 0; i < 10000; i++) {
            set.contains(i * 10);
        }
        
        // Test iteration performance
        int count = 0;
        for (Integer id : set) {
            count++;
        }
        
        long elapsed = System.currentTimeMillis() - start;
        
        assertEquals(size, count);
        assertTrue(elapsed < 1000, "Should complete quickly: " + elapsed + "ms");
    }
    
    @Test
    @DisplayName("Filter returns same instance when no change")
    void testFilterSameInstance() {
        WordIdSet set = WordIdSet.of(1, 2, 3);
        
        // Filter that keeps everything
        WordIdSet filtered = set.filter(id -> true);
        
        assertSame(set, filtered, "Should return same instance when no change");
    }
}

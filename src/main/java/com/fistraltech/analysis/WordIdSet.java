package com.fistraltech.analysis;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Memory-efficient set of words represented by their integer IDs.
 * 
 * <p>This class replaces {@code Set<String>} for tracking filtered word sets,
 * avoiding the memory overhead of String references and HashSet nodes.
 * 
 * <p><strong>Memory comparison (for 2,315 words):</strong>
 * <ul>
 *   <li>HashSet&lt;String&gt;: ~2,315 × 48 bytes ≈ 111 KB per set</li>
 *   <li>WordIdSet (int[]): 2,315 × 4 bytes ≈ 9.3 KB per set</li>
 *   <li>WordIdSet (BitSet): 2,315 bits ≈ 290 bytes per set</li>
 * </ul>
 * 
 * <p>This implementation uses a compact int[] array that stores only the IDs
 * of words currently in the set. The array is kept sorted for efficient operations.
 * 
 * <p><strong>Usage:</strong>
 * <pre>{@code
 * ResponseMatrix matrix = ...;
 * 
 * // Create a set with all words
 * WordIdSet allWords = WordIdSet.all(matrix.getWordCount());
 * 
 * // Filter based on response pattern
 * short targetPattern = matrix.getPattern(guessId, targetId);
 * WordIdSet filtered = allWords.filter(id -> matrix.getPattern(guessId, id) == targetPattern);
 * 
 * // Iterate over remaining words
 * for (int wordId : filtered) {
 *     String word = matrix.getWord(wordId);
 * }
 * }</pre>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. Each game
 * session should maintain its own WordIdSet instances.
 * 
 * @see ResponseMatrix
 */
public final class WordIdSet implements Iterable<Integer> {
    
    /** Array of word IDs in this set, sorted in ascending order */
    private final int[] ids;
    
    /** Number of valid entries in the ids array */
    private final int size;
    
    /**
     * Creates a WordIdSet from an array of IDs.
     * The array is assumed to be sorted and contain no duplicates.
     * 
     * @param ids the array of word IDs (will be used directly, not copied)
     * @param size the number of valid entries in the array
     */
    private WordIdSet(int[] ids, int size) {
        this.ids = ids;
        this.size = size;
    }
    
    /**
     * Creates a WordIdSet containing all word IDs from 0 to count-1.
     * 
     * @param count the number of words (typically from ResponseMatrix.getWordCount())
     * @return a new WordIdSet containing all IDs
     */
    public static WordIdSet all(int count) {
        int[] ids = new int[count];
        for (int i = 0; i < count; i++) {
            ids[i] = i;
        }
        return new WordIdSet(ids, count);
    }
    
    /**
     * Creates an empty WordIdSet.
     * 
     * @return an empty WordIdSet
     */
    public static WordIdSet empty() {
        return new WordIdSet(new int[0], 0);
    }
    
    /**
     * Creates a WordIdSet from a Set of words using a ResponseMatrix for ID lookup.
     * 
     * @param words the set of word strings
     * @param matrix the response matrix for word-to-ID conversion
     * @return a new WordIdSet containing the IDs of all words found in the matrix
     */
    public static WordIdSet fromStrings(Set<String> words, ResponseMatrix matrix) {
        int[] tempIds = new int[words.size()];
        int count = 0;
        
        for (String word : words) {
            int id = matrix.getWordId(word);
            if (id >= 0) {
                tempIds[count++] = id;
            }
        }
        
        // Sort the IDs
        if (count > 0) {
            Arrays.sort(tempIds, 0, count);
        }
        
        // Create properly sized array
        int[] ids = count == tempIds.length ? tempIds : Arrays.copyOf(tempIds, count);
        return new WordIdSet(ids, count);
    }
    
    /**
     * Creates a WordIdSet from an existing int array.
     * The array will be copied and sorted.
     * 
     * @param wordIds array of word IDs
     * @return a new WordIdSet
     */
    public static WordIdSet of(int... wordIds) {
        if (wordIds == null || wordIds.length == 0) {
            return empty();
        }
        int[] ids = Arrays.copyOf(wordIds, wordIds.length);
        Arrays.sort(ids);
        return new WordIdSet(ids, ids.length);
    }
    
    /**
     * Returns the number of words in this set.
     * 
     * @return the size of this set
     */
    public int size() {
        return size;
    }
    
    /**
     * Returns true if this set is empty.
     * 
     * @return true if size is 0
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns the word ID at the given index.
     * 
     * @param index the index (0 to size-1)
     * @return the word ID at that index
     * @throws ArrayIndexOutOfBoundsException if index is out of bounds
     */
    public int get(int index) {
        if (index < 0 || index >= size) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return ids[index];
    }
    
    /**
     * Returns the underlying array of IDs.
     * <p><strong>Warning:</strong> This returns the internal array directly for performance.
     * Do not modify the returned array.
     * 
     * @return the internal ID array
     */
    public int[] getIds() {
        return ids;
    }
    
    /**
     * Checks if this set contains the given word ID.
     * Uses binary search for O(log n) lookup.
     * 
     * @param wordId the word ID to check
     * @return true if the ID is in this set
     */
    public boolean contains(int wordId) {
        return Arrays.binarySearch(ids, 0, size, wordId) >= 0;
    }
    
    /**
     * Creates a new WordIdSet containing only IDs that match the given predicate.
     * 
     * @param predicate the filter condition
     * @return a new filtered WordIdSet
     */
    public WordIdSet filter(java.util.function.IntPredicate predicate) {
        int[] filtered = new int[size];
        int count = 0;
        
        for (int i = 0; i < size; i++) {
            if (predicate.test(ids[i])) {
                filtered[count++] = ids[i];
            }
        }
        
        if (count == size) {
            return this; // No change
        }
        
        return new WordIdSet(Arrays.copyOf(filtered, count), count);
    }
    
    /**
     * Filters this set to only include words that produce the given pattern
     * when guessed against, using the ResponseMatrix.
     * 
     * @param matrix the response matrix
     * @param guessId the guess word ID
     * @param pattern the required response pattern
     * @return a new WordIdSet containing only matching words
     */
    public WordIdSet filterByPattern(ResponseMatrix matrix, int guessId, short pattern) {
        int[] filtered = new int[size];
        int count = 0;
        
        for (int i = 0; i < size; i++) {
            int targetId = ids[i];
            if (matrix.getPattern(guessId, targetId) == pattern) {
                filtered[count++] = targetId;
            }
        }
        
        if (count == size) {
            return this;
        }
        
        return new WordIdSet(Arrays.copyOf(filtered, count), count);
    }
    
    /**
     * Computes bucket counts for a guess word against this set of targets.
     * 
     * @param matrix the response matrix
     * @param guessId the guess word ID
     * @param maxPatterns maximum number of patterns (typically 4^wordLength)
     * @return array where index is the pattern and value is the count
     */
    public int[] getBucketCounts(ResponseMatrix matrix, int guessId, int maxPatterns) {
        int[] counts = new int[maxPatterns];
        
        for (int i = 0; i < size; i++) {
            short pattern = matrix.getPattern(guessId, ids[i]);
            counts[pattern]++;
        }
        
        return counts;
    }
    
    /**
     * Computes bucket counts using a pre-allocated array (zero-allocation path).
     * 
     * @param matrix the response matrix
     * @param guessId the guess word ID
     * @param counts pre-allocated counts array (will be filled with zeros first)
     */
    public void getBucketCounts(ResponseMatrix matrix, int guessId, int[] counts) {
        Arrays.fill(counts, 0);
        
        for (int i = 0; i < size; i++) {
            short pattern = matrix.getPattern(guessId, ids[i]);
            counts[pattern]++;
        }
    }
    
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            private int index = 0;
            
            @Override
            public boolean hasNext() {
                return index < size;
            }
            
            @Override
            public Integer next() {
                if (index >= size) {
                    throw new NoSuchElementException();
                }
                return ids[index++];
            }
        };
    }
    
    /**
     * Provides a primitive int iterator to avoid boxing overhead.
     * 
     * @return a primitive iterator
     */
    public PrimitiveIterator primitiveIterator() {
        return new PrimitiveIterator();
    }
    
    /**
     * Primitive int iterator to avoid autoboxing in performance-critical loops.
     */
    public class PrimitiveIterator {
        private int index = 0;
        
        public boolean hasNext() {
            return index < size;
        }
        
        public int nextInt() {
            if (index >= size) {
                throw new NoSuchElementException();
            }
            return ids[index++];
        }
        
        public void reset() {
            index = 0;
        }
    }
    
    @Override
    public String toString() {
        if (size == 0) {
            return "WordIdSet[]";
        }
        StringBuilder sb = new StringBuilder("WordIdSet[");
        int displayCount = Math.min(size, 10);
        for (int i = 0; i < displayCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(ids[i]);
        }
        if (size > 10) {
            sb.append(", ... (").append(size).append(" total)");
        }
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WordIdSet)) return false;
        WordIdSet other = (WordIdSet) obj;
        if (size != other.size) return false;
        for (int i = 0; i < size; i++) {
            if (ids[i] != other.ids[i]) return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        int result = size;
        for (int i = 0; i < Math.min(size, 10); i++) {
            result = 31 * result + ids[i];
        }
        return result;
    }
}

package com.fistraltech.analysis;

import java.util.Objects;

/**
 * Memory-efficient composite key for caching response patterns between word pairs.
 * 
 * <p>This class replaces concatenated String keys (e.g., "SLATE:AROSE") with a lightweight
 * object holding direct references to the interned word strings. This dramatically reduces
 * memory usage in the response cache:
 * <ul>
 *   <li>Concatenated String key: ~56 bytes (object header + char array + metadata)</li>
 *   <li>WordPairKey: ~28 bytes (object header + 2 references)</li>
 * </ul>
 * 
 * <p><strong>Usage requirements:</strong>
 * <ul>
 *   <li>Words should be interned or reuse the same String instances from the Dictionary</li>
 *   <li>Both words must be non-null</li>
 * </ul>
 * 
 * <p>Thread-safety: This class is immutable and safe for concurrent use as a map key.
 * 
 * @see WordEntropy
 * @see ResponsePattern
 */
public final class WordPairKey {
    
    private final String guessWord;
    private final String targetWord;
    private final int hashCode;
    
    /**
     * Creates a new word pair key.
     * 
     * <p>For optimal memory efficiency, both parameters should be interned strings
     * or references to strings already stored in the Dictionary's word set.
     * 
     * @param guessWord the guess word (must not be null)
     * @param targetWord the target word (must not be null)
     * @throws NullPointerException if either word is null
     */
    public WordPairKey(String guessWord, String targetWord) {
        this.guessWord = Objects.requireNonNull(guessWord, "guessWord must not be null");
        this.targetWord = Objects.requireNonNull(targetWord, "targetWord must not be null");
        // Pre-compute hash code for efficiency (immutable object)
        this.hashCode = computeHashCode();
    }
    
    /**
     * Returns the guess word.
     * @return the guess word
     */
    public String getGuessWord() {
        return guessWord;
    }
    
    /**
     * Returns the target word.
     * @return the target word
     */
    public String getTargetWord() {
        return targetWord;
    }
    
    private int computeHashCode() {
        // Use a simple, fast hash combination
        // Multiply first hash by 31 (prime) and add second - standard Java pattern
        return 31 * guessWord.hashCode() + targetWord.hashCode();
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WordPairKey)) return false;
        WordPairKey other = (WordPairKey) obj;
        // Use reference equality first for interned strings (fast path)
        // Fall back to equals() for safety
        return (guessWord == other.guessWord || guessWord.equals(other.guessWord))
            && (targetWord == other.targetWord || targetWord.equals(other.targetWord));
    }
    
    @Override
    public String toString() {
        return guessWord + ":" + targetWord;
    }
}

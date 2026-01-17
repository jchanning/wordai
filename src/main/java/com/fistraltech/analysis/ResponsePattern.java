package com.fistraltech.analysis;

/**
 * Memory-efficient encoding of response patterns for Wordle-like games.
 * 
 * <p>Instead of storing response patterns as strings (e.g., "GARXR" consuming ~48 bytes),
 * this class encodes them as a single short integer (2 bytes), achieving ~96% memory
 * savings in the response cache.
 * 
 * <p><strong>Encoding scheme:</strong>
 * <ul>
 *   <li>Each position uses 2 bits to encode one of 4 possible states</li>
 *   <li>G (Green/correct) = 0b00 = 0</li>
 *   <li>A (Amber/present) = 0b01 = 1</li>
 *   <li>R (Red/absent) = 0b10 = 2</li>
 *   <li>X (Excess) = 0b11 = 3</li>
 *   <li>Position 0 uses bits 0-1, position 1 uses bits 2-3, etc.</li>
 * </ul>
 * 
 * <p><strong>Capacity:</strong>
 * <ul>
 *   <li>short (16 bits) supports up to 8 positions</li>
 *   <li>Suitable for word lengths 4-8 (typical Wordle variants)</li>
 * </ul>
 * 
 * <p><strong>Example:</strong>
 * <pre>
 * Response "GARXR" encodes as:
 *   Position 0: G=0 → bits 0-1 = 00
 *   Position 1: A=1 → bits 2-3 = 01
 *   Position 2: R=2 → bits 4-5 = 10
 *   Position 3: X=3 → bits 6-7 = 11
 *   Position 4: R=2 → bits 8-9 = 10
 *   Binary: 10_11_10_01_00 = 0b1011100100 = 740
 * </pre>
 * 
 * <p>Thread-safety: This class is immutable and safe for concurrent use.
 * 
 * @see WordEntropy
 * @see WordPairKey
 */
public final class ResponsePattern {
    
    // Status code mappings (2 bits each)
    private static final int GREEN = 0;   // 0b00 - Correct position
    private static final int AMBER = 1;   // 0b01 - Wrong position, letter present
    private static final int RED = 2;     // 0b10 - Letter absent
    private static final int EXCESS = 3;  // 0b11 - Excess occurrence
    
    private static final int BITS_PER_POSITION = 2;
    private static final int POSITION_MASK = 0b11;
    
    // Lookup table for fast char-to-code conversion
    private static final int[] CHAR_TO_CODE = new int[128];
    private static final char[] CODE_TO_CHAR = {'G', 'A', 'R', 'X'};
    
    static {
        // Initialize lookup table
        CHAR_TO_CODE['G'] = GREEN;
        CHAR_TO_CODE['A'] = AMBER;
        CHAR_TO_CODE['R'] = RED;
        CHAR_TO_CODE['X'] = EXCESS;
    }
    
    private final short encoded;
    private final byte wordLength;
    
    /**
     * Creates a ResponsePattern from an encoded short value.
     * 
     * @param encoded the encoded pattern value
     * @param wordLength the word length (for decoding)
     */
    private ResponsePattern(short encoded, byte wordLength) {
        this.encoded = encoded;
        this.wordLength = wordLength;
    }
    
    /**
     * Encodes a response pattern string into a ResponsePattern.
     * 
     * @param pattern the pattern string (e.g., "GARXR")
     * @return the encoded ResponsePattern
     * @throws IllegalArgumentException if pattern is null, empty, or contains invalid characters
     */
    public static ResponsePattern encode(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Pattern must not be null or empty");
        }
        if (pattern.length() > 8) {
            throw new IllegalArgumentException("Pattern length exceeds maximum of 8: " + pattern.length());
        }
        
        short encoded = 0;
        int length = pattern.length();
        
        for (int i = 0; i < length; i++) {
            char c = pattern.charAt(i);
            int code = CHAR_TO_CODE[c];
            encoded |= (short) (code << (i * BITS_PER_POSITION));
        }
        
        return new ResponsePattern(encoded, (byte) length);
    }
    
    /**
     * Encodes a response pattern directly from status characters.
     * 
     * @param statuses array of status characters ('G', 'A', 'R', 'X')
     * @return the encoded ResponsePattern
     */
    public static ResponsePattern encode(char[] statuses) {
        if (statuses == null || statuses.length == 0) {
            throw new IllegalArgumentException("Statuses must not be null or empty");
        }
        if (statuses.length > 8) {
            throw new IllegalArgumentException("Status length exceeds maximum of 8: " + statuses.length);
        }
        
        short encoded = 0;
        
        for (int i = 0; i < statuses.length; i++) {
            int code = CHAR_TO_CODE[statuses[i]];
            encoded |= (short) (code << (i * BITS_PER_POSITION));
        }
        
        return new ResponsePattern(encoded, (byte) statuses.length);
    }
    
    /**
     * Returns the raw encoded value.
     * This is suitable for use as a primitive map key or for storage.
     * 
     * @return the encoded short value
     */
    public short getEncoded() {
        return encoded;
    }
    
    /**
     * Returns the word length this pattern was created for.
     * 
     * @return the word length
     */
    public int getWordLength() {
        return wordLength;
    }
    
    /**
     * Gets the status code at a specific position.
     * 
     * @param position the position (0-based)
     * @return the status character ('G', 'A', 'R', or 'X')
     */
    public char getStatusAt(int position) {
        int code = (encoded >> (position * BITS_PER_POSITION)) & POSITION_MASK;
        return CODE_TO_CHAR[code];
    }
    
    /**
     * Decodes the pattern back to a string representation.
     * 
     * @return the pattern string (e.g., "GARXR")
     */
    public String decode() {
        char[] chars = new char[wordLength];
        for (int i = 0; i < wordLength; i++) {
            chars[i] = getStatusAt(i);
        }
        return new String(chars);
    }
    
    /**
     * Checks if this pattern represents a winning response (all greens).
     * 
     * @return true if all positions are GREEN
     */
    public boolean isWinner() {
        // All greens means all bits are 0
        return encoded == 0;
    }
    
    @Override
    public int hashCode() {
        return encoded;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ResponsePattern)) return false;
        ResponsePattern other = (ResponsePattern) obj;
        return encoded == other.encoded && wordLength == other.wordLength;
    }
    
    @Override
    public String toString() {
        return decode();
    }
    
    /**
     * Creates a ResponsePattern from a raw encoded value.
     * Used when retrieving from cache.
     * 
     * @param encoded the encoded short value
     * @param wordLength the word length
     * @return the ResponsePattern
     */
    public static ResponsePattern fromEncoded(short encoded, byte wordLength) {
        return new ResponsePattern(encoded, wordLength);
    }
}

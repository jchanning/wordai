package com.fistraltech.analysis;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

/**
 * Tests for the memory-efficient response caching structures.
 */
public class ResponseCacheTest {

    @Test
    void testWordPairKeyEquality() {
        // Use interned strings for accurate testing
        String word1 = "SLATE".intern();
        String word2 = "AROSE".intern();
        
        WordPairKey key1 = new WordPairKey(word1, word2);
        WordPairKey key2 = new WordPairKey(word1, word2);
        
        assertEquals(key1, key2, "Same word pairs should be equal");
        assertEquals(key1.hashCode(), key2.hashCode(), "Same word pairs should have same hashCode");
    }
    
    @Test
    void testWordPairKeyInequality() {
        String word1 = "SLATE".intern();
        String word2 = "AROSE".intern();
        String word3 = "CRANE".intern();
        
        WordPairKey key1 = new WordPairKey(word1, word2);
        WordPairKey key2 = new WordPairKey(word1, word3);
        WordPairKey key3 = new WordPairKey(word2, word1); // Order matters
        
        assertNotEquals(key1, key2, "Different target words should not be equal");
        assertNotEquals(key1, key3, "Swapped words should not be equal");
    }
    
    @Test
    void testWordPairKeyWithInternedStrings() {
        // Verify that WordPairKey works correctly with interned strings
        String a1 = new String("HELLO").intern();
        String a2 = new String("HELLO").intern();
        String b = "WORLD".intern();
        
        assertSame(a1, a2, "Interned strings should be same reference");
        
        WordPairKey key1 = new WordPairKey(a1, b);
        WordPairKey key2 = new WordPairKey(a2, b);
        
        assertEquals(key1, key2, "Keys with interned strings should be equal");
    }
    
    @Test
    void testResponsePatternEncode() {
        // Test encoding simple patterns
        ResponsePattern pattern = ResponsePattern.encode("GGGGG");
        assertEquals("GGGGG", pattern.decode(), "All greens should encode/decode correctly");
        assertTrue(pattern.isWinner(), "All greens should be winner");
        
        pattern = ResponsePattern.encode("RRRRR");
        assertEquals("RRRRR", pattern.decode(), "All reds should encode/decode correctly");
        assertFalse(pattern.isWinner(), "All reds should not be winner");
        
        pattern = ResponsePattern.encode("GARXR");
        assertEquals("GARXR", pattern.decode(), "Mixed pattern should encode/decode correctly");
        assertFalse(pattern.isWinner(), "Mixed pattern should not be winner");
    }
    
    @Test
    void testResponsePatternStatusAt() {
        ResponsePattern pattern = ResponsePattern.encode("GARXR");
        
        assertEquals('G', pattern.getStatusAt(0));
        assertEquals('A', pattern.getStatusAt(1));
        assertEquals('R', pattern.getStatusAt(2));
        assertEquals('X', pattern.getStatusAt(3));
        assertEquals('R', pattern.getStatusAt(4));
    }
    
    @Test
    void testResponsePatternDifferentLengths() {
        // 4-letter word
        ResponsePattern p4 = ResponsePattern.encode("GARA");
        assertEquals(4, p4.getWordLength());
        assertEquals("GARA", p4.decode());
        
        // 6-letter word
        ResponsePattern p6 = ResponsePattern.encode("GARAXR");
        assertEquals(6, p6.getWordLength());
        assertEquals("GARAXR", p6.decode());
        
        // 7-letter word  
        ResponsePattern p7 = ResponsePattern.encode("GARAXRG");
        assertEquals(7, p7.getWordLength());
        assertEquals("GARAXRG", p7.decode());
    }
    
    @Test
    void testResponsePatternEquality() {
        ResponsePattern p1 = ResponsePattern.encode("GARXR");
        ResponsePattern p2 = ResponsePattern.encode("GARXR");
        ResponsePattern p3 = ResponsePattern.encode("GGGGG");
        
        assertEquals(p1, p2, "Same patterns should be equal");
        assertEquals(p1.hashCode(), p2.hashCode(), "Same patterns should have same hashCode");
        assertNotEquals(p1, p3, "Different patterns should not be equal");
    }
    
    @Test
    void testResponsePatternShortEncoding() {
        // Verify that the encoding uses short values efficiently
        ResponsePattern pattern = ResponsePattern.encode("GGGGG");
        assertEquals(0, pattern.getEncoded(), "All greens should encode to 0");
        
        // G=0, A=1, R=2, X=3
        // AAAAA = 01_01_01_01_01 binary = 341
        pattern = ResponsePattern.encode("AAAAA");
        assertEquals(341, pattern.getEncoded());
    }
    
    @Test
    void testWordPairKeyNullHandling() {
        assertThrows(NullPointerException.class, () -> new WordPairKey(null, "AROSE"));
        assertThrows(NullPointerException.class, () -> new WordPairKey("SLATE", null));
    }
    
    @Test
    void testResponsePatternInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode((String) null));
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode(""));
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode("GGGGGGGGG")); // 9 chars
    }
}

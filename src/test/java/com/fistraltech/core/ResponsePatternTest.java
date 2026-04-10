package com.fistraltech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResponsePattern} encode/decode invariants.
 */
@DisplayName("ResponsePattern Tests")
public class ResponsePatternTest {

    @Test
    @DisplayName("All-green pattern encodes and decodes correctly")
    void encode_allGreen_roundTrips() {
        ResponsePattern pattern = ResponsePattern.encode("GGGGG");
        assertEquals("GGGGG", pattern.decode());
        assertTrue(pattern.isWinner());
    }

    @Test
    @DisplayName("All-red pattern encodes and decodes correctly")
    void encode_allRed_roundTrips() {
        ResponsePattern pattern = ResponsePattern.encode("RRRRR");
        assertEquals("RRRRR", pattern.decode());
        assertFalse(pattern.isWinner());
    }

    @Test
    @DisplayName("Mixed pattern with all four status codes encodes and decodes correctly")
    void encode_mixedPattern_roundTrips() {
        ResponsePattern pattern = ResponsePattern.encode("GARXR");
        assertEquals("GARXR", pattern.decode());
        assertFalse(pattern.isWinner());
    }

    @Test
    @DisplayName("getStatusAt returns correct character at each position")
    void getStatusAt_mixedPattern_returnsCorrectChars() {
        ResponsePattern pattern = ResponsePattern.encode("GARXR");
        assertEquals('G', pattern.getStatusAt(0));
        assertEquals('A', pattern.getStatusAt(1));
        assertEquals('R', pattern.getStatusAt(2));
        assertEquals('X', pattern.getStatusAt(3));
        assertEquals('R', pattern.getStatusAt(4));
    }

    @Test
    @DisplayName("Patterns of length 4, 6, and 7 encode correctly")
    void encode_differentLengths_preservesLength() {
        ResponsePattern p4 = ResponsePattern.encode("GARA");
        assertEquals(4, p4.getWordLength());
        assertEquals("GARA", p4.decode());

        ResponsePattern p6 = ResponsePattern.encode("GARAXR");
        assertEquals(6, p6.getWordLength());
        assertEquals("GARAXR", p6.decode());

        ResponsePattern p7 = ResponsePattern.encode("GARAXRG");
        assertEquals(7, p7.getWordLength());
        assertEquals("GARAXRG", p7.decode());
    }

    @Test
    @DisplayName("Equal patterns are equal and have the same hashCode")
    void equality_samePattern_equalsAndHashCode() {
        ResponsePattern p1 = ResponsePattern.encode("GARXR");
        ResponsePattern p2 = ResponsePattern.encode("GARXR");
        ResponsePattern p3 = ResponsePattern.encode("GGGGG");

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }

    @Test
    @DisplayName("All-green encodes to 0; all-amber encodes to 341")
    void getEncoded_knownValues_matchExpected() {
        // G=0b00 per position → all zeros
        ResponsePattern allGreen = ResponsePattern.encode("GGGGG");
        assertEquals(0, allGreen.getEncoded());

        // A=0b01 per position: 01_01_01_01_01 = 341
        ResponsePattern allAmber = ResponsePattern.encode("AAAAA");
        assertEquals(341, allAmber.getEncoded());
    }

    @Test
    @DisplayName("Encoding with status char array matches string encoding")
    void encode_charArray_matchesStringEncoding() {
        ResponsePattern fromString = ResponsePattern.encode("GARXR");
        ResponsePattern fromChars = ResponsePattern.encode(new char[]{'G', 'A', 'R', 'X', 'R'});
        assertEquals(fromString, fromChars);
    }

    @Test
    @DisplayName("Null or empty input throws IllegalArgumentException")
    void encode_invalidInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode((String) null));
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode(""));
        assertThrows(IllegalArgumentException.class, () -> ResponsePattern.encode("GGGGGGGGG")); // 9 chars
    }
}

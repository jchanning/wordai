package com.fistraltech.analysis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EntropyKey Tests")
class EntropyKeyTest {

    @Test
    @DisplayName("constructor_rejectsNullGuessedWord")
    void constructor_rejectsNullGuessedWord() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new EntropyKey(null, "GARXR"));

        assertEquals("guessedWord must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("constructor_rejectsNullResponse")
    void constructor_rejectsNullResponse() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> new EntropyKey("SLATE", null));

        assertEquals("response must not be null", exception.getMessage());
    }

    @Test
    @DisplayName("equalsAndHashCode_useImmutableCompositeState")
    void equalsAndHashCode_useImmutableCompositeState() {
        EntropyKey first = new EntropyKey("SLATE", "GARXR");
        EntropyKey same = new EntropyKey("SLATE", "GARXR");
        EntropyKey differentGuess = new EntropyKey("CRANE", "GARXR");
        EntropyKey differentResponse = new EntropyKey("SLATE", "GGGGG");

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, differentGuess);
        assertNotEquals(first, differentResponse);
    }

    @Test
    @DisplayName("compareTo_ordersByCompositeKey")
    void compareTo_ordersByCompositeKey() {
        EntropyKey laterResponse = new EntropyKey("ARISE", "RRRRR");
        EntropyKey earlierGuess = new EntropyKey("AROSE", "GGGGG");
        EntropyKey sameAsEarlier = new EntropyKey("AROSE", "GGGGG");

        List<EntropyKey> ordered = List.of(laterResponse, earlierGuess).stream().sorted().toList();

        assertEquals(List.of(laterResponse, earlierGuess), ordered);
        assertEquals(0, earlierGuess.compareTo(sameAsEarlier));
    }
}
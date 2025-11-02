package com.fistraltech.game;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.ResponseEntry;

/**
 * Unit tests for the GameController.play() method.
 * Tests various scenarios including correct guesses, partial matches, and invalid inputs.
 */
class GameControllerTest {
    
    private GameController controller;
    private Set<String> validWords;
    private Set<String> gameWords;
    
    @BeforeEach
    void setUp() {
        controller = new GameController();
        
        // Create a comprehensive set of 5-letter words for testing
        validWords = new HashSet<>();
        validWords.add("AROSE");
        validWords.add("RAISE");
        validWords.add("STARE");
        validWords.add("SLATE");
        validWords.add("CRANE");
        validWords.add("ADIEU");
        validWords.add("AUDIO");
        validWords.add("HOUSE");
        validWords.add("BEACH");
        validWords.add("BEECH");
        validWords.add("BETEL");
        validWords.add("BEGIN");
        validWords.add("PANSY");
        validWords.add("SALSA");
        validWords.add("JERKY");
        validWords.add("TESTS");
        validWords.add("HELLO");
        validWords.add("WORLD");
        
        gameWords = new HashSet<>(validWords);
        
        // Initialize the controller with dictionaries
        // IMPORTANT: Call loadGameWords FIRST to set the word length, then loadAllValidWords
        controller.loadGameWords(5, gameWords);
        controller.loadAllValidWords(validWords);
    }
    
    @Test
    @DisplayName("Test exact match - all letters correct")
    void testExactMatch() throws InvalidWordException {
        controller.setTargetWord("AROSE");
        Response response = controller.play("AROSE");
        
        assertEquals(5, response.getStatuses().size());
        for (ResponseEntry entry : response.getStatuses()) {
            assertEquals('G', entry.status, 
                "All letters should be Green (correct) for exact match");
        }
    }
    
    @Test
    @DisplayName("Test no matching letters")
    void testNoMatchingLetters() throws InvalidWordException {
        // Add UNTIL to dictionary first
        validWords.add("UNTIL");
        controller.loadAllValidWords(validWords);
        
        controller.setTargetWord("AROSE");
        Response response = controller.play("UNTIL");
        
        // UNTIL vs AROSE - U, N, T, I, L are all not in AROSE
        for (ResponseEntry entry : response.getStatuses()) {
            assertEquals('R', entry.status,
                "All letters should be Red (absent) when no matches");
        }
    }
    
    @Test
    @DisplayName("Test invalid word throws exception")
    void testInvalidWord() throws InvalidWordException {
        controller.setTargetWord("AROSE");
        
        InvalidWordException exception = assertThrows(InvalidWordException.class, () -> {
            controller.play("ZZZZZ");
        }, "Should throw InvalidWordException for word not in dictionary");
        
        assertNotNull(exception);
    }
    
    @Test
    @DisplayName("Test wrong word length throws exception")
    void testWrongWordLength() throws InvalidWordException {
        controller.setTargetWord("AROSE");
        
        InvalidWordException exception = assertThrows(InvalidWordException.class, () -> {
            controller.play("CAT");
        }, "Should throw InvalidWordException for wrong word length");
        
        assertNotNull(exception);
    }
    
    /**
     * Parameterized test for specific test cases.
     * Format: targetWord, guessWord, expectedResponse
     * Response format: G=Green (correct position), A=Amber (wrong position), R=Red (absent), X=Excess (duplicate beyond available count)
     */
    @ParameterizedTest
    @DisplayName("Test specific word combinations")
    @CsvSource({
        // Target,  Guess,   Expected (position 0-4: G=Green, A=Amber, R=Red, X=Excess)
        "AROSE,    RAISE,   AARGG",  // R in word wrong pos (A), A in word wrong pos (A), I absent (R), S correct (G), E correct (G)
        "AROSE,    STARE,   ARAAG",  // S in word wrong pos (A), T absent (R), A correct (G), R in word wrong pos (A), E correct (G)
        "AROSE,    AROSE,   GGGGG",  // Perfect match
        "HOUSE,    AROSE,   RRAGG",  // A absent (R), R absent (R), O in word wrong pos (A), S correct (G), E correct (G)
        "BEACH,    BEECH,   GGXGG",  // B correct (G), E correct (G), second E excess (X), C correct (G), H correct (G)
        "BEECH,    BEACH,   GGRGG",  // B correct (G), E correct (G), A not in word (R), C correct (G), H correct (G)
    })
    void testSpecificCases(String targetWord, String guessWord, String expectedResponse) 
            throws InvalidWordException {
        
        controller.setTargetWord(targetWord);
        Response response = controller.play(guessWord);
        
        assertEquals(expectedResponse.length(), response.getStatuses().size(),
            "Response length should match word length");
        
        // Verify each position
        for (int i = 0; i < expectedResponse.length(); i++) {
            char expected = expectedResponse.charAt(i);
            char actual = response.getStatuses().get(i).status;
            
            assertEquals(expected, actual,
                String.format("Position %d: Expected '%c' for letter '%c' in guess '%s' vs target '%s'",
                    i, expected, guessWord.charAt(i), guessWord, targetWord));
        }
    }
    
    /**
     * Test case for duplicate letters in guess word
     */
    @Test
    @DisplayName("Test duplicate letters - BETEL vs BEECH")
    void testDuplicateLetters_BetelVsBeech() throws InvalidWordException {
        controller.setTargetWord("BEECH");
        Response response = controller.play("BETEL");
        
        // B - correct position (Green)
        assertEquals('G', response.getStatuses().get(0).status, 
            "B should be Green (correct position)");
        
        // First E - correct position (Green)
        assertEquals('G', response.getStatuses().get(1).status,
            "First E should be Green (correct position)");
        
        // T - not in word (Red)
        assertEquals('R', response.getStatuses().get(2).status,
            "T should be Red (not in word)");
        
        // Second E - wrong position (Amber)
        assertEquals('A', response.getStatuses().get(3).status,
            "Second E should be Amber (wrong position)");
        
        // L - not in word (Red)
        assertEquals('R', response.getStatuses().get(4).status,
            "L should be Red (not in word)");
    }
    
    /**
     * Test case for when guess has more occurrences of a letter than target
     */
    @Test
    @DisplayName("Test excess letter occurrences - SALSA vs PANSY")
    void testExcessLetterOccurrences() throws InvalidWordException {
        controller.setTargetWord("PANSY");
        Response response = controller.play("SALSA");
        
        // S - wrong position (Excess) - PANSY has S at position 3, but position 3 in guess also has S
        assertEquals('X', response.getStatuses().get(0).status,
            "First S should be Excess - only one S in PANSY and it's matched at position 3");
        
        // A - correct position (Green) - PANSY has A at position 1
        assertEquals('G', response.getStatuses().get(1).status,
            "First A should be Green - correct position");
        
        // L - not in word (Red)
        assertEquals('R', response.getStatuses().get(2).status,
            "L should be Red");
        
        // Second S - correct position (Green)
        assertEquals('G', response.getStatuses().get(3).status,
            "Second S should be Green");
        
        // Second A - excess as only one A in PANSY (Excess)
        assertEquals('X', response.getStatuses().get(4).status,
            "Second A should be Excess");
    }
    
    /**
     * Test helper method to verify response matches expected pattern
     */
    @SuppressWarnings("unused")
    private void assertResponseMatches(Response response, String expectedPattern, String message) {
        assertEquals(expectedPattern.length(), response.getStatuses().size(),
            message + " - length mismatch");
        
        for (int i = 0; i < expectedPattern.length(); i++) {
            assertEquals(expectedPattern.charAt(i), 
                response.getStatuses().get(i).status,
                message + " - position " + i);
        }
    }
    
    /**
     * Template test method for adding custom test cases
     * Usage: Uncomment and fill in your specific test scenario
     */
    // @Test
    // @DisplayName("Custom test case: [Describe your scenario]")
    // void testCustomCase() throws InvalidWordException {
    //     controller.setTargetWord("YOUR_TARGET");
    //     Response response = controller.play("YOUR_GUESS");
    //     
    //     assertResponseMatches(response, "EXPECTED", "Custom test description");
    // }
}

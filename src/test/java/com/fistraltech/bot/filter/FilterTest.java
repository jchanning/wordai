package com.fistraltech.bot.filter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.util.Config;

import java.util.Set;
import java.util.HashSet;

/**
 * Comprehensive tests for the Filter class, focusing on edge cases involving
 * duplicate letters that previously caused filtering bugs.
 * 
 * Key bug scenarios tested:
 * 1. JERKY scenario - Duplicate letters in target word counted multiple times in mustContain
 * 2. PANSY/SALSA scenario - Excess 'X' status incorrectly removing letters  
 * 3. MOURN/FUROR scenario - Duplicate letters in guess with mixed G/A/X statuses
 * 4. STUDY/MUCUS scenario - Multiple duplicate letters with excess instances
 */
class FilterTest {

    @Test
    @DisplayName("Bug Fix #1: JERKY scenario - mustContain should use Set to prevent duplicate letter requirements")
    void testJerkyScenario_MustContainUsesSet() throws InvalidWordException {
        // Bug: When target has duplicate letters (JERKY has 2 E's), and guess has that letter,
        // the old List-based mustContain would add 'e' multiple times, requiring words with
        // multiple E's even though the guess only had one E.
        
        // Create dictionary with target and test words
        Dictionary dict = createTestDictionary("jerky", "arose", "deter", "elder", "eerie");
        
        // Create game with JERKY as target
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("jerky");
        
        // Guess AROSE
        Response response = game.guess("arose");
        
        // Apply filter
        Filter filter = new Filter(5);
        filter.update(response);
        Dictionary filtered = filter.apply(dict);
        
        // JERKY must remain (it's the target!)
        assertTrue(filtered.contains("jerky"), 
            "JERKY (target) must remain after AROSE guess");
        
        // Words with the required letters should also remain (unless position constraints exclude them)
        assertTrue(filtered.contains("elder"), "ELDER should remain (has R and E in valid positions)");
        
        // EERIE should be filtered out because it has E at position 4, which was Amber in the guess
        assertFalse(filtered.contains("eerie"), "EERIE should be filtered (has E at position 4 which was marked as wrong position)");
    }

    @Test
    @DisplayName("Bug Fix #2: PANSY/SALSA scenario - Excess 'X' status should not remove letters from positions")
    void testPansySalsaScenario_ExcessNotRemovingLetters() throws InvalidWordException {
        // Bug: When guess has MORE instances of a letter than target, excess instances get 'X' status.
        // The old code treated X like R (Red) and removed the letter from all positions.
        // SALSA has 2 A's and 2 S's, PANSY has 1 of each.
        
        Dictionary dict = createTestDictionary("pansy", "salsa", "patsy", "tansy", "raise");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("pansy");
        
        // First guess: RAISE
        Filter filter = new Filter(5);
        Response response1 = game.guess("raise");
        filter.update(response1);
        
        // Second guess: SALSA (has duplicate A and S)
        Response response2 = game.guess("salsa");
        filter.update(response2);
        
        Dictionary filtered = filter.apply(dict);
        
        // PANSY must remain after both guesses
        assertTrue(filtered.contains("pansy"), 
            "PANSY (target) must remain after SALSA guess with duplicate letters");
    }

    @Test
    @DisplayName("Bug Fix #3: MOURN/FUROR scenario - Mixed G/A/X statuses for duplicate guess letters")
    void testMournFurorScenario_MixedStatusesForDuplicates() throws InvalidWordException {
        // FUROR has: 2 R's (one gets A, one gets X), 1 O (gets X because O is at wrong position)
        // MOURN has: 1 R at pos 4, 1 O at pos 1, 1 U at pos 2
        
        Dictionary dict = createTestDictionary("mourn", "furor", "flour", "rumor");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("mourn");
        
        Filter filter = new Filter(5);
        Response response = game.guess("furor");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // MOURN must remain
        assertTrue(filtered.contains("mourn"), 
            "MOURN (target) must remain after FUROR guess");
    }

    @Test
    @DisplayName("Bug Fix #4: STUDY/MUCUS scenario - Multiple different letters with excess instances")
    void testStudyMucusScenario_MultipleExcessLetters() throws InvalidWordException {
        // MUCUS has 2 U's (one A, one X) and 1 S (gets A status)
        // STUDY has 1 U at pos 2, 1 S at pos 0
        
        Dictionary dict = createTestDictionary("study", "mucus", "focus", "truss");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("study");
        
        Filter filter = new Filter(5);
        Response response = game.guess("mucus");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // STUDY must remain
        assertTrue(filtered.contains("study"), 
            "STUDY (target) must remain after MUCUS guess");
    }

    @Test
    @DisplayName("Verify: Green status locks position correctly")
    void testGreenLocksPosition() throws InvalidWordException {
        Dictionary dict = createTestDictionary("arose", "erase", "prose");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("arose");
        
        Filter filter = new Filter(5);
        Response response = game.guess("prose");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // Only words with matching green positions should remain
        assertTrue(filtered.contains("arose"), "AROSE should remain (R at pos 1, O at pos 2, S at pos 3, E at pos 4)");
        assertFalse(filtered.contains("erase"), "ERASE should be filtered (R not at pos 1)");
    }

    @Test
    @DisplayName("Verify: Amber status adds to mustContain and removes from guessed position")
    void testAmberMustContain() throws InvalidWordException {
        Dictionary dict = createTestDictionary("mourn", "arose", "mayor", "roams");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("mourn");
        
        Filter filter = new Filter(5);
        Response response = game.guess("arose");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // Words must contain R and O (both Amber in guess)
        assertTrue(filtered.contains("mourn"), "MOURN should remain (has R and O)");
        
        // But not at the positions where they were guessed
        assertFalse(filtered.contains("arose"), "AROSE should be filtered (has R at guessed position)");
    }

    @Test
    @DisplayName("Verify: Red status removes letter from all positions (when no G/A instances exist)")
    void testRedRemovesLetter() throws InvalidWordException {
        Dictionary dict = createTestDictionary("mourn", "arose", "kayak", "knack");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("mourn");
        
        Filter filter = new Filter(5);
        Response response = game.guess("kayak");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // K, A, Y should all be Red (don't exist in MOURN)
        assertFalse(filtered.contains("kayak"), "KAYAK should be filtered (has K, A, Y which are all Red)");
        assertTrue(filtered.contains("mourn"), "MOURN should remain (no K, A, or Y)");
    }

    @Test
    @DisplayName("Bug Fix #5: BEECH/BETEL scenario - Must count minimum required letter occurrences")
    void testBeechBetelScenario_CountLetterOccurrences() throws InvalidWordException {
        // Bug: When BETEL is guessed against BEECH:
        // - B = Green (pos 0)
        // - E = Green (pos 1) 
        // - T = Red (not in word)
        // - E = Amber (second E exists but not at pos 3)
        // - L = Red (not in word)
        // This means the target has AT LEAST 2 E's (one Green, one Amber)
        // Words like BEGIN (only 1 E) should be filtered out
        
        Dictionary dict = createTestDictionary("beech", "betel", "begin", "beige", "beret", "rebel");
        WordGame game = new WordGame(dict, createTestConfig());
        game.setTargetWord("beech");
        
        Filter filter = new Filter(5);
        Response response = game.guess("betel");
        filter.update(response);
        
        Dictionary filtered = filter.apply(dict);
        
        // BEECH must remain (it's the target!)
        assertTrue(filtered.contains("beech"), 
            "BEECH (target) must remain after BETEL guess");
        
        // BEGIN should be filtered out (only has 1 E, but we need at least 2)
        assertFalse(filtered.contains("begin"), 
            "BEGIN should be filtered (has only 1 E, but target requires at least 2)");
        
        // BEIGE has 2 E's and matches position constraints, so it should REMAIN
        assertTrue(filtered.contains("beige"), 
            "BEIGE should remain (has 2 E's at positions 1 and 4, and E is not at position 3)");
        
        // BERET should be filtered (has E at position 3, which was marked Amber - wrong position)
        assertFalse(filtered.contains("beret"), 
            "BERET should be filtered (has E at position 3 which violates Amber constraint)");
        
        // REBEL should be filtered (E's not in correct positions)
        assertFalse(filtered.contains("rebel"), 
            "REBEL should be filtered (has 2 E's but doesn't have E at position 1)");
    }

    @Test
    @DisplayName("Integration: Target word always remains after any valid guess")
    void testTargetAlwaysRemains() throws InvalidWordException {
        String[] targets = {"jerky", "pansy", "mourn", "study", "sweet"};
        String[][] guesses = {
            {"arose", "deter"},
            {"raise", "salsa"},
            {"arose", "furor"},
            {"kneel", "mucus"},
            {"arose", "steel"}
        };
        
        for (int i = 0; i < targets.length; i++) {
            String target = targets[i];
            
            // Create dictionary with target and guess words
            Set<String> wordSet = new HashSet<>();
            wordSet.add(target);
            for (String guess : guesses[i]) {
                wordSet.add(guess);
            }
            Dictionary dict = new Dictionary(5);
            dict.addWords(wordSet);
            
            WordGame game = new WordGame(dict, createTestConfig());
            game.setTargetWord(target);
            Filter filter = new Filter(5);
            
            for (String guess : guesses[i]) {
                Response response = game.guess(guess);
                filter.update(response);
                
                Dictionary filtered = filter.apply(dict);
                assertTrue(filtered.contains(target), 
                    String.format("Target %s must remain after guess %s", 
                        target.toUpperCase(), guess.toUpperCase()));
            }
        }
    }

    // Helper method to create test dictionaries easily
    private Dictionary createTestDictionary(String... words) {
        Dictionary dict = new Dictionary(5);
        Set<String> wordSet = new HashSet<>();
        for (String word : words) {
            wordSet.add(word);
        }
        dict.addWords(wordSet);
        return dict;
    }
    
    // Helper method to create test Config objects
    private Config createTestConfig() {
        Config config = new Config();
        config.setWordLength(5);
        config.setMaxAttempts(6);
        return config;
    }
}

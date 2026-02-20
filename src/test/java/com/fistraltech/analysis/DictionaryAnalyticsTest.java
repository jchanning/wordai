package com.fistraltech.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;

/**
 * Tests for {@link DictionaryAnalytics} – letter frequency, positional stats,
 * and column-level frequency helpers.
 */
@DisplayName("DictionaryAnalytics Tests")
class DictionaryAnalyticsTest {

    private Dictionary dictionary;
    private DictionaryAnalytics analytics;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        // Deliberate word set so counts are predictable:
        // AABBB: A×2 B×3
        // CCAAA: C×2 A×3
        // Using real English words instead for readability:
        words.add("AROSE");  // A R O S E
        words.add("STARE");  // S T A R E
        words.add("CRANE");  // C R A N E
        dictionary.addWords(words);
        analytics = new DictionaryAnalytics(dictionary);
    }

    // ---- getLetterCount ----

    @Test
    @DisplayName("getLetterCount returns a non-empty map")
    void getLetterCount_returnsNonEmptyMap() {
        Map<Character, Integer> counts = analytics.getLetterCount();
        assertFalse(counts.isEmpty());
    }

    @Test
    @DisplayName("getLetterCount counts 'A' correctly across words")
    void getLetterCount_countsLetterA() {
        // AROSE has 1 A, STARE has 1 A, CRANE has 1 A => 3 total
        Map<Character, Integer> counts = analytics.getLetterCount();
        assertEquals(3, counts.get('A'), "A appears once in each of 3 words");
    }

    @Test
    @DisplayName("getLetterCount counts 'E' correctly across words")
    void getLetterCount_countsLetterE() {
        // AROSE, STARE, CRANE each end in E => 3 total
        Map<Character, Integer> counts = analytics.getLetterCount();
        assertEquals(3, counts.get('E'), "E appears once in each of 3 words");
    }

    @Test
    @DisplayName("getLetterCount counts 'R' correctly across words")
    void getLetterCount_countsLetterR() {
        // AROSE: R at 1, STARE: R at 3, CRANE: R at 1 => 3 total
        Map<Character, Integer> counts = analytics.getLetterCount();
        assertEquals(3, counts.get('R'), "R appears once in each of 3 words");
    }

    @Test
    @DisplayName("getLetterCount does not include absent letters")
    void getLetterCount_absentLetterNotInMap() {
        Map<Character, Integer> counts = analytics.getLetterCount();
        assertFalse(counts.containsKey('Z'), "Z should not appear in the counts");
    }

    // ---- getOccurrenceCountByPosition ----

    @Test
    @DisplayName("getOccurrenceCountByPosition returns a list of length wordLength for each letter")
    void getOccurrenceCountByPosition_listLengthEqualsWordLength() {
        Map<Character, List<Integer>> result = analytics.getOccurrenceCountByPosition();
        for (Map.Entry<Character, List<Integer>> entry : result.entrySet()) {
            assertEquals(5, entry.getValue().size(),
                "Each letter's positional list should have 5 entries");
        }
    }

    @Test
    @DisplayName("getOccurrenceCountByPosition: A at position 2 appears in AROSE and CRANE")
    void getOccurrenceCountByPosition_letterAtPosition() {
        Map<Character, List<Integer>> result = analytics.getOccurrenceCountByPosition();
        // AROSE: A at 0; STARE: A at 2; CRANE: A at 2 => A at pos 2 = 2 words
        List<Integer> aCounts = result.get('A');
        assertNotNull(aCounts, "A should have positional counts");
        assertEquals(2, aCounts.get(2), "A appears at position 2 in STARE and CRANE");
    }

    @Test
    @DisplayName("getOccurrenceCountByPosition: A at position 0 appears only in AROSE")
    void getOccurrenceCountByPosition_aAtPosition0() {
        Map<Character, List<Integer>> result = analytics.getOccurrenceCountByPosition();
        List<Integer> aCounts = result.get('A');
        assertNotNull(aCounts);
        assertEquals(1, aCounts.get(0), "A appears at position 0 only in AROSE");
    }

    // ---- getMostFrequentCharByPosition / getLeastFrequentCharByPosition ----

    @Test
    @DisplayName("getMostFrequentCharByPosition returns a list of length wordLength")
    void getMostFrequentCharByPosition_returnsCorrectLength() {
        List<Character> result = analytics.getMostFrequentCharByPosition();
        assertEquals(5, result.size(), "Should have one entry per word position");
    }

    @Test
    @DisplayName("getLeastFrequentCharByPosition returns a list of length wordLength")
    void getLeastFrequentCharByPosition_returnsCorrectLength() {
        List<Character> result = analytics.getLeastFrequentCharByPosition();
        assertEquals(5, result.size(), "Should have one entry per word position");
    }

    @Test
    @DisplayName("Most frequent char at position 4 is E (all three words end in E)")
    void getMostFrequentCharByPosition_endsInE() {
        List<Character> result = analytics.getMostFrequentCharByPosition();
        assertEquals('E', result.get(4), "All three words end in E so E must be most common at pos 4");
    }

    // ---- Dictionary with a single repeated letter ----

    @Test
    @DisplayName("Single-word dictionary: letter count equals the actual letter occurrences")
    void getLetterCount_singleWordDictionary() {
        Dictionary d = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("AABBB");
        // A×2, B×3
        // Can't use this because dictionary enforces words to be 5-char, but we need valid chars.
        // Use SPEED: S P E E D => E×2
        d = new Dictionary(5);
        words = new HashSet<>();
        words.add("SPEED");
        d.addWords(words);
        DictionaryAnalytics a = new DictionaryAnalytics(d);
        Map<Character, Integer> counts = a.getLetterCount();
        assertEquals(2, counts.get('E'), "SPEED has two E's");
        assertEquals(1, counts.get('S'), "SPEED has one S");
        assertEquals(1, counts.get('D'), "SPEED has one D");
        assertEquals(1, counts.get('P'), "SPEED has one P");
        assertTrue(!counts.containsKey('A'), "A is not in SPEED");
    }
}

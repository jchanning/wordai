package com.fistraltech.core;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dictionary Tests")
class DictionaryTest {

    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("AROSE");
        words.add("STARE");
        words.add("CRANE");
        words.add("SLATE");
        words.add("RAISE");
        dictionary.addWords(words);
    }

    @Test
    @DisplayName("contains returns true for a word in the dictionary")
    void contains_knownWord_returnsTrue() {
        assertTrue(dictionary.contains("AROSE"));
    }

    @Test
    @DisplayName("contains returns false for a word not in the dictionary")
    void contains_unknownWord_returnsFalse() {
        assertFalse(dictionary.contains("ZZZZY"));
    }

    @Test
    @DisplayName("getWordCount returns the correct total")
    void getWordCount_returnsCorrectCount() {
        assertEquals(5, dictionary.getWordCount());
    }

    @Test
    @DisplayName("getWordLength returns the configured word length")
    void getWordLength_returnsConfiguredLength() {
        assertEquals(5, dictionary.getWordLength());
    }

    @Test
    @DisplayName("getMasterSetOfWords contains all added words")
    void getMasterSetOfWords_containsAllWords() {
        Set<String> words = dictionary.getMasterSetOfWords();
        assertEquals(5, words.size());
        assertTrue(words.contains("AROSE"));
        assertTrue(words.contains("SLATE"));
    }

    @Test
    @DisplayName("getWords(char, position) returns words with that letter at that position")
    void getWords_byLetterAndPosition_filtersCorrectly() {
        // SLATE and STARE start with 'S'
        Set<String> result = dictionary.getWords('S', 0);
        assertTrue(result.contains("SLATE"), "SLATE has S at position 0");
        assertTrue(result.contains("STARE"), "STARE has S at position 0");
        assertFalse(result.contains("AROSE"), "AROSE does not have S at position 0");
    }

    @Test
    @DisplayName("getColumnLengths has one entry per word position")
    void getColumnLengths_hasOneEntryPerPosition() {
        assertEquals(5, dictionary.getColumnLengths().size());
    }

    @Test
    @DisplayName("addWords throws for words of the wrong length")
    void addWords_wrongLength_throwsIllegalArgument() {
        Set<String> bad = new HashSet<>();
        bad.add("CAT");
        assertThrows(IllegalArgumentException.class, () -> dictionary.addWords(bad));
    }

    @Test
    @DisplayName("selectRandomWord returns a word present in the dictionary")
    void selectRandomWord_returnsWordFromDictionary() {
        String word = dictionary.selectRandomWord();
        assertTrue(dictionary.getMasterSetOfWords().contains(word));
    }
}
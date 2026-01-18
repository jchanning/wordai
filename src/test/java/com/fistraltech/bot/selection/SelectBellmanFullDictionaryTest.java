package com.fistraltech.bot.selection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

class SelectBellmanFullDictionaryTest {

    @Test
    void prefersGuessesOutsideRemainingWhenAvailable() {
        Set<String> fullWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE",
                "STARE",
                "SLATE",
                "CRANE"
        ));

        Set<String> remainingWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE"
        ));

        Dictionary fullDictionary = new Dictionary(5);
        fullDictionary.addWords(fullWords);

        Dictionary remainingDictionary = new Dictionary(5);
        remainingDictionary.addWords(remainingWords);

        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(fullDictionary);
        String selected = algo.selectWord(new Response(""), remainingDictionary);

        assertTrue(!remainingWords.contains(selected),
                "Expected a guess outside remaining candidates when available");
    }

    @Test
    void throwsWhenNoExternalGuessesAvailable() {
        Set<String> remainingWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE"
        ));

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(remainingWords);

        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(dictionary);
        
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            algo.selectWord(new Response(""), dictionary);
        });
        
        assertTrue(exception.getMessage().contains("No candidate guesses available"),
                "Expected exception about no candidates available");
    }

    @Test
    void neverSelectsSameWordTwice() {
        Set<String> fullWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE",
                "STARE",
                "SLATE",
                "CRANE",
                "TARES",
                "CARES"
        ));

        Set<String> remainingWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE"
        ));

        Dictionary fullDictionary = new Dictionary(5);
        fullDictionary.addWords(fullWords);
        
        Dictionary remainingDictionary = new Dictionary(5);
        remainingDictionary.addWords(remainingWords);

        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(fullDictionary);
        Set<String> selectedWords = new HashSet<>();

        // Make several selections - use remaining dictionary so external guesses are available
        for (int i = 0; i < 5; i++) {
            String selected = algo.selectWord(new Response(""), remainingDictionary);
            assertFalse(selectedWords.contains(selected),
                    "Word '" + selected + "' was selected more than once");
            selectedWords.add(selected);
        }

        // Verify we got 5 unique words
        assertTrue(selectedWords.size() == 5,
                "Expected 5 unique words, got " + selectedWords.size());
    }
}

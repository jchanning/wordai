package com.fistraltech.bot.selection;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.util.ConfigManager;

@DisplayName("SelectBellmanFullDictionary Tests")
class SelectBellmanFullDictionaryTest {

    @Test
    @DisplayName("prefersPotentiallyCorrectGuessWhenReductionTies")
    void prefersPotentiallyCorrectGuessWhenReductionTies() {
        Set<String> fullWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE",
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
        double candidateScore = calculateExpectedRemaining(remainingDictionary, "AROSE");
        double externalScore = calculateExpectedRemaining(remainingDictionary, "CRANE");

        assertEquals(1.0d, candidateScore, 1e-9,
            "Expected a potentially correct guess to fully separate the two remaining words");
        assertEquals(candidateScore, externalScore, 1e-9,
            "Expected the external guess to tie with the potentially correct guess");

        String selected = algo.selectWord(new Response(""), remainingDictionary);

        assertTrue(remainingWords.contains(selected),
            "Expected a potentially correct guess when reduction is tied");
    }

    @Test
        @DisplayName("evaluatesRemainingCandidatesWhenNoExternalGuessesAvailable")
        void evaluatesRemainingCandidatesWhenNoExternalGuessesAvailable() {
        Set<String> remainingWords = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE"
        ));

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(remainingWords);

        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(dictionary);
        String selected = algo.selectWord(new Response(""), dictionary);

        assertTrue(remainingWords.contains(selected),
            "Expected the algorithm to consider remaining candidates when no external guesses exist");
    }

    @Test
        @DisplayName("neverSelectsSameWordTwice")
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

    private double calculateExpectedRemaining(Dictionary remainingDictionary, String guess) {
        WordEntropy analyser = new WordEntropy(remainingDictionary, ConfigManager.getInstance().createGameConfig(), false);
        return calculateExpectedRemaining(analyser.getResponseBuckets(guess), remainingDictionary.getWordCount());
    }

    private double calculateExpectedRemaining(Map<Short, Set<String>> buckets, int remainingSize) {
        double total = 0d;
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            double probability = (double) bucketSize / remainingSize;
            total += probability * bucketSize;
        }
        return total;
    }
}

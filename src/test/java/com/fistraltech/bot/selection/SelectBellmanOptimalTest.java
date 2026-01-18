package com.fistraltech.bot.selection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.ResponseHelper;

class SelectBellmanOptimalTest {

    @Test
    void selectsWordWithMinimumExpectedRemaining() throws InvalidWordException {
        Set<String> words = new LinkedHashSet<>(Arrays.asList(
                "AROSE",
                "RAISE",
                "STARE",
                "SLATE",
                "CRANE"
        ));

        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(words);

        SelectBellmanOptimal algo = new SelectBellmanOptimal(dictionary);
        String selected = algo.selectWord(new Response(""));

        Set<String> minWords = getMinExpectedRemainingWords(words);
        assertTrue(minWords.contains(selected),
                "Selected word should minimize expected remaining dictionary size");
    }

    @Test
    void returnsOnlyWordWhenDictionarySizeIsOne() {
        Set<String> words = new LinkedHashSet<>(Arrays.asList("AROSE"));
        Dictionary dictionary = new Dictionary(5);
        dictionary.addWords(words);

        SelectBellmanOptimal algo = new SelectBellmanOptimal(dictionary);
        String selected = algo.selectWord(new Response(""));

        assertEquals("AROSE", selected);
    }

    private Set<String> getMinExpectedRemainingWords(Set<String> words) throws InvalidWordException {
        int size = words.size();
        double bestScore = Double.MAX_VALUE;
        Set<String> bestWords = new HashSet<>();

        for (String guess : words) {
            double expected = calculateExpectedRemaining(guess, words, size);
            if (expected < bestScore) {
                bestScore = expected;
                bestWords.clear();
                bestWords.add(guess);
            } else if (Double.compare(expected, bestScore) == 0) {
                bestWords.add(guess);
            }
        }

        return bestWords;
    }

    private double calculateExpectedRemaining(String guess, Set<String> words, int dictionarySize)
            throws InvalidWordException {
        Map<String, Set<String>> buckets = new HashMap<>();

        for (String target : words) {
            Response response = ResponseHelper.evaluate(target, guess);
            String key = response.toString();
            buckets.computeIfAbsent(key, k -> new HashSet<>()).add(target);
        }

        double total = 0d;
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            double probability = (double) bucketSize / dictionarySize;
            total += probability * bucketSize;
        }
        return total;
    }
}

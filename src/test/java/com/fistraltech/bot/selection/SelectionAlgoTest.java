package com.fistraltech.bot.selection;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Tests for {@link SelectRandom} and {@link SelectMaximumEntropy} selection algorithms.
 */
@DisplayName("SelectionAlgo Tests")
class SelectionAlgoTest {

    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        words.add("stare");
        words.add("crane");
        words.add("slate");
        words.add("raise");
        words.add("rates");
        words.add("tears");
        words.add("react");
        words.add("crate");
        words.add("siren");
        dictionary.addWords(words);
    }

    // ---- SelectRandom ----

    @RepeatedTest(10)
    @DisplayName("SelectRandom always returns a word from the dictionary on first call")
    void selectRandom_firstCall_returnsWordFromDictionary() {
        SelectRandom algo = new SelectRandom(dictionary);
        String word = algo.selectWord(new Response(""));
        assertNotNull(word, "Should return a non-null word");
        assertTrue(dictionary.getMasterSetOfWords().contains(word),
            "Word '" + word + "' should be in the dictionary");
    }

    @Test
    @DisplayName("SelectRandom returns the last remaining word when only one candidate is left")
    void selectRandom_singleCandidate_returnsThatWord() {
        Dictionary single = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        single.addWords(words);

        SelectRandom algo = new SelectRandom(single);
        String word = algo.selectWord(new Response(""));
        assertEquals("arose", word);
    }

    @Test
    @DisplayName("SelectRandom algo name is set correctly")
    void selectRandom_algoName() {
        SelectRandom algo = new SelectRandom(dictionary);
        assertEquals("Random", algo.getAlgoName());
    }

    // ---- SelectMaximumEntropy ----

    @Test
    @DisplayName("SelectMaximumEntropy returns a word from the dictionary on first call")
    void selectMaxEntropy_firstCall_returnsWordFromDictionary() {
        SelectMaximumEntropy algo = new SelectMaximumEntropy(dictionary);
        String word = algo.selectWord(new Response(""));
        assertNotNull(word, "Should return a non-null word");
        assertTrue(dictionary.getMasterSetOfWords().contains(word),
            "Word '" + word + "' should be in the dictionary");
    }

    @Test
    @DisplayName("SelectMaximumEntropy returns the single remaining word when only one candidate is left")
    void selectMaxEntropy_singleCandidate_returnsThatWord() {
        Dictionary single = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        single.addWords(words);

        SelectMaximumEntropy algo = new SelectMaximumEntropy(single);
        String word = algo.selectWord(new Response(""));
        assertEquals("arose", word);
    }

    @Test
    @DisplayName("SelectMaximumEntropy algo name is set correctly")
    void selectMaxEntropy_algoName() {
        SelectMaximumEntropy algo = new SelectMaximumEntropy(dictionary);
        assertEquals("SelectMaximumEntropy", algo.getAlgoName());
    }

    @Test
    @DisplayName("SelectMaximumEntropy second call returns a word after a previous non-null response")
    void selectMaxEntropy_secondCall_returnsWordAfterResponse() throws com.fistraltech.core.InvalidWordException {
        SelectMaximumEntropy algo = new SelectMaximumEntropy(dictionary);

        // First call
        String firstWord = algo.selectWord(new Response(""));
        assertNotNull(firstWord);

        // Evaluate against a lowercase target word and select again
        com.fistraltech.core.WordGame game = new com.fistraltech.core.WordGame(dictionary, buildConfig());
        game.setTargetWord("arose");
        Response response = game.evaluate(firstWord);

        // Only assert non-null if the first guess was not the target (i.e., game not won yet)
        if (!response.getWinner()) {
            String secondWord = algo.selectWord(response);
            assertNotNull(secondWord, "Second selection should still return a word");
        }
    }

    // ---- SelectBellmanFullDictionary ----

    @Test
    @DisplayName("SelectBellmanFullDictionary algo is constructable with the full dictionary")
    void selectBellman_construction_succeeds() {
        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(dictionary);
        assertNotNull(algo);
    }

    // ---- Helpers ----

    private com.fistraltech.util.Config buildConfig() {
        com.fistraltech.util.Config config = new com.fistraltech.util.Config();
        config.setMaxAttempts(6);
        return config;
    }
}

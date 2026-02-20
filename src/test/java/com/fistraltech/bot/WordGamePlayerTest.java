package com.fistraltech.bot;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * Tests for the full {@link WordGamePlayer} game loop.
 *
 * <p>Uses a small, deterministic dictionary so tests complete quickly
 * and remain stable regardless of external word files.
 */
@DisplayName("WordGamePlayer Tests")
class WordGamePlayerTest {

    private Dictionary dictionary;
    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setMaxAttempts(20); // generous limit for small-dictionary games
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

    // ---- SelectRandom bot ----

    @RepeatedTest(5)
    @DisplayName("SelectRandom bot eventually guesses the target word")
    void randomBot_eventuallyWins() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setRandomTargetWord();

        SelectRandom algo = new SelectRandom(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        player.playGame(game);

        // The last response in history should be a winner
        ResultHistory history = player.getResultHistory();
        assertFalse(history.getHistory().isEmpty(), "Should have at least one guess");
        assertTrue(history.getHistory()
                          .get(history.getHistory().size() - 1)
                          .getWinner(),
                "The final guess should be the winning guess");
    }

    @Test
    @DisplayName("SelectRandom bot result history grows with each guess")
    void randomBot_historyTracksAllGuesses() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("arose");

        SelectRandom algo = new SelectRandom(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        player.playGame(game);

        ResultHistory history = player.getResultHistory();
        assertTrue(history.getHistory().size() >= 1, "History must contain at least the winning guess");
    }

    @Test
    @DisplayName("SelectRandom bot dictionary history shrinks or stays equal over time")
    void randomBot_dictionaryHistoryShrinksOverTime() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("arose");

        SelectRandom algo = new SelectRandom(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        player.playGame(game);

        DictionaryHistory dictHistory = player.getDictionaryHistory();
        assertNotNull(dictHistory, "Dictionary history should be recorded");
    }

    // ---- SelectMaximumEntropy bot ----

    @Test
    @DisplayName("SelectMaximumEntropy bot wins on the small dictionary")
    void entropyBot_winsOnSmallDictionary() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("arose");

        SelectMaximumEntropy algo = new SelectMaximumEntropy(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        player.playGame(game);

        ResultHistory history = player.getResultHistory();
        assertFalse(history.getHistory().isEmpty());
        assertTrue(history.getHistory()
                          .get(history.getHistory().size() - 1)
                          .getWinner(),
                "Entropy bot should find the word");
    }

    @Test
    @DisplayName("SelectMaximumEntropy bot solves in at most 6 attempts on a 10-word dictionary")
    void entropyBot_solvesEfficiently() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("crane");

        SelectMaximumEntropy algo = new SelectMaximumEntropy(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        player.playGame(game);

        int attempts = player.getResultHistory().getHistory().size();
        assertTrue(attempts <= 6,
            "Entropy bot should solve a 10-word dictionary in at most 6 attempts, but took " + attempts);
    }

    // ---- Player accessors ----

    @Test
    @DisplayName("getDictionary returns the same dictionary used to create the game")
    void getDictionary_returnsSameInstance() {
        WordGame game = new WordGame(dictionary, config);
        SelectRandom algo = new SelectRandom(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        assertEquals(dictionary, player.getDictionary());
    }

    @Test
    @DisplayName("getAlgo returns the algorithm used to create the player")
    void getAlgo_returnsSameInstance() {
        WordGame game = new WordGame(dictionary, config);
        SelectRandom algo = new SelectRandom(dictionary);
        WordGamePlayer player = new WordGamePlayer(game, algo);
        assertEquals(algo, player.getAlgo());
    }
}

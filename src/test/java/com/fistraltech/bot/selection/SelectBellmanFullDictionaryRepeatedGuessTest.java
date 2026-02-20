package com.fistraltech.bot.selection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fistraltech.bot.filter.Filter;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/**
 * Regression tests for the specific repeated-guess failures observed in historical CSV output.
 *
 * <p>Original failures (discovered via SelectBellmanFullDictionary):
 * <ul>
 *   <li>NICHE  - RAISE,LINGO,REUSE,REUSE,REUSE,REUSE (LOST)</li>
 *   <li>PALER  - RAISE,EMPTY,RIDER,RIDER,RIDER,RIDER (LOST)</li>
 *   <li>VOCAL  - RAISE,CLOUT,FOLLY,FOLLY,FOLLY,FOLLY (LOST)</li>
 *   <li>AROSE  - RAISE,REUSE,REUSE,REUSE,REUSE,REUSE (LOST)</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SelectBellmanFullDictionaryRepeatedGuessTest {

    private Dictionary dictionary;
    private Config config;

    @BeforeAll
    void loadDictionary() throws IOException {
        dictionary = Dictionary.createDictionary("classpath:dictionaries/5_letter_words.txt", 5);
        config = new Config();
        config.setMaxAttempts(20);
    }

    @Test
    void iteration430_NICHE_shouldNotRepeatGuess() throws InvalidWordException {
        assertNoRepeatedGuesses("niche");
    }

    @Test
    void iteration643_PALER_shouldNotRepeatGuess() throws InvalidWordException {
        assertNoRepeatedGuesses("paler");
    }

    @Test
    void iteration1861_VOCAL_shouldNotRepeatGuess() throws InvalidWordException {
        assertNoRepeatedGuesses("vocal");
    }

    @Test
    void iteration1849_AROSE_shouldNotRepeatGuess() throws InvalidWordException {
        assertNoRepeatedGuesses("arose");
    }

    @Test
    void noRepeatedGuessesAcrossSampleTargets() throws InvalidWordException {
        String[] targets = {"crane", "slate", "stare", "raise", "tears", "bread", "brown", "ghost"};
        for (String target : targets) {
            if (dictionary.contains(target)) {
                assertNoRepeatedGuesses(target);
            }
        }
    }

    // ---- Helper ----

    /**
     * Drives the game manually, mirroring how GameSession uses SelectBellmanFullDictionary:
     * - First guess uses a fixed opener ("raise"), made outside the Bellman algo
     *   (GameSession uses getWordWithMaximumReduction for the first guess).
     * - Subsequent guesses go through {@code algo.selectWord(filteredDictionary)}.
     *
     * SelectBellmanFullDictionary cannot be driven via WordGamePlayer because the
     * first call would receive fullDictionary as the remaining dict, causing an
     * IllegalStateException (no external candidates when remaining == full).
     */
    private void assertNoRepeatedGuesses(String target) throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord(target);

        SelectBellmanFullDictionary algo = new SelectBellmanFullDictionary(dictionary);
        Filter filter = new Filter(dictionary.getWordLength());

        Set<String> seen = new HashSet<>();
        List<String> guesses = new ArrayList<>();

        // First guess: fixed opener matching historical production behaviour
        String firstGuess = "raise";
        assertTrue(dictionary.contains(firstGuess),
            "Opening word '" + firstGuess + "' must be in dictionary");
        seen.add(firstGuess);
        guesses.add(firstGuess);

        Response response = game.evaluate(firstGuess);
        if (response.getWinner()) {
            return; // won on first guess
        }
        filter.update(response);
        Dictionary remaining = filter.apply(dictionary);

        // Subsequent guesses via Bellman (uses fullDictionary internally for candidate pool)
        boolean won = false;
        for (int attempt = 1; attempt < 15; attempt++) {
            String guess = algo.selectWord(remaining);
            assertFalse(seen.contains(guess),
                "Repeated guess for target '" + target + "': '" + guess + "' at attempt "
                    + (attempt + 1) + ". Sequence: " + guesses);
            seen.add(guess);
            guesses.add(guess);

            response = game.evaluate(guess);
            if (response.getWinner()) {
                won = true;
                break;
            }
            filter.update(response);
            remaining = filter.apply(dictionary);
        }

        assertTrue(won,
            "Game not won for target '" + target + "'. Guesses: " + guesses);
        assertTrue(guesses.size() <= 15,
            "Expected to solve '" + target + "' in at most 15 guesses, took: " + guesses.size());
    }
}

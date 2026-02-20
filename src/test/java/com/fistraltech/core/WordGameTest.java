package com.fistraltech.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.fistraltech.util.Config;

/**
 * Tests for the core WordGame guess/response logic.
 */
@DisplayName("WordGame Tests")
class WordGameTest {

    private Dictionary dictionary;
    private Config config;

    @BeforeEach
    void setUp() {
        config = new Config();
        config.setMaxAttempts(6);
        dictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("AROSE");
        words.add("RAISE");
        words.add("STARE");
        words.add("CRANE");
        words.add("SLATE");
        words.add("BEACH");
        words.add("BEECH");
        words.add("HELLO");
        words.add("WORLD");
        words.add("MUCUS");
        words.add("STUDY");
        words.add("JERKY");
        words.add("ELDER");
        dictionary.addWords(words);
    }

    // ---- setTargetWord ----

    @Test
    @DisplayName("setTargetWord accepts a word that is in the dictionary")
    void setTargetWord_validWord_setsWithoutException() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        assertEquals("AROSE", game.getTargetWord());
    }

    @Test
    @DisplayName("setTargetWord throws InvalidWordException for an unknown word")
    void setTargetWord_unknownWord_throwsException() {
        WordGame game = new WordGame(dictionary, config);
        assertThrows(InvalidWordException.class, () -> game.setTargetWord("ZZZZY"));
    }

    // ---- Exact match ----

    @Test
    @DisplayName("Exact match produces all-Green response and is a winner")
    void guess_exactMatch_allGreenAndWinner() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        Response r = game.guess("AROSE");

        assertTrue(r.getWinner(), "Exact match should be a winner");
        assertEquals("AROSE", r.getWord());
        for (ResponseEntry entry : r.getStatuses()) {
            assertEquals('G', entry.status, "Every position should be Green for exact match");
        }
    }

    // ---- Per-position response codes ----

    @ParameterizedTest
    @DisplayName("Response codes match G/A/R/X semantics")
    @CsvSource({
        // target,  guess,   expected per-position response
        "AROSE, RAISE, AARGG",   // R in word wrong pos, A in word wrong pos, I absent, S correct, E correct
        "AROSE, STARE, ARAAG",   // S wrong pos, T absent, A correct, R wrong pos, E correct
        "AROSE, CRANE, RGARG",   // C absent=R, R correct pos=G, A wrong pos=A, N absent=R, E correct=G
        "BEACH, BEECH, GGXGG",   // B G, E G, extra E excess, C G, H G
        "BEECH, BEACH, GGRGG",   // B G, E G, A absent, C G, H G
        "STUDY, MUCUS, RARXA"    // M absent=R, U wrong pos=A, C absent=R, second U excess=X, S wrong pos=A
    })
    void guess_responseCodes(String target, String guess, String expected) throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord(target);
        Response r = game.guess(guess);

        List<ResponseEntry> statuses = r.getStatuses();
        assertEquals(expected.length(), statuses.size());
        for (int i = 0; i < expected.length(); i++) {
            assertEquals(expected.charAt(i), statuses.get(i).status,
                String.format("Position %d: guess '%s' vs target '%s'", i, guess, target));
        }
    }

    // ---- Winner flag ----

    @Test
    @DisplayName("Non-exact guess is not flagged as winner")
    void guess_nonExact_notWinner() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        Response r = game.guess("RAISE");
        assertFalse(r.getWinner());
    }

    // ---- Guess count tracking ----

    @Test
    @DisplayName("getNoOfAttempts increments after each guess")
    void getNoOfAttempts_incrementsEachGuess() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        assertEquals(0, game.getNoOfAttempts());
        game.guess("RAISE");
        assertEquals(1, game.getNoOfAttempts());
        game.guess("STARE");
        assertEquals(2, game.getNoOfAttempts());
    }

    @Test
    @DisplayName("getGuesses returns responses in order")
    void getGuesses_returnsResponsesInOrder() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        game.guess("RAISE");
        game.guess("SLATE");
        List<Response> guesses = game.getGuesses();
        assertEquals(2, guesses.size());
        assertEquals("RAISE", guesses.get(0).getWord());
        assertEquals("SLATE", guesses.get(1).getWord());
    }

    // ---- Validation ----

    @Test
    @DisplayName("Guessing a word not in the dictionary throws InvalidWordException")
    void guess_wordNotInDictionary_throwsException() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        assertThrows(InvalidWordException.class, () -> game.guess("ZZZZY"));
    }

    @Test
    @DisplayName("Guessing a word of wrong length throws InvalidWordLengthException")
    void guess_wrongLength_throwsException() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        assertThrows(InvalidWordException.class, () -> game.guess("CAT"));
    }

    @Test
    @DisplayName("Exceeding max attempts throws an exception")
    void guess_exceedsMaxAttempts_throwsException() throws InvalidWordException {
        config.setMaxAttempts(2);
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        game.guess("RAISE");
        game.guess("STARE");
        assertThrows(InvalidWordException.class, () -> game.guess("CRANE"));
    }

    // ---- evaluate() does not record guess ----

    @Test
    @DisplayName("evaluate() does not increment the guess counter")
    void evaluate_doesNotRecordGuess() throws InvalidWordException {
        WordGame game = new WordGame(dictionary, config);
        game.setTargetWord("AROSE");
        game.evaluate("RAISE");
        assertEquals(0, game.getNoOfAttempts());
    }

    // ---- Duplicate letter edge cases ----

    @Test
    @DisplayName("JERKY scenario: single-E guess vs double-E target produces correct response")
    void guess_jerkyScenario_singleLetterGuessVsDoubleLetterTarget() throws InvalidWordException {
        // ELDER has 2 E's. Guessing AROSE vs ELDER:
        // A-absent, R-wrongpos, O-absent, S-absent, E-wrongpos
        Dictionary dict2 = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("ELDER");
        words.add("AROSE");
        dict2.addWords(words);
        WordGame game = new WordGame(dict2, config);
        game.setTargetWord("ELDER");
        Response r = game.evaluate("AROSE");
        List<ResponseEntry> s = r.getStatuses();
        // A not in ELDER -> R
        assertEquals('R', s.get(0).status, "A not in ELDER");
        // E in ELDER  -> A (wrong position, pos 0 vs pos 0 in elder? no, E is at pos 0 in ELDER)
        // Actually E is at pos 0 in ELDER and pos 0 in AROSE - wait AROSE: A(0)R(1)O(2)S(3)E(4)
        // ELDER: E(0)L(1)D(2)E(3)R(4)
        // A vs E -> R (A pos 0, not in ELDER)
        // R vs L -> A (R is in ELDER at pos 4, wrong position)
        // O vs D -> R
        // S vs E -> R (S not in ELDER)
        // E vs R -> A (E is in ELDER but not at pos 4? Well ELDER has E at 0 and 3...)
        // E(4 in guess) vs R(4 in target) - E is in ELDER -> A (present but wrong pos)
        assertEquals('A', s.get(1).status, "R is in ELDER at pos 4, wrong position in guess");
        assertEquals('R', s.get(2).status, "O not in ELDER");
        assertEquals('R', s.get(3).status, "S not in ELDER");
        assertEquals('A', s.get(4).status, "E is in ELDER but not at pos 4");
    }
}

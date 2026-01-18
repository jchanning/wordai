package com.fistraltech.bot.selection;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.util.ConfigManager;

/**
 * Bellman-style selection that allows guesses from the full dictionary even when
 * they are not currently valid candidates, prioritizing maximum reduction of the
 * remaining search space.
 *
 * <p>If more than one candidate remains, this strategy prefers guesses outside the
 * remaining set ("known incorrect") to maximize information gain. When only one
 * candidate remains, it returns that word to finish the game.
 */
public class SelectBellmanFullDictionary extends SelectionAlgo {

    private static final Logger logger = Logger.getLogger(SelectBellmanFullDictionary.class.getName());
    private final Dictionary fullDictionary;
    private final Set<String> guessedWords;

    public SelectBellmanFullDictionary(Dictionary dictionary) {
        super(dictionary);
        this.fullDictionary = dictionary;
        this.guessedWords = new HashSet<>();
        setAlgoName("SelectBellmanFullDictionary");
    }

    /**
     * Selects a word using an explicit remaining dictionary while still allowing
     * guesses from the full dictionary.
     *
     * @param remainingDictionary the current filtered dictionary
     * @return the next guess
     */
    public String selectWord(Dictionary remainingDictionary) {
        return selectWord(new Response(""), remainingDictionary);
    }

    @Override
    String selectWord(Response lastResponse, Dictionary remainingDictionary) {
        if (remainingDictionary.getWordCount() == 1) {
            String finalWord = remainingDictionary.selectRandomWord();
            if (guessedWords.contains(finalWord)) {
                logger.warning("Final word " + finalWord + " was already guessed! This should not happen.");
            }
            guessedWords.add(finalWord);
            return finalWord;
        }

        Set<String> remainingWords = remainingDictionary.getMasterSetOfWords();
        Set<String> candidateGuesses = new HashSet<>(fullDictionary.getMasterSetOfWords());

        // Remove already guessed words from ALL candidates first
        candidateGuesses.removeAll(guessedWords);

        if (remainingDictionary.getWordCount() > 1) {
            // Prefer guesses outside remaining (Bellman strategy)
            candidateGuesses.removeAll(remainingWords);
        }

        // Fail fast if no candidates available
        if (candidateGuesses.isEmpty()) {
            throw new IllegalStateException(
                "No candidate guesses available. Full dictionary size: " + fullDictionary.getWordCount() + 
                ", Remaining words: " + remainingDictionary.getWordCount() + 
                ", Already guessed: " + guessedWords.size() + 
                ". All words from full dictionary have either been guessed or are in the remaining set."
            );
        }

        WordEntropy analyser = new WordEntropy(remainingDictionary, ConfigManager.getInstance().createGameConfig(), false);
        int remainingSize = remainingDictionary.getWordCount();

        String bestWord = null;
        double bestScore = Double.MAX_VALUE;

        for (String guess : candidateGuesses) {
            double expectedRemaining = calculateExpectedRemaining(analyser.getResponseBuckets(guess), remainingSize);
            if (expectedRemaining < bestScore) {
                bestScore = expectedRemaining;
                bestWord = guess;
            }
        }

        if (bestWord == null) {
            throw new IllegalStateException(
                "No best word found after evaluating " + candidateGuesses.size() + " candidates. " +
                "This indicates a logic error in the selection algorithm."
            );
        }

        if (guessedWords.contains(bestWord)) {
            throw new IllegalStateException(
                "Selected word '" + bestWord + "' was already guessed. " +
                "Guessed words: " + guessedWords + ". This should never happen."
            );
        }

        guessedWords.add(bestWord);
        return bestWord;
    }

    private double calculateExpectedRemaining(Map<Short, Set<String>> buckets, int remainingSize) {
        double total = 0d;
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            if (bucketSize == 0) {
                continue;
            }
            double probability = (double) bucketSize / remainingSize;
            total += probability * bucketSize;
        }
        return total;
    }

    /**
     * Resets the algorithm state for a new game.
     * Clears the guessed words set in addition to parent reset operations.
     */
    @Override
    public void reset() {
        super.reset();
        int sizeBeforeReset = guessedWords.size();
        guessedWords.clear();
        if (sizeBeforeReset > 0) {
            logger.info("Reset: Cleared " + sizeBeforeReset + " guessed words");
        }
    }
}

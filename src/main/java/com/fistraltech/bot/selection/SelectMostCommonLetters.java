package com.fistraltech.bot.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Selection algorithm that biases guesses toward high-frequency letters in the remaining candidate set.
 *
 * <p>The algorithm computes overall letter frequencies for the current dictionary and then filters the dictionary
 * to words that contain the top N most frequent letters. A word is then selected at random from this subset.
 *
 * <p><strong>Heuristic</strong>
 * <ul>
 *   <li>Compute letter counts over remaining candidates.</li>
 *   <li>Choose the top 3 letters and filter to words containing them.</li>
 *   <li>If that yields no candidates (too strict), fall back to top 2 letters.</li>
 *   <li>Select a random word from the resulting subset.</li>
 * </ul>
 *
 * <p><strong>When to use</strong>: a lightweight improvement over pure random selection.
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 */

public class SelectMostCommonLetters extends SelectionAlgo {

    public SelectMostCommonLetters(Dictionary dictionary){
        super(dictionary);
        setAlgoName("MostCommonLetters");
    }

    /**
     * Selects a candidate word using the most common letters in the current dictionary.
     *
     * <p>Note: {@code lastResponse} is not used directly; the {@link SelectionAlgo} base class has already
     * applied it to produce the filtered {@code dictionary} passed in here.
     */
    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        DictionaryAnalytics analyser = new DictionaryAnalytics(dictionary);
        List<Map.Entry<Character, Integer>> letterFrequency = new ArrayList<>(analyser.getLetterCount().entrySet());
        letterFrequency.sort(Map.Entry.comparingByValue());

        Dictionary subset = dictionary;

        int numberOfLetters = letterFrequency.size();
        int numberOfCommonLetters = 3;
        int lettersToConsider = Math.min(numberOfLetters, numberOfCommonLetters);
        for(int i = 0; i < lettersToConsider; ++i){
            char letter = letterFrequency.get(numberOfLetters-(i+1)).getKey();
            subset = subset.getWords(letter);
        }

        if(subset.getWordCount() == 0){
            subset = dictionary;
            numberOfCommonLetters = 2;
            lettersToConsider = Math.min(numberOfLetters, numberOfCommonLetters);
            for(int i = 0; i < lettersToConsider; ++i){
                char letter = letterFrequency.get(numberOfLetters-(i+1)).getKey();
                subset = subset.getWords(letter);
            }
        }
        return subset.selectRandomWord();
    }
}

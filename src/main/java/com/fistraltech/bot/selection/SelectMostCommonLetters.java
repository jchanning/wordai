package com.fistraltech.bot.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
/** In this strategy, a configurable number of the most commonly occurring letters are selected and the word list is
 * filtered to contain only the words containing those letters. A word is then  randomly selected from this subset.  
 * Selecting 3 of the most commonly found letters was found to be the optimal strategy, using 4 or 5 letters 
 * frequently leads to an empty list of words.
 *
 * Large scale simulation testing proved that this strategy is marginally better than just a random selection.
 * */

public class SelectMostCommonLetters extends SelectionAlgo {

    public SelectMostCommonLetters(Dictionary dictionary){
        super(dictionary);
        setAlgoName("MostCommonLetters");
    }

    @Override
    /**
     * Selects a word from the dictionary based on the most common letters found in the last response.
     *
     * @param lastResponse the last response received
     * @param dictionary the dictionary to select words from
     * @return a randomly selected word from the filtered subset of words
     */
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

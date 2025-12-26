package com.fistraltech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fistraltech.core.Column;
import com.fistraltech.core.Dictionary;
/**
 * Performs statistical and information–theoretic analysis over a {@link Dictionary}.
 * <p>
 * Core responsibilities:
 * <ul>
 *   <li>Aggregate letter frequency statistics (overall and by position).</li>
 *   <li>Bucket words by their feedback (response) pattern when a candidate word is guessed.</li>
 *   <li>Compute Shannon entropy for a candidate guess to estimate expected information gain.</li>
 *   <li>Identify words with maximal entropy (strong initial or exploratory guesses).</li>
 * </ul>
 * The analysis functions enable selection algorithms to prioritise guesses that reduce the remaining
 * search space most efficiently.
 * <p>
 * Performance notes:
 * <ul>
 *   <li>Methods that iterate the full dictionary are O(N * L) where N = number of words, L = word length.</li>
 *   <li>{@code getResponseBuckets} uses response caching: responses for word pairs are computed once and reused.</li>
 *   <li>Entropy calculations depend on response bucket distribution and are O(B) where B = number of distinct patterns.</li>
 *   <li>Response cache dramatically improves {@code getMaximumEntropyWord} from O(N²×C) to O(N²×C) first call, O(N²) subsequent.</li>
 * </ul>
 * Thread-safety: This analyser is not inherently thread-safe; concurrent access should provide external synchronisation
 * if the underlying {@link Dictionary} is mutable.
 */

/** A class used to analyse Dictionaries */
public class DictionaryAnalytics {

    private final Dictionary dictionary;
    
    public DictionaryAnalytics(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /**
     * Counts total occurrences of each letter across all words, disregarding position.
     * @return map from letter to total count (unspecified order – currently {@link TreeMap} sorted by letter)
     * Complexity: O(N * L)
     */
    public Map<Character, Integer> getLetterCount() {
        Map<Character,Integer> letterCount = new TreeMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        int wordLength = dictionary.getWordLength();
        for(String word: words){
            char[] ch = word.toCharArray();
            for(int i=0; i< wordLength;++i){
                if(letterCount.containsKey(ch[i])){
                    int newCount = letterCount.get(ch[i]);
                    letterCount.put(ch[i],++newCount);
                }
                else{
                    letterCount.put(ch[i],1);
                }

            }
        }
        return letterCount;
    }

    /**
     * For each letter, returns a list of counts by positional index (0..wordLength-1).
     * The list length equals dictionary word length; each entry is how many words contain the letter at that position.
     * Initialises absent letters lazily.
     * @return map from letter to list of positional counts.
     * Complexity: O(N * L)
     */
    public Map<Character, List<Integer>> getOccurrenceCountByPosition() {
        Map<Character, List<Integer>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        int wordLength = dictionary.getWordLength();

        for (String word : words) {
            char[] ch = word.toCharArray();
            for (int i = 0; i < wordLength; ++i) {
                if (result.containsKey(ch[i])) {
                    List<Integer> countByPosition = result.get(ch[i]);
                    int newCount = countByPosition.get(i);
                    countByPosition.set(i, ++newCount);

                } else {
                    List<Integer> countByPosition = new ArrayList<>(wordLength);
                    //Not sure why this is necessary to initialise the list, there must be a better alternative
                    for (int j = 0; j < wordLength; ++j) { countByPosition.add(0);}
                    countByPosition.set(i, 1);
                    result.put(ch[i], countByPosition);
                    }
                }
            }
        return result;
    }
  
    /**
     * Returns, for each column (position), the most frequently occurring letter across all words.
     * @return list of letters where index i corresponds to column i
     */
    public List<Character> getMostFrequentCharByPosition(){
        List<Character> result = new ArrayList<>();
        for(Column c: dictionary.getColumns()){
            result.add(c.getMostCommonLetter());
        }
        return result;
    }

    /**
     * Returns, for each column (position), the least frequently occurring letter across all words.
     * @return list of letters where index i corresponds to column i
     */
    public List<Character> getLeastFrequentCharByPosition(){
        List<Character> result = new ArrayList<>();
        for(Column c: dictionary.getColumns()){
            result.add(c.getLeastCommonLetter());
        }
        return result;
    }
}

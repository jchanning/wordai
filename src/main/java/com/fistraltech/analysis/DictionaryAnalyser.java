package com.fistraltech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fistraltech.core.Column;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;
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
 *   <li>{@code getResponseBuckets} constructs a new {@link WordGame} per target word; consider caching if invoked heavily.</li>
 *   <li>Entropy calculations depend on response bucket distribution and are O(B) where B = number of distinct patterns.</li>
 * </ul>
 * Thread-safety: This analyser is not inherently thread-safe; concurrent access should provide external synchronisation
 * if the underlying {@link Dictionary} is mutable.
 */

/** A class used to analyse Dictionaries */
public class DictionaryAnalyser {

    private final Dictionary dictionary;

    public DictionaryAnalyser(Dictionary dictionary) {
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
     * Groups every word in the dictionary into buckets keyed by the response pattern produced
     * when comparing the candidate {@code word} against each target word.
     * The response pattern string encodes per-position feedback (e.g. Greens, Ambers, Reds, Excess).
     * @param word candidate guess word used to produce response patterns
     * @return map from response pattern string to set of words generating that pattern
     * Complexity: O(N * C) where C is cost of computing a response (roughly O(L)).
     */
    public Map<String, Set<String>> getResponseBuckets(String word) {
        Map<String, Set<String>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        for (String w : words) {
            try {
                Config config = new Config();
                WordGame game = new WordGame(dictionary, config);
                game.setTargetWord(w);
                Response r = game.guess(w, word);
                String bucket = r.toString();
                result.computeIfAbsent(bucket, k -> new HashSet<>()).add(w);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }


    /**
     * Finds the word with maximal Shannon entropy relative to the current dictionary state.
     * Higher entropy implies greater expected reduction of remaining search space on next guess.
     * @return word yielding highest entropy (first encountered in case of ties)
     * Complexity: O(N * (B + response cost)) due to repeated entropy computations.
     */
    public String getMaximumEntropyWord(){
        float maximumEntropy = 0f;
        String result="";
        for(String word: dictionary.getMasterSetOfWords()){
            if(maximumEntropy == 0f){
                maximumEntropy = getEntropy(word);
                result = word;
            }
            else{
                float e = getEntropy(word);
                if(e>maximumEntropy){
                    maximumEntropy = e;
                    result = word;
                }
            }
        }
        return result;
    }
    /**
     * Calculates Shannon entropy (in bits) for a candidate guess based on distribution of
     * response pattern buckets: -Σ p * log2(p). Higher values indicate greater expected information gain.
     * @param word candidate guess word
     * @return entropy value in bits (0 if dictionary empty)
     */
    public float getEntropy(String word) {
        int dictionarySize = dictionary.getWordCount();
        
        // Early exit for edge cases
        if (dictionarySize == 0) {
            return 0f;
        }
        
        Map<String, Set<String>> buckets = getResponseBuckets(word);
        float entropy = 0f;
        
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            
            // Skip empty buckets (shouldn't happen but safe to check)
            if (bucketSize == 0) {
                continue;
            }
            
            // Calculate probability and entropy contribution
            double probability = (double) bucketSize / dictionarySize;
            
            // Shannon entropy formula: -sum(p * log2(p))
            double logProbability = Math.log(probability) / Math.log(2);
            entropy += -(probability * logProbability);
        }
        
        return entropy;
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

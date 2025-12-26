package com.fistraltech.bot.selection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Selection strategy that minimises the expected sum of column lengths after a potential guess.
 * <p>
 * This strategy calculates, for each candidate word, the expected number of remaining possible
 * letter positions after guessing that word. It uses response buckets to group outcomes and
 * directly computes column statistics from the bucket contents, avoiding expensive filtering.
 * <p>
 * Algorithm:
 * 1. For each candidate word, get response buckets (groups of targets producing same response)
 * 2. For each bucket, calculate column lengths directly from the bucket's word set
 * 3. Calculate weighted average: Σ (bucket_probability × bucket_column_lengths)
 * 4. Select word with minimum expected column lengths
 * <p>
 * Optimization: Results are cached based on dictionary size to avoid recalculation when the
 * dictionary hasn't changed. This is critical for the first guess where the full dictionary
 * is used.
 * <p>
 * Complexity: O(N × B × L) where N = dictionary size, B = number of unique response buckets
 * (typically much smaller than N), L = word length. With caching, subsequent calls on the same
 * dictionary are O(1).
 */
public class MinimiseColumnLengths extends SelectionAlgo{
    
    // Cache: dictionary size -> (word -> expected column length)
    private static final Map<Integer, Map<String, Float>> cache = new HashMap<>();

    public MinimiseColumnLengths(Dictionary dictionary){
        super(dictionary);
        setAlgoName("MinimiseColumnLengths");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary)
    {
        if (dictionary.getWordCount() == 0) {
            return "";
        }
        
        // If only one word left, return it
        if (dictionary.getWordCount() == 1) {
            return dictionary.selectRandomWord();
        }
        
        int dictSize = dictionary.getWordCount();
        Map<String, Float> cachedResults = cache.get(dictSize);
        
        // If we have cached results for this dictionary size, use them
        if (cachedResults != null && !cachedResults.isEmpty()) {
            return findMinimumWord(cachedResults, dictionary);
        }
        
        // Otherwise, calculate and cache
        Map<String, Float> results = new HashMap<>();
        WordEntropy analyser = new WordEntropy(dictionary);
        
        // Evaluate each candidate word
        for (String candidateWord : dictionary.getMasterSetOfWords()) {
            float expectedColumnLength = calculateExpectedColumnLength(candidateWord, dictionary, analyser);
            results.put(candidateWord, expectedColumnLength);
        }
        
        // Cache the results
        cache.put(dictSize, results);
        
        return findMinimumWord(results, dictionary);
    }
    
    /**
     * Finds the word with minimum expected column length from cached results.
     * Only considers words that are in the current dictionary.
     */
    private String findMinimumWord(Map<String, Float> results, Dictionary dictionary) {
        float minExpectedColumnLength = Float.MAX_VALUE;
        String bestWord = "";
        
        Set<String> validWords = dictionary.getMasterSetOfWords();
        for (String word : validWords) {
            Float expectedLength = results.get(word);
            if (expectedLength != null && expectedLength < minExpectedColumnLength) {
                minExpectedColumnLength = expectedLength;
                bestWord = word;
            }
        }
        
        return bestWord.isEmpty() ? dictionary.selectRandomWord() : bestWord;
    }
    
    /**
     * Calculates the expected total column length if we guess the candidate word.
     * Uses response buckets to efficiently group possible outcomes and directly
     * calculates column lengths from bucket contents.
     * 
     * @param candidateWord the word to evaluate
     * @param dictionary the current dictionary
     * @param analyser pre-constructed analyser for this dictionary
     * @return expected column length (lower is better)
     */
    private float calculateExpectedColumnLength(String candidateWord, Dictionary dictionary, WordEntropy analyser) {
        Map<String, Set<String>> buckets = analyser.getResponseBuckets(candidateWord);
        int dictionarySize = dictionary.getWordCount();
        int wordLength = dictionary.getWordLength();
        
        float expectedLength = 0f;
        
        for (Map.Entry<String, Set<String>> entry : buckets.entrySet()) {
            Set<String> bucketWords = entry.getValue();
            int bucketSize = bucketWords.size();
            
            // Skip empty buckets
            if (bucketSize == 0) {
                continue;
            }
            
            // Calculate probability of this response pattern
            double probability = (double) bucketSize / dictionarySize;
            
            // Calculate column lengths directly from bucket words
            int columnLength = calculateColumnLengthForWords(bucketWords, wordLength);
            
            // Add weighted contribution to expected value
            expectedLength += probability * columnLength;
        }
        
        return expectedLength;
    }
    
    /**
     * Calculates the total column length (sum of unique letters per position) for a set of words.
     * This is equivalent to creating a Dictionary from these words and calling getLetterCount(),
     * but more efficient as it doesn't require creating Dictionary objects.
     * 
     * @param words the set of words to analyze
     * @param wordLength the length of each word
     * @return total number of unique letters across all positions
     */
    private int calculateColumnLengthForWords(Set<String> words, int wordLength) {
        if (words.isEmpty()) {
            return 0;
        }
        
        // For each position, collect unique letters
        int totalLength = 0;
        for (int pos = 0; pos < wordLength; pos++) {
            Set<Character> uniqueLetters = new HashSet<>();
            for (String word : words) {
                uniqueLetters.add(word.charAt(pos));
            }
            totalLength += uniqueLetters.size();
        }
        
        return totalLength;
    }

}

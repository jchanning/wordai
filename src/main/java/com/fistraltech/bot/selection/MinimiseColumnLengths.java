package com.fistraltech.bot.selection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * This is a variation of the SelectMximunEntropy algorithm. The target function looks to 
 * minimise the column length (i.e. number of  valid letter choices, across the positionse in
 * the word) rather than maximise entropy. 
 * 
 * In many cases, a single column may have a very high number of remaining valid letters, which 
 * can lead to a loss. Eliminating this risk is the target of this algorithm.There are other 
 * techniques that can address this but it involves guessing words that are known to be
 * wrong.
 *  
 * This strategy calculates, for each guess word, the number of remaining possible
 * letters by positions for each target word in the dictionary. It then calcuates the average across
 * the target words. Note variants on this strategt would focus on the minimum of maximum column 
 * lengths which would identify the best or worst case scenarios.
 * 
 * Although the total number of possible words is the multiple of the column count from p0 to pn,
 * the majority of combinations are invalid (e.g. ABAAB, DDXXA, etc. are not in the English language 
 * dictionary). Using a sum is assumed to give a better target to minimise, but this remains to be 
 * proven.
 * 
 * This algorithm does not use response buckets to group outcomes, but that is a possible optimisation. 
 * It just needs to know the column sum for each guess word across all target words.
 *  
 * Algorithm:
 * 1. For each guess word and target word pair, get the response 
 * 2. Filter the dictionary based on that response to get the possible remaining words
 * 3. Calculate the column lengths for the remaining words
 * 4. Calculate the average column length across all target words for the guess word
 * 5. Select the guess word with the minimum average column length
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
        
        // Evaluate each candidate (guess) word
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
        //Iterate over valid words
        for (String word : validWords) {
            Float expectedLength = results.get(word);
            if (expectedLength != null && expectedLength < minExpectedColumnLength) {
                minExpectedColumnLength = expectedLength;
                bestWord = word;
            }
        }
        
        // Best word should not be empty, care needs to be taken here as it random selection is misleading
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
            // Multiply total length by number of unique letters in this position to get all possibilities. This replaces a simple sum.
            if(totalLength == 0)
                totalLength = uniqueLetters.size();
            else totalLength *= uniqueLetters.size();
        }
        
        return totalLength;
    }

}

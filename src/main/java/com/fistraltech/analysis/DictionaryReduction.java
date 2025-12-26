package com.fistraltech.analysis;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.GameResponse;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;

public class DictionaryReduction {
    private final Dictionary dictionary;

    public DictionaryReduction(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    public String getWordWithMaximumReduction(){
        String bestWord = null;
        double minAverageDictionarySize = Double.MAX_VALUE;

        for (String candidateWord : dictionary.getMasterSetOfWords()) {
            double averageSize = calculateAverageDictionarySize(candidateWord);
            if (averageSize < minAverageDictionarySize) {
                minAverageDictionarySize = averageSize;
                bestWord = candidateWord;
            }
        }

        return bestWord;
    }

    /**
     * Calculates the expected (average) dictionary size after using the given guess word.
     * This is computed by grouping all possible target words by their response pattern,
     * then taking the weighted average of the bucket sizes.
     */
    private double calculateAverageDictionarySize(String guessWord){
        java.util.Map<String, java.util.Set<String>> responseBuckets = new java.util.HashMap<>();
        
        // Group all words by their response pattern when guessWord is used as the guess
        for (String targetWord : dictionary.getMasterSetOfWords()) {
            try{
                Response response = GameResponse.evaluate(targetWord, guessWord);
                String responseKey = response.toString();
                
                responseBuckets.computeIfAbsent(responseKey, k -> new java.util.HashSet<>()).add(targetWord);
            } catch (InvalidWordException e) {
                //Should not happen as words are from the dictionary
                e.printStackTrace();
            }
        }
        
        // Calculate weighted average of bucket sizes
        double totalSize = 0;
        int totalWords = dictionary.getWordCount();
        
        for (java.util.Set<String> bucket : responseBuckets.values()) {
            int bucketSize = bucket.size();
            double probability = (double) bucketSize / totalWords;
            totalSize += probability * bucketSize;
        }
        
        return totalSize;
    }
}

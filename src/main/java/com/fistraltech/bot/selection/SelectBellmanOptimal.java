package com.fistraltech.bot.selection;

import java.util.Map;
import java.util.Set;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.util.ConfigManager;

/**
 * Selection algorithm that minimizes the expected remaining dictionary size
 * after the next guess (Bellman one-step optimality).
 *
 * <p>The expected remaining size is computed by grouping all possible target
 * words by their response pattern to a candidate guess and taking the weighted
 * average of the bucket sizes. The word with the smallest expected remaining
 * size is selected.
 */
public class SelectBellmanOptimal extends SelectionAlgo {

    public SelectBellmanOptimal(Dictionary dictionary) {
        super(dictionary);
        setAlgoName("SelectBellmanOptimal");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        if (dictionary.getWordCount() == 1) {
            return dictionary.selectRandomWord();
        }

        WordEntropy analyser = new WordEntropy(dictionary, ConfigManager.getInstance().createGameConfig(), false);
        int dictionarySize = dictionary.getWordCount();

        String bestWord = null;
        double bestScore = Double.MAX_VALUE;

        for (String candidateWord : dictionary.getMasterSetOfWords()) {
            double expectedRemaining = calculateExpectedRemaining(analyser.getResponseBuckets(candidateWord), dictionarySize);
            if (expectedRemaining < bestScore) {
                bestScore = expectedRemaining;
                bestWord = candidateWord;
            }
        }

        return bestWord != null ? bestWord : dictionary.selectRandomWord();
    }

    private double calculateExpectedRemaining(Map<Short, Set<String>> buckets, int dictionarySize) {
        double total = 0d;
        for (Set<String> bucket : buckets.values()) {
            int bucketSize = bucket.size();
            if (bucketSize == 0) {
                continue;
            }
            double probability = (double) bucketSize / dictionarySize;
            total += probability * bucketSize;
        }
        return total;
    }
}

package com.fistraltech.bot.selection;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Selection algorithm that chooses the next guess by maximizing expected information gain (entropy).
 *
 * <p>This strategy relies on {@link WordEntropy}, which computes response buckets for candidate guesses
 * against the current dictionary and selects the word with the highest entropy.
 *
 * <p><strong>Conceptual model</strong>
 * <ul>
 *   <li>For a candidate guess, partition remaining targets into buckets by response pattern.</li>
 *   <li>Compute entropy $-\sum p \log_2 p$ over bucket probabilities.</li>
 *   <li>Select the guess with maximum entropy (most informative on average).</li>
 * </ul>
 *
 * <p><strong>Usage</strong>
 * <pre>{@code
 * Dictionary dictionary = ...;
 * SelectionAlgo algo = new SelectMaximumEntropy(dictionary);
 * String guess = algo.selectWord(new Response(""));
 * }</pre>
 *
 * <p><strong>Performance</strong>: typically the most expensive strategy; it may evaluate many candidate
 * words and bucket distributions.
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 * @see WordEntropy
 */
public class SelectMaximumEntropy extends SelectionAlgo{
    private WordEntropy wordEntropy;

    public SelectMaximumEntropy(Dictionary dictionary) {
        super(dictionary);
        this.wordEntropy = new WordEntropy(dictionary);
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        String result = wordEntropy.getMaximumEntropyWord();
        return result;
    }
}

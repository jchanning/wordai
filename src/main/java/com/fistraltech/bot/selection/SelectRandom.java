package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/**
 * Baseline selection algorithm that chooses a random word from the current set of valid candidates.
 *
 * <p>This strategy is primarily used as a control/baseline for comparison with more sophisticated
 * approaches (e.g. entropy-maximizing or frequency-based selection). It does not attempt to
 * optimize information gain; it simply samples uniformly from the filtered dictionary.
 *
 * <p><strong>Conceptual model</strong>
 * <ul>
 *   <li>{@link com.fistraltech.bot.selection.SelectionAlgo} maintains a cumulative {@link com.fistraltech.bot.filter.Filter}.</li>
 *   <li>On each call, the base class applies all constraints to produce an updated dictionary.</li>
 *   <li>This implementation selects one word at random from that updated dictionary.</li>
 * </ul>
 *
 * <p><strong>Usage</strong>
 * <pre>{@code
 * Dictionary dictionary = ...;
 * SelectionAlgo algo = new SelectRandom(dictionary);
 * String guess = algo.selectWord(new Response(""));
 * }</pre>
 *
 * <p><strong>Performance</strong>: O(1) selection once filtering has produced the candidate set.
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 */
public class SelectRandom extends SelectionAlgo {

    public SelectRandom(Dictionary dictionary){
        super(dictionary);
        setAlgoName("Random");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        return dictionary.selectRandomWord();
    }
}

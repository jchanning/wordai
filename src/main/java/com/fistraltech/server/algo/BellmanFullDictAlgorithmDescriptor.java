package com.fistraltech.server.algo;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectBellmanFullDictionary;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/**
 * Descriptor for the Bellman full-dictionary selection algorithm.
 *
 * <p>This algorithm is <em>stateful</em>: it tracks previously guessed words to avoid
 * duplicates across multiple suggestions within the same game. The registry caller
 * must cache the instance for the game session's lifetime.
 */
@Component
@Order(3)
public class BellmanFullDictAlgorithmDescriptor implements AlgorithmDescriptor {

    @Override
    public String getId() {
        return "BELLMAN_FULL_DICTIONARY";
    }

    @Override
    public String getDisplayName() {
        return "3 Expert — Bellman Optimality";
    }

    @Override
    public String getDescription() {
        return "Minimises expected guesses across the full dictionary — strongest, most thorough";
    }

    @Override
    public SelectionAlgo create(Dictionary dictionary) {
        return new SelectBellmanFullDictionary(dictionary);
    }

    @Override
    public boolean isStateful() {
        return true;
    }
}

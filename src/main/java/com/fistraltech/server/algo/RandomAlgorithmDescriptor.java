package com.fistraltech.server.algo;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/** Descriptor for the {@code RANDOM} baseline selection algorithm. */
@Component
@Order(1)
public class RandomAlgorithmDescriptor implements AlgorithmDescriptor {

    @Override
    public String getId() {
        return "RANDOM";
    }

    @Override
    public String getDisplayName() {
        return "Basic — Random";
    }

    @Override
    public String getDescription() {
        return "Picks words at random from valid candidates — weakest, no strategy";
    }

    @Override
    public SelectionAlgo create(Dictionary dictionary) {
        return new SelectRandom(dictionary);
    }

    @Override
    public boolean isStateful() {
        return false;
    }
}

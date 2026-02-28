package com.fistraltech.server.algo;

import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectRandom;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/** Descriptor for the {@code RANDOM} baseline selection algorithm. */
@Component
public class RandomAlgorithmDescriptor implements AlgorithmDescriptor {

    @Override
    public String getId() {
        return "RANDOM";
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

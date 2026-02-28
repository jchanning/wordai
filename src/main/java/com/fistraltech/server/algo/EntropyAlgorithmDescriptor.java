package com.fistraltech.server.algo;

import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/** Descriptor for the maximum-entropy selection algorithm. */
@Component
public class EntropyAlgorithmDescriptor implements AlgorithmDescriptor {

    @Override
    public String getId() {
        return "ENTROPY";
    }

    @Override
    public SelectionAlgo create(Dictionary dictionary) {
        return new SelectMaximumEntropy(dictionary);
    }

    @Override
    public boolean isStateful() {
        return false;
    }
}

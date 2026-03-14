package com.fistraltech.server.algo;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.fistraltech.bot.selection.SelectMaximumEntropy;
import com.fistraltech.bot.selection.SelectionAlgo;
import com.fistraltech.core.Dictionary;

/** Descriptor for the maximum-entropy selection algorithm. */
@Component
@Order(2)
public class EntropyAlgorithmDescriptor implements AlgorithmDescriptor {

    @Override
    public String getId() {
        return "ENTROPY";
    }

    @Override
    public String getDisplayName() {
        return "Smart — Maximum Entropy";
    }

    @Override
    public String getDescription() {
        return "Maximises information gained per guess — smart, reliable performance";
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

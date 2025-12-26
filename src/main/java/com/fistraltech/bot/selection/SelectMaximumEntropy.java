package com.fistraltech.bot.selection;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

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

package com.fistraltech.bot.selection;

import com.fistraltech.analysis.DictionaryAnalyser;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

public class SelectMaximumEntropy extends SelectionAlgo{
    public SelectMaximumEntropy(Dictionary dictionary) {
        super(dictionary);
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        DictionaryAnalyser analyser = new DictionaryAnalyser(dictionary);
        String result = analyser.getMaximumEntropyWord();
        return result;
    }
}

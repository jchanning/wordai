package com.fistraltech.bot.selection;

import com.fistraltech.analysis.DictionaryReduction;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

public class SelectMaximumDictionaryReduction extends SelectionAlgo{
    public SelectMaximumDictionaryReduction(Dictionary dictionary) {
        super(dictionary);
        setAlgoName("SelectMaximumDictionaryReduction");
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        DictionaryReduction dictionaryReduction = new DictionaryReduction(dictionary);
        return dictionaryReduction.getWordWithMaximumReduction();
    }
}

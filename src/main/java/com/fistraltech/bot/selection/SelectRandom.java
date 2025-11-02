package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

/** Random selection chooses randomly from the Dictionary of valid words.
 * It remains to be proven if a better strategy than this exists. */

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

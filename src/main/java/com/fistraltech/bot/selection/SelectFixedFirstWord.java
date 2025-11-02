package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

import java.util.Objects;

/** It is a commonly held view that there is a "best" starting word for the Wordle game.  This is clearly false as the
 * best starting word is always the correct answer.  This Selection strategy fixes the first word and then randomly
 * selects from the remaining valid words enabling the "best" starting word theory to be tested.
 *  *
 * In this strategy, the same word is used as the first guess in each game.  A first word with high coverage of the
 * vowels appears to produce good outcomes.
 * */

public class SelectFixedFirstWord extends SelectionAlgo{

    private final String firstWord;

    public SelectFixedFirstWord(Dictionary dictionary, String firstWord){
        super(dictionary);
        setAlgoName("SelectFixedFirstWord");
        this.firstWord = firstWord;
    }

     String selectWord(Response lastResponse, Dictionary dictionary)
     {
         if(Objects.equals(lastResponse.getWord(), "")) return firstWord;
         else return dictionary.selectRandomWord();
     }
}

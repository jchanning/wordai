package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

import java.util.Objects;

/**
 * Selection algorithm that forces a fixed opening guess and then falls back to random selection.
 *
 * <p>This algorithm exists to test the hypothesis that there is a "best" first word for Wordle-like games.
 * The first guess is always {@code firstWord}. All subsequent guesses are selected at random from the
 * remaining valid candidates.
 *
 * <p><strong>Usage</strong>
 * <pre>{@code
 * Dictionary dictionary = ...;
 * SelectionAlgo algo = new SelectFixedFirstWord(dictionary, "AROSE");
 * String guess1 = algo.selectWord(new Response("")); // returns AROSE
 * }</pre>
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 */

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

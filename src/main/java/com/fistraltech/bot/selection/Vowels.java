package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Experimental selection algorithm that prioritizes early vowel coverage.
 *
 * <p><strong>Current implementation</strong>:
 * <ul>
 *   <li><strong>First guess:</strong> randomly choose 3 vowels, filter the dictionary to words containing all three,
 *       then select a random word from that subset.</li>
 *   <li><strong>Subsequent guesses:</strong> currently fall back to random selection.</li>
 * </ul>
 *
 * <p>The class contains fields intended for richer vowel-tracking heuristics, but they are not yet used.
 * The Javadoc reflects the behavior as implemented (not the original design notes).
 *
 * <p><strong>Thread safety</strong>: not thread-safe; use one instance per game.
 *
 * @author Fistral Technologies
 */

public class Vowels extends SelectionAlgo {
    private int counter = 0;
    private final List<Character> vowels = new ArrayList<>();
    private int vowelsTested;
    private int vowelsFound;

    public Vowels(Dictionary dictionary){
        super(dictionary);
        setAlgoName("Vowels");
        vowels.add('a');
        vowels.add('e');
        vowels.add('i');
        vowels.add('o');
        vowels.add('u');
    }

    @Override
    String selectWord(Response lastResponse, Dictionary dictionary) {
        String result;
        if (counter == 0) {
            result = firstGuess(dictionary);
        }
        else if (counter == 1) {
            result = secondGuess(dictionary);
        }
        else if (counter == 2) {
            result = thirdGuess(dictionary);
        }
        else 
            result = dictionary.selectRandomWord();
        counter++;
        return result;
    }

    private String firstGuess(Dictionary dictionary){
        char[] selectedVowels = new char[3];
        // randomly select 3 vowels
        int index = (int)(Math.random()* vowels.size());
            selectedVowels[0] = vowels.remove(index);
            index = (int)(Math.random()* vowels.size());
            selectedVowels[1] = vowels.remove(index);
            index = (int)(Math.random()* vowels.size());
            selectedVowels[2] = vowels.remove(index);
            // Filter the dictionary to contain only words that have the three vowels
        Dictionary selection =  dictionary.getWords(selectedVowels[0]);
        selection = selection.getWords(selectedVowels[1]);
        selection = selection.getWords(selectedVowels[2]);
        vowelsTested = 3;
        return selection.selectRandomWord();
    }

    private String secondGuess(Dictionary dictionary){
        return dictionary.selectRandomWord();
    }

    private String thirdGuess(Dictionary dictionary){
        return dictionary.selectRandomWord();
    }
}

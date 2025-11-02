package com.fistraltech.bot.selection;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;

import java.util.ArrayList;
import java.util.List;

/** Human experience suggests that identifying the vowels that are in or not in the word as early as possible is
 * beneficial to a successful outcome. Testing for all 5 vowels in the first two gueses is therefore proposed as an
 * optimal strategy.
 *
 * In this selection strategy:
 * FIRST GUESS
 * 1) The first word is selected at random from those containing 3 vowels.
 * SECOND GUESS
 * 1) If the choice matches no vowels, a word is selected randomly from the words that contain the remaining two vowels
 * 2) If the choice matches one vowel, select from the words that contain the matched vowel, plus the two others.  If no
 * word exists with all three vowels, try to match one additional vowel.
 * 3) If the choice matches two vowel, proceed to random selection
 * THIRD GUESS
 * By the third guess, you should either have matched at least two vowels or begin looking to try any vowel that has not
 * been tried along with 'y'.
 * */

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

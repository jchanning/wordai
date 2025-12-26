package com.fistraltech.core;

import com.fistraltech.bot.filter.Filter;


/** Static methods that are used in a WordGame to process guesses and create responses. This has been refactored out of WordGame as it is used in selection
 * algorithms and Dictionary analysis and provides better separation of concerns. */

public class GameResponse {
    public static Response evaluate(String targetWord, String word) throws InvalidWordException {
        
        // Check word length
        if(targetWord.length() != word.length()){
            throw new InvalidWordLengthException("Invalid word, it is the wrong length");
        }   

        // Quick return for winning guess
        if(targetWord.equals(word)){
            Response response = new Response(word);
            response.setWinner(true);
            for(int i = 0; i < word.length(); ++i){
                response.setStatus(word.charAt(i), 'G');
            }
            return response;
        }

        // Statuses are Red, Amber, Green, or eXcess
        int wordLength = targetWord.length();
        char[] statusArray = new char[wordLength];
        boolean[] targetLetterUsed = new boolean[wordLength];
        char[] guessedLetters = word.toCharArray();
        char[] targetWordLetters = targetWord.toCharArray();

        // First pass: check for correct letters in the correct position
        for (int i = 0; i < wordLength; ++i) {
            if (guessedLetters[i] == targetWordLetters[i]) {
                statusArray[i] = 'G';
                targetLetterUsed[i] = true;
            } else {
                statusArray[i] = 'R';
            }
        }
        
        // Second pass: check for correct letters in the wrong position
        for (int i = 0; i < wordLength; ++i) {
            if (statusArray[i] == 'R') {
                for (int j = 0; j < wordLength; ++j) {
                    if (!targetLetterUsed[j] && guessedLetters[i] == targetWordLetters[j]) {
                        statusArray[i] = 'A';
                        targetLetterUsed[j] = true;
                        break;
                    }
                }
            }
        }
        
        // Third pass: mark excess letters as 'X' instead of 'R'
        // If a letter has Green or Amber status elsewhere, remaining Red instances should be X (excess)
        for (int i = 0; i < wordLength; ++i) {
            if (statusArray[i] == 'R') {
                // Check if this letter appears with G or A status anywhere else
                boolean hasGreenOrAmber = false;
                for (int j = 0; j < wordLength; ++j) {
                    if (guessedLetters[i] == guessedLetters[j] && (statusArray[j] == 'G' || statusArray[j] == 'A')) {
                        hasGreenOrAmber = true;
                        break;
                    }
                }
                if (hasGreenOrAmber) {
                    statusArray[i] = 'X'; // Excess instance of a letter that exists in the word
                }
            }
    }
        
    //Build the response object
    Response response = new Response(word);
    response.setWinner(false);

    for (int i = 0; i < wordLength; ++i) {
        response.setStatus(guessedLetters[i], statusArray[i]);
    }
    return response;
    }

    public static Dictionary applyFilter(Response response, Dictionary dictionary) {
        Filter filter = new Filter(dictionary.getWordLength());
        filter.update(response);
        return filter.apply(dictionary);
    }
}

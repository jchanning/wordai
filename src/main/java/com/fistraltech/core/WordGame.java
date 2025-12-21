package com.fistraltech.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.fistraltech.util.Config;

/**
 An implementation of a word guessing game where the word can be any length.

 At the start of the game a target word is chosen from the available valid words. The player has to identify the target
 word by making guesses and processing the response that is returned. The guess is tested against the target word and
 information is passed back encoded as follows:
 R = incorrect letter
 A = correct letter, wrong position
 G = correct letter in the correct position

 This uses a Red, Amber, Green traffic light concept.

 The game records each guess that has been made.

 The objective of the game is to correctly guess the target word using as few guesses as possible.  The game ends either
 when the target word is successfully guessed or the maximum number of attempts has been used.
 */

public class WordGame {
    private String targetWord;
    private char[] targetWordLetters;
    private final Dictionary allValidWords;
    private final Dictionary gameWords;
    private int wordLength;
    private final Config gameConfig;

    private final List<Response> guesses = new ArrayList<>();

    /** Create a new word game based on the dictionary provided
     * In the default case, the game words are the same as the dictionary words
    */
    public WordGame(Dictionary dictionary, Config gameConfig){
        this(dictionary, dictionary, gameConfig);
    }

    /* Creates a new word game, but with only a sub-set of the full dictionary is used as the target word.
     * The enables games of different difficulty to be played. It also enables targeted testing of the game.
     */
    public WordGame(Dictionary dictionaryOfAllValidWords, Dictionary dictionaryOfGameWords, Config gameConfig){
        this.allValidWords = dictionaryOfAllValidWords;
        this.gameWords = dictionaryOfGameWords;
        this.gameConfig = gameConfig;
    }

    public Dictionary getDictionary() {
        return allValidWords;
    }

    public List<Response> getGuesses() {
        return guesses;
    }

    @Override
    public String toString() {
        return "The target word of " + targetWord +
                " was guessed after " + getNoOfAttempts() +
                " attempts";
    }

    /** Sets the target word that needs to be guessed to win the game*/
    public void setTargetWord(String word) throws InvalidWordException {
        if(!allValidWords.contains(word)) {
            throw new InvalidWordException("Invalid target word, it is not in the dictionary");
        }
        this.targetWord = word;
        this.wordLength = word.length();
        this.targetWordLetters = targetWord.toCharArray();
    }

    /** Choose a random word from the dictionary as the target word*/
    public void setRandomTargetWord() throws InvalidWordException {
        String targetWord = gameWords.selectRandomWord();
        Logger logger = Logger.getLogger(WordGame.class.getName());
        logger.info("The target word is: " + targetWord);
        setTargetWord(targetWord);
    }

    public String getTargetWord() {
        return targetWord;
    }

    public int getNoOfAttempts(){
        return guesses.size();
    }

    public Response guess(String word) throws InvalidWordException {
        return guess(targetWord, word);
    }

    public Response evaluate(String word) throws InvalidWordException {
        char[] guessedLetters = word.toCharArray();
        boolean isWinner = word.equals(targetWord);

        // Use to track the partial matches when a letter occurs multiple times in the guess or target word
        boolean[] targetLetterUsed = new boolean[wordLength];


        // Statuses are Red, Amber, Green, or eXcess
        char[] statusArray = new char[wordLength];

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
        response.setWinner(isWinner);

        for (int i = 0; i < wordLength; ++i) {
            response.setStatus(guessedLetters[i], statusArray[i]);
        }
        return response;
    }

    /** When a guess is made, information is returned to the player */
    public Response guess(String targetWord, String word) throws InvalidWordException {
        checkWordIsValid(word);
        checkMaxAttempts();
        
        Response response = evaluate(word);

        guesses.add(response);
        return response;
    }

    /**
     * @param word
     */
    private void checkWordIsValid(String word) throws InvalidWordException {
        if(word.length() != wordLength){
            throw new InvalidWordLengthException("Invalid word, it is the wrong length");
        }
        if(!allValidWords.contains(word)){
            throw new InvalidWordException("Invalid word, it is not in the dictionary");
        }
        if(!allValidWords.contains(word)){
            throw new InvalidWordException("Invalid word, it is not in the dictionary");
        }
    }

    private void checkMaxAttempts() throws InvalidWordException {
        if(getNoOfAttempts() >= gameConfig.getMaxAttempts()){
            throw new InvalidWordException("Maximum number of attempts reached");
        }
    }
}

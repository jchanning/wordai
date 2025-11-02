package com.fistraltech.analysis;

import com.fistraltech.core.Dictionary;


/** Class to explore word complexity.
 * Complexity arises where you have correctly guessed many of the letters, but still many options remain.
 * This class tests how many words are different by a single letter or by a pair of letters.  An example would be:
 * Single letter: bound, found, hound, mound, pound, round, sound, wound
 *
 * *ound is therefore very difficult to guess correctly as so many first letters are possible.
 *
 * Similarly for two letters:
 * batch, botch, catch, latch, match, patch, hitch, bitch
 * **tch would be complex to guess correctly
 *
 * More generally, when evaluating a guess, the best guesses are:
 * 1) The correct answer.  This may seem obvious, but needs to be stated.  The probability of doing this is 1/n, where n
 * is the number of words in the dictionary.  This is currently about 2300 for Wordle.
 * 2) The solution that reduces the size of the list of correct possible answer by the most.  If the starting number of
 * letter is n0, then there are n1 words after the first guess, n2 after the second guess, etc. until n(m) = 1 (M<=6).
 * n(m)/n(n+1) therefore needs to be as small as possible.
 * */

public class ComplexityAnalyser {
    private final Dictionary dictionary;

    public ComplexityAnalyser(Dictionary dictionary){
        this.dictionary = dictionary;
    }

    /*public void setOutputFiles(String details, String summary) throws IOException{
        FileWriter detailsWriter = new FileWriter(details);
        FileWriter summaryWriter = new FileWriter(summary);
    }

    /** Calculates the number of words in the dictionary that are different from the target word by 1 letter (4 match)*/
    public int analyseOneLetterComplexity(String word){
        int counter = 0;
        for(int i=0; i<dictionary.getWordLength();++i) {
            char[] letterArray = word.toCharArray();
            letterArray[i] = '.';
            String test = new String(letterArray);
            for(String w :dictionary.getMasterSetOfWords()){
                if(w.matches(test) && !word.equals(w)){
                    counter++;
                }
            }
        }
        return counter;
    }

    /**
     * Calculates the number of words in the dictionary that are different from the target word by 2 letters (3 match).
     *
     * @param targetWord the word to compare against the dictionary words
     * @return the number of words in the dictionary that differ from the target word by exactly 2 letters
     */
    public int analyseTwoLetterComplexity(String targetWord) {
        int counter = 0;
        try {
            for (int i = 0; i < dictionary.getWordLength(); ++i) {
                for (int j = i + 1; j < dictionary.getWordLength(); ++j) {
                    char[] letterArray = targetWord.toCharArray();
                    letterArray[i] = '.';
                    letterArray[j] = '.';
                    String test = new String(letterArray);
                    for (String w : dictionary.getMasterSetOfWords()) {
                        if (w.matches(test) && !targetWord.equals(w)) {
                            counter++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counter;
    }

    /** Calculates the number of words in the dictionary that are different from the target word by 3 letters (2 match)*/
    public int analyseThreeLetterComplexity(String targetWord) {
        int counter = 0;
        try {
            for (int i = 0; i < dictionary.getWordLength(); ++i) {
                for (int j = i + 1; j < dictionary.getWordLength(); ++j) {
                    for (int k = j + 1; k < dictionary.getWordLength(); ++k) {
                        char[] letterArray = targetWord.toCharArray();
                        letterArray[i] = '.';
                        letterArray[j] = '.';
                        letterArray[k] = '.';
                        String test = new String(letterArray);
                        for (String w : dictionary.getMasterSetOfWords()) {
                            if (w.matches(test) && !targetWord.equals(w)) {
                                counter++;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return counter;
    }
}

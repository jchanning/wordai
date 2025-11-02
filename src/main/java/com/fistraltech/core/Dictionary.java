package com.fistraltech.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fistraltech.bot.filter.FilterCharacters;

/*
 class used to manage a dictionary of words for a game like Wordle.  The interface enables identification of words that
 meet the feedback criteria received when making a guess.  It allows the rapid identification of:
1) Letter match: Remove Words that do not contain the matching letter
2) Wrong position: Remove Words that contain the matching letter in the wrong position
3) Valid word: A word is a valid guess if it is contained in the dictionary

The class needs to provide the following functionality:
1) Maintain a Set of all words
2) Be able to search if a word is contained in the Dictionary
3) Find all words that contain a given letter at given position
4) Find all words with a given letter at any position other than index n
5) Find all words that do not contain a given letter
 */

public class Dictionary {
    /** The master set of all words of length wordLength*/
    private Set<String> masterSetOfWords = new HashSet<>();

    private final List<Column> columns = new ArrayList<>();
    private final int wordLength;

    /* Creates an empty Dictionary to hold words of the specified length*/
    public Dictionary(int wordLength){
        this.wordLength = wordLength;
        for(int i = 0; i < wordLength; ++i){
            columns.add(new Column());
        }
    }

    public static Dictionary createDictionary(String fileName, int wordLength) throws IOException {
        Dictionary dictionary = new Dictionary(wordLength);
        Set<String> words = WordSource.getWordsFromFile(fileName);
        dictionary.addWords(words);
        return dictionary;
    }

    public List<Column> getColumns() {
        return columns;
    }
    public int getWordLength() {
        return wordLength;
    }

    public int getWordCount(){
        return masterSetOfWords.size();
    }

    /** Returns the total count of possible letters across all positions (sum of column lengths)
     * This metric decreases as guesses eliminate possibilities, showing the filtering impact.*/
    public int getLetterCount(){
        int totalCount = 0;
        for(Column c : columns){
            totalCount += c.length();
        }
        return totalCount;
    }

    /** Returns the unique set of characters contained in the words in the dictionary */
    public Set<Character> getUniqueCharacters(){
        Set<Character> allLetters = new HashSet<>();
        for(Column c : columns){
            allLetters.addAll(c.getLetters());
        }
        return allLetters;
    }

    /** Each position in the word has a "column" of possible characters (starting with all letters of the alphabet).
     * As guesses are made the column length decreases as letters are eliminated.*/
    public List<Integer> getColumnLengths(){
        List<Integer> result = new ArrayList<>();
        for(Column c : columns){
            result.add(c.length());
        }
        return result;
    }

    public boolean contains(String word){
        return masterSetOfWords.contains(word);
    }

    /* @todo 10/09/2022 Prevent words of incorrect length being added */
    public void addWords(Set<String> words){
        this.masterSetOfWords = words;
        for(String word: words){
            if(word.length() != wordLength){
                throw new IllegalArgumentException(word + " length is incorrect");
            }
        }
        buildIndex();
    }

    public String selectRandomWord(){
        int index = (int)(Math.random()* masterSetOfWords.size());
        System.out.println("Random index:" + index);
        System.out.println("Word count:" + masterSetOfWords.size());
        String[] wordArray = masterSetOfWords.toArray(new String[0]);
        return wordArray[index];
    }

    public Set<String> getMasterSetOfWords(){
        return masterSetOfWords;
    }

    /** Get all words that have character 'c' at position i*/
    public Set<String> getWords(char c, int i){
        return columns.get(i).getWords(c);
    }

    /** Returns all words that contain the character specified at any location in the work.  By returning a Dictionary
     * calls can be chained to find all anagrams of a specified set of letters.  */
    public Dictionary getWords(char c){
        Dictionary result = new Dictionary(wordLength);
        Set<String> words = new HashSet<>();
        for(int i =0; i<wordLength;++i){
            Set<String> wordsContainingLetter = columns.get(i).getWords(c);
            if(wordsContainingLetter!=null) {
                words.addAll(wordsContainingLetter);
            }
        }
        result.addWords(words);
        return result;
    }

    /** Gets all words that have any of the characters in the array at position i*/
    public Set<String> getWords(char[] c, int i){
        Set<String> result = new HashSet<>();
        for(char ch : c) {
            Set<String> wordsContainingLetter = columns.get(i).getWords(ch);
            if(wordsContainingLetter!=null) {
                result.addAll(wordsContainingLetter);
            }
        }
        return result;
    }

    /** Gets all words that meet the filter criteria*/
    public Set<String> getWords(List<FilterCharacters> filter){
        Set<String> result = new HashSet<>();
        char[] chars;
        for(String word: masterSetOfWords){
            boolean[] includeWord = new boolean[wordLength];
            chars = word.toCharArray();
            for(int i =0; i<wordLength;++i){
                // get all valid characters for this index and test against the word in the master list
                FilterCharacters fc = filter.get(i);
                for(Character c : fc.getLetters()) {
                    if (chars[i] == c) {
                        includeWord[i] = true;
                        break;
                    }
                }
                if(!includeWord[i]){
                    // letter not found so can break from this loop
                    break;
                }

            }
            boolean allLettersFound = true;
            for (boolean b : includeWord) {
                allLettersFound = allLettersFound && b;
            }
            if(allLettersFound){
                //System.out.println("Added word:" + word);
                result.add(word);
            }
        }
        return result;
    }

    private void buildIndex(){
        for(String word: masterSetOfWords){
          char[] ch = word.toCharArray();
          for(int i=0; i< wordLength;++i){
              Column c = columns.get(i);
              c.put(ch[i], word);
          }
        }
    }

    public String toString(){
        return "Dictionary size: " + masterSetOfWords.size();
    }
}

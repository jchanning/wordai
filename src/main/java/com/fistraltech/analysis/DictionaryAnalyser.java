package com.fistraltech.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fistraltech.core.Column;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.util.Config;

/** A class used to analyse Dictionaries */
public class DictionaryAnalyser {

    private final Dictionary dictionary;

    public DictionaryAnalyser(Dictionary dictionary) {
        this.dictionary = dictionary;
    }

    /** A method used to identify the number of times each letter appears in the Dictionary across all words and in any
     * position*/
    public Map<Character, Integer> getLetterCount() {
        Map<Character,Integer> letterCount = new TreeMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        int wordLength = dictionary.getWordLength();
        for(String word: words){
            char[] ch = word.toCharArray();
            for(int i=0; i< wordLength;++i){
                if(letterCount.containsKey(ch[i])){
                    int newCount = letterCount.get(ch[i]);
                    letterCount.put(ch[i],++newCount);
                }
                else{
                    letterCount.put(ch[i],1);
                }

            }
        }
        return letterCount;
    }

    /** For each character of the alphabet, A to Z, returns the number of times that letter appears in each position
     * from 0 to n-1 for n letter words */
    public Map<Character, List<Integer>> getOccurrenceCountByPosition() {
        Map<Character, List<Integer>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        int wordLength = dictionary.getWordLength();

        for (String word : words) {
            char[] ch = word.toCharArray();
            for (int i = 0; i < wordLength; ++i) {
                if (result.containsKey(ch[i])) {
                    List<Integer> countByPosition = result.get(ch[i]);
                    int newCount = countByPosition.get(i);
                    countByPosition.set(i, ++newCount);

                } else {
                    List<Integer> countByPosition = new ArrayList<>(wordLength);
                    //Not sure why this is necessary to initialise the list, there must be a better alternative
                    for (int j = 0; j < wordLength; ++j) { countByPosition.add(0);}
                    countByPosition.set(i, 1);
                    result.put(ch[i], countByPosition);
                    }
                }
            }
        return result;
    }


    public Map<String, Set<String>> getResponseBuckets(String word) {
        Map<String, Set<String>> result = new HashMap<>();
        Set<String> words = dictionary.getMasterSetOfWords();
        
        for (String w : words) {
            try {
                Config config = new Config();
                WordGame game = new WordGame(dictionary, config);
                game.setTargetWord(w);
                Response r = game.guess(w, word);
                String bucket = r.toString();
                if (result.containsKey(bucket)) {
                    result.get(bucket).add(w);
                } else {
                    Set<String> l = new HashSet<>();
                    l.add(w);
                    result.put(bucket, l);
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
        return result;
    }


    /** Returns the word with the highest Entropy in the Dictionary */
    public String getMaximumEntropyWord(){
        float maximumEntropy = 0f;
        String result="";
        for(String word: dictionary.getMasterSetOfWords()){
            if(maximumEntropy == 0f){
                maximumEntropy = getEntropy(word);
                result = word;
            }
            else{
                float e = getEntropy(word);
                if(e>maximumEntropy){
                    maximumEntropy = e;
                    result = word;
                }
            }
        }
        return result;
    }
    /** Returns the entropy of an individual word in the Dictionary */
    public float getEntropy(String word){
        float entropy = 0f;
        int dictonarySize = dictionary.getWordCount();
        Map<String, Set<String>> buckets = getResponseBuckets(word);
        for(Map.Entry<String, Set<String>> e : buckets.entrySet()){
            int bucketCount = e.getValue().size();
            double p = ((double)bucketCount/(double)dictonarySize);
            double pe = Math.log(p)/Math.log(2);
            entropy += -(p * pe);
        }
        return entropy;
    }

    public List<Character> getMostFrequentCharByPosition(){
        List<Character> result = new ArrayList<>();
        for(Column c: dictionary.getColumns()){
            result.add(c.getMostCommonLetter());
        }
        return result;
    }

    /** Returns the least frequently occurring letter by column position (0 to n-1 in an n letter word)     */
    public List<Character> getLeastFrequentCharByPosition(){
        List<Character> result = new ArrayList<>();
        for(Column c: dictionary.getColumns()){
            result.add(c.getLeastCommonLetter());
        }
        return result;
    }
}

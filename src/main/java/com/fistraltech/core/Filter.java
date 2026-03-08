package com.fistraltech.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Encodes and applies constraints from word guess feedback to filter valid candidates.
 */
public class Filter {
    private int wordLength;
    private final List<FilterCharacters> filterArray = new ArrayList<>();
    private final Map<Character, Integer> mustContain = new HashMap<>();

    public int getWordLength() {
        return wordLength;
    }

    public Filter(int wordLength) {
        if (wordLength <= 0) {
            throw new IllegalArgumentException("wordLength must be positive");
        }
        this.wordLength = wordLength;
        for(int i =0; i<wordLength; ++i){
            filterArray.add(new FilterCharacters());
        }
    }

    public void update(Response response){
        List<ResponseEntry> status = response.getStatuses();

        Map<Character, Integer> responseLetterCounts = new HashMap<>();
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G' || re.status == 'A'){
                responseLetterCounts.put(re.letter,
                responseLetterCounts.getOrDefault(re.letter, 0) + 1);
            }
        }

        for(Map.Entry<Character, Integer> entry : responseLetterCounts.entrySet()){
            char letter = entry.getKey();
            int count = entry.getValue();
            mustContain.put(letter, Math.max(mustContain.getOrDefault(letter, 0), count));
        }

        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G'){
                filterArray.set(i, new FilterCharacters(re.letter));
            }
        }

        Map<Character, Boolean> hasNonRedStatus = new HashMap<>();
        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if(re.status == 'G' || re.status == 'A'){
                hasNonRedStatus.put(re.letter, true);
            }
        }

        for(int i=0; i<status.size(); ++i){
            ResponseEntry re = status.get(i);
            if (null != re.status)switch (re.status) {
                case 'R':
                    if(!hasNonRedStatus.containsKey(re.letter)){
                        removeLetter(re.letter);
                    }   break;
                case 'A':
                    filterArray.get(i).remove(re.letter);
                    break;
                case 'X':
                    filterArray.get(i).remove(re.letter);
                    break;
                default:
                    break;
            }
        }
    }

    public void removeLetter(char letter){
        for(FilterCharacters fc : filterArray){
            if(fc.getLetters().size() > 1) {
                fc.remove(letter);
            }
        }
    }

    public void removeLetter(char letter, int i){
        filterArray.get(i).remove(letter);
    }

    public void removeAllOtherLetters(char letter, int position){
        filterArray.set(position, new FilterCharacters(letter));
    }

    public Dictionary apply(Dictionary input){
        Set<String> filteredWords = input.getWords(filterArray);

        Set<String> result = new TreeSet<>();

        for(String word : filteredWords){
            boolean valid = true;
            for(Map.Entry<Character, Integer> entry : mustContain.entrySet()){
                char letter = entry.getKey();
                int requiredCount = entry.getValue();

                int actualCount = 0;
                for(int i = 0; i < word.length(); i++){
                    if(word.charAt(i) == letter){
                        actualCount++;
                    }
                }

                if(actualCount < requiredCount) {
                    valid = false;
                    break;
                }
            }
            if(valid)
            {
                result.add(word);
            }
        }
        Dictionary d = new Dictionary(wordLength);
            d.addWords(result);

        return d;
    }

    public void clear() {
        filterArray.clear();
        for(int i = 0; i < wordLength; ++i){
            filterArray.add(new FilterCharacters());
        }
        mustContain.clear();
    }
}
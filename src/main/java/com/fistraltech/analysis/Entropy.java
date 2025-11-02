package com.fistraltech.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Class used to calculate the Entropy of a guess.  Entropy varies depending on the content of the dictionary, however
 * the mapping between Guess to Response to Target Word remains fixed.  Computing this and serialising the result is
 * therefore useful.
 *
 * There is a one-to-one relationship between Guess and Response and a one-to-many relationship between Response and
 * possible Target words.*/

public class Entropy {
    private final Map<EntropyKey, Set<String>> entries;

    public Entropy(){
        this.entries = new HashMap<>();
    }

    public void addEntry(EntropyKey k, String word){
        if(entries.containsKey(k)){
            entries.get(k).add(word);
        }
        else{
            Set<String> words = new HashSet<>();
            words.add(word);
            entries.put(k,words);
        }
    }
}

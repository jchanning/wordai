package com.fistraltech.analysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores entropy response buckets for a guessed word and feedback pattern.
 *
 * <p>Each {@link EntropyKey} combines a guess and a response pattern, while the mapped
 * value contains all candidate target words consistent with that pair. This structure is
 * used by analysis flows that need to bucket dictionary words by response outcome before
 * computing information gain.
 */

public class Entropy {
    
    // Map of EntropyKey (Guess+Response) to set of possible target words
    private final Map<EntropyKey, Set<String>> entries;

    /**
     * Creates an empty entropy bucket store.
     */
    public Entropy(){
        this.entries = new HashMap<>();
    }

    /**
     * Adds one target word to the bucket for a guess/response pair.
     *
     * @param k entropy bucket key combining guessed word and response
     * @param word target word consistent with the key
     */
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

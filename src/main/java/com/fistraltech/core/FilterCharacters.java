package com.fistraltech.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Valid characters for a single word position within a filter.
 */
public class FilterCharacters {
    private final Set<Character> filter;

    Character[] alphabet = {'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p',
            'q','r','s','t','u','v','w','x','y','z'};

    public FilterCharacters(char c){
        filter = new HashSet<>();
        filter.add(c);
    }

    public FilterCharacters(){
        filter = new HashSet<>(Arrays.asList(alphabet));
    }

    public Set<Character> getLetters(){
        return filter;
    }

    public void remove(char c){
        filter.remove(c);
    }
}
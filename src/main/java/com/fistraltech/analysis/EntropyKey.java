package com.fistraltech.analysis;

public class EntropyKey implements Comparable<EntropyKey>{
    String guessedWord;
    String response;

    public String getCompositeKey() {
        return guessedWord +"~"+response;
    }

    @Override
    public int compareTo(EntropyKey o) {
        String t = getCompositeKey();
        String other = ((EntropyKey)o).getCompositeKey();
        return t.compareTo(other);
    }

    public boolean equals(Object o){
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return this.getCompositeKey().equals(((EntropyKey)o).getCompositeKey());
    }

    public int hashCode(){
        return getCompositeKey().hashCode();
    }
}

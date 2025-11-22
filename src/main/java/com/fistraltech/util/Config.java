package com.fistraltech.util;

public class Config {

    private int wordLength;
    private int maxAttempts = 6;
    private boolean hardMode;
    private boolean showHints;
    private String pathToDictionaryOfAllWords;
    private String pathToDictionaryOfGameWords;

    public String getPathToDictionaryOfAllWords() {
        return pathToDictionaryOfAllWords;
    }

    public void setPathToDictionaryOfAllWords(String pathToDictionaryOfAllWords) {
        this.pathToDictionaryOfAllWords = pathToDictionaryOfAllWords;
    }

    public String getPathToDictionaryOfGameWords() {
        return pathToDictionaryOfGameWords;
    }

    public void setPathToDictionaryOfGameWords(String pathToDictionaryOfGameWords) {
        this.pathToDictionaryOfGameWords = pathToDictionaryOfGameWords;
    }

    public int getWordLength() {
        return wordLength;
    }

    public void setWordLength(int wordLength) {
        this.wordLength = wordLength;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isHardMode() {
        return hardMode;
    }

    public void setHardMode(boolean hardMode) {
        this.hardMode = hardMode;
    }

    public boolean isShowHints() {
        return showHints;
    }

    public void setShowHints(boolean showHints) {
        this.showHints = showHints;
    }
}

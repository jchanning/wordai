package com.fistraltech.analysis;

/**
 * Immutable composite key used as a map key in entropy computations.
 *
 * <p>Each key represents the pair of a guessed word and the response pattern it
 * produced, allowing the {@link Entropy} class to bucket possible target words by
 * (guess, response) pair for entropy calculation.
 *
 * <p>Instances are safe to use in {@link java.util.HashMap} and {@link java.util.TreeMap}
 * because the fields are final and all equality/ordering methods are based on immutable state.
 */
public class EntropyKey implements Comparable<EntropyKey> {

    private final String guessedWord;
    private final String response;

    /**
     * Creates an {@code EntropyKey} for the given guessed word and response pattern.
     *
     * @param guessedWord the word that was guessed; must not be {@code null}
     * @param response    the response pattern produced by the game; must not be {@code null}
     * @throws IllegalArgumentException if either argument is {@code null}
     */
    public EntropyKey(String guessedWord, String response) {
        if (guessedWord == null) {
            throw new IllegalArgumentException("guessedWord must not be null");
        }
        if (response == null) {
            throw new IllegalArgumentException("response must not be null");
        }
        this.guessedWord = guessedWord;
        this.response = response;
    }

    public String getCompositeKey() {
        return guessedWord + "~" + response;
    }

    @Override
    public int compareTo(EntropyKey o) {
        return getCompositeKey().compareTo(o.getCompositeKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        return this.getCompositeKey().equals(((EntropyKey) o).getCompositeKey());
    }

    @Override
    public int hashCode() {
        return getCompositeKey().hashCode();
    }
}

package com.fistraltech.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts manual Wordle feedback into the same {@link Response} shape used by the
 * in-app evaluator.
 *
 * <p>The adapter accepts either compact status codes ({@code G}, {@code A}, {@code R},
 * {@code X}) or the corresponding Wordle color symbols. It then normalizes duplicate
 * gray feedback into {@code X} so the existing filter logic can keep applying the same
 * duplicate-letter semantics used by automated game play.
 */
public final class ManualWordleFeedbackAdapter {

    private ManualWordleFeedbackAdapter() {
    }

    /**
     * Builds a {@link Response} from a user-entered guess and the feedback pattern shown
     * by an external Wordle game.
     *
     * @param guessedWord the guessed word entered by the user
     * @param feedbackPattern the feedback symbols for each position
     * @return a Response that can be consumed by the existing filter and suggestion logic
     */
    public static Response fromWordleFeedback(String guessedWord, String feedbackPattern) {
        if (guessedWord == null || guessedWord.isBlank()) {
            throw new IllegalArgumentException("Guessed word must not be blank");
        }
        if (feedbackPattern == null || feedbackPattern.isBlank()) {
            throw new IllegalArgumentException("Feedback pattern must not be blank");
        }

        char[] guessLetters = guessedWord.toCharArray();
        char[] statuses = normalizeFeedbackPattern(feedbackPattern);

        if (statuses.length != guessLetters.length) {
            throw new IllegalArgumentException(
                "Feedback length must match guessed word length: "
                    + guessLetters.length + " != " + statuses.length);
        }

        boolean winner = true;
        for (char status : statuses) {
            if (status != 'G') {
                winner = false;
                break;
            }
        }

        normalizeDuplicateGreensAndAmbers(guessLetters, statuses);

        Response response = new Response(guessedWord);
        response.setWinner(winner);

        for (int i = 0; i < guessLetters.length; i++) {
            response.setStatus(guessLetters[i], statuses[i]);
        }

        return response;
    }

    private static char[] normalizeFeedbackPattern(String feedbackPattern) {
        List<Character> statuses = new ArrayList<>();
        feedbackPattern.codePoints()
            .filter(codePoint -> !Character.isWhitespace(codePoint))
            .forEach(codePoint -> statuses.add(normalizeFeedbackSymbol(codePoint)));

        char[] normalized = new char[statuses.size()];
        for (int i = 0; i < statuses.size(); i++) {
            normalized[i] = statuses.get(i);
        }
        return normalized;
    }

    private static char normalizeFeedbackSymbol(int codePoint) {
        return switch (codePoint) {
            case 'G', 'g' -> 'G';
            case 'A', 'a', 'Y', 'y' -> 'A';
            case 'R', 'r', 0x2B1B, 0x2B1C, 0x25FC, 0x25FB -> 'R';
            case 'X', 'x' -> 'X';
            case 0x1F7E9 -> 'G';
            case 0x1F7E8 -> 'A';
            default -> throw new IllegalArgumentException(
                "Unsupported feedback symbol: " + new String(Character.toChars(codePoint)));
        };
    }

    private static void normalizeDuplicateGreensAndAmbers(char[] guessLetters, char[] statuses) {
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i] != 'R') {
                continue;
            }

            boolean hasGreenOrAmber = false;
            for (int j = 0; j < statuses.length; j++) {
                if (guessLetters[i] == guessLetters[j] && (statuses[j] == 'G' || statuses[j] == 'A')) {
                    hasGreenOrAmber = true;
                    break;
                }
            }

            if (hasGreenOrAmber) {
                statuses[i] = 'X';
            }
        }
    }
}
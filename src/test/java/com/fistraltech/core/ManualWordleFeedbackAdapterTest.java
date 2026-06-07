package com.fistraltech.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ManualWordleFeedbackAdapter}.
 */
@DisplayName("ManualWordleFeedbackAdapter Tests")
class ManualWordleFeedbackAdapterTest {

    @Test
    @DisplayName("Color feedback converts to a winner response when all letters are green")
    void fromWordleFeedback_allGreen_marksWinner() {
        Response response = ManualWordleFeedbackAdapter.fromWordleFeedback("arose", "🟩🟩🟩🟩🟩");

        assertTrue(response.getWinner());
        assertEquals("arose", response.getWord());
        assertEquals("GGGGG", response.toString());
    }

    @Test
    @DisplayName("Duplicate gray feedback is normalized to X for filter compatibility")
    void fromWordleFeedback_duplicateGrayLetter_becomesExcess() {
        Response response = ManualWordleFeedbackAdapter.fromWordleFeedback("mucus", "⬛🟨⬛⬛⬛");

        assertEquals("RARXR", response.toString());
        assertEquals('X', response.getStatuses().get(3).status);
    }

    @Test
    @DisplayName("Gray letters stay red when no confirmed duplicate exists")
    void fromWordleFeedback_grayWithoutConfirmedDuplicate_staysRed() {
        Response response = ManualWordleFeedbackAdapter.fromWordleFeedback("llama", "⬛⬛⬛⬛⬛");

        assertEquals("RRRRR", response.toString());
    }

    @Test
    @DisplayName("Whitespace-separated compact statuses are accepted")
    void fromWordleFeedback_whitespaceSeparatedStatuses_areAccepted() {
        Response response = ManualWordleFeedbackAdapter.fromWordleFeedback("crane", "G A R R R");

        assertEquals("GARRR", response.toString());
    }

    @Test
    @DisplayName("Invalid feedback is rejected fast")
    void fromWordleFeedback_invalidInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
            () -> ManualWordleFeedbackAdapter.fromWordleFeedback("crane", "🟩🟨⬛"));
        assertThrows(IllegalArgumentException.class,
            () -> ManualWordleFeedbackAdapter.fromWordleFeedback("crane", "🟩🟨⬛⬛❌"));
    }
}
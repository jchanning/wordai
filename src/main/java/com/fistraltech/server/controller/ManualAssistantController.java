package com.fistraltech.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.analysis.DictionaryAnalytics;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.server.ManualAssistantService;
import com.fistraltech.server.dto.GameResponse;
import com.fistraltech.server.dto.ManualAssistantCreateRequest;
import com.fistraltech.server.dto.ManualAssistantCreateResponse;
import com.fistraltech.server.dto.ManualAssistantFeedbackRequest;
import com.fistraltech.server.dto.ManualAssistantFeedbackResponse;
import com.fistraltech.server.dto.ManualAssistantStrategyRequest;
import com.fistraltech.server.dto.ManualAssistantSuggestionResponse;
import com.fistraltech.server.model.ManualAssistantSession;
import com.fistraltech.web.ApiErrors;
import com.fistraltech.web.ApiResourceNotFoundException;

import jakarta.validation.Valid;

/**
 * REST controller for target-free manual Wordle assistant sessions.
 *
 * <p><strong>Base path</strong>: {@code /api/v1/wordai/assistant}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Sessions</strong>: create/delete assistant sessions and choose strategy</li>
 *   <li><strong>Feedback</strong>: submit guessed word + external color feedback</li>
 *   <li><strong>Suggestion</strong>: request the next recommended guess</li>
 * </ul>
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/assistant", ApiRoutes.V1_ROOT + "/assistant"})
public class ManualAssistantController {

    private static final Logger logger = Logger.getLogger(ManualAssistantController.class.getName());
    private static final String INTERNAL_SERVER_ERROR_TITLE = "Internal server error";

    private final ManualAssistantService manualAssistantService;

    public ManualAssistantController(ManualAssistantService manualAssistantService) {
        this.manualAssistantService = manualAssistantService;
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(@RequestBody(required = false) ManualAssistantCreateRequest request) {
        try {
            String dictionaryId = request != null ? request.getDictionaryId() : null;
            String strategy = request != null ? request.getStrategy() : null;

            ManualAssistantSession session = manualAssistantService.createSession(dictionaryId, strategy);
            ManualAssistantCreateResponse response = new ManualAssistantCreateResponse(
                session.getSessionId(),
                session.getDictionaryId(),
                session.getWordLength(),
                session.getSelectedStrategy(),
                session.getRemainingWordsCount());
            response.setDictionaryMetrics(buildDictionaryMetrics(session.getFilteredDictionary()));

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (InvalidWordException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiErrors.response(HttpStatus.FORBIDDEN, "Algorithm disabled", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating manual assistant session", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE, "Failed to create assistant session");
        }
    }

    @PostMapping("/sessions/{sessionId}/feedback")
    public ResponseEntity<?> submitFeedback(@PathVariable String sessionId,
                                            @Valid @RequestBody ManualAssistantFeedbackRequest request) {
        try {
            Response response = manualAssistantService.submitFeedback(
                sessionId,
                request.getGuessedWord(),
                request.getFeedbackPattern());

            ManualAssistantSession session = manualAssistantService.getSession(sessionId);
            ManualAssistantFeedbackResponse feedbackResponse = new ManualAssistantFeedbackResponse(
                sessionId,
                response.getWord(),
                response.toString(),
                session.getAttemptCount(),
                session.getRemainingWordsCount(),
                response.getWinner());
            feedbackResponse.setDictionaryMetrics(buildDictionaryMetrics(session.getFilteredDictionary()));

            return ResponseEntity.ok(feedbackResponse);
        } catch (InvalidWordException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (ApiResourceNotFoundException e) {
            return ApiErrors.response(HttpStatus.NOT_FOUND, e.getError(), e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Unexpected error applying manual feedback for session " + sessionId);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE, "Failed to apply feedback");
        }
    }

    @GetMapping("/sessions/{sessionId}/suggestion")
    public ResponseEntity<?> getSuggestion(@PathVariable String sessionId) {
        try {
            ManualAssistantSession session = manualAssistantService.getSession(sessionId);
            String suggestion = manualAssistantService.suggestWord(sessionId);

            ManualAssistantSuggestionResponse response = new ManualAssistantSuggestionResponse(
                sessionId,
                suggestion,
                session.getSelectedStrategy(),
                session.getRemainingWordsCount());

            return ResponseEntity.ok(response);
        } catch (ApiResourceNotFoundException e) {
            return ApiErrors.response(HttpStatus.NOT_FOUND, e.getError(), e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Unexpected error getting assistant suggestion for session " + sessionId);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE, "Failed to get suggestion");
        }
    }

    @PutMapping("/sessions/{sessionId}/strategy")
    public ResponseEntity<?> setStrategy(@PathVariable String sessionId,
                                         @Valid @RequestBody ManualAssistantStrategyRequest request) {
        try {
            String selected = manualAssistantService.setStrategy(sessionId, request.getStrategy());
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("strategy", selected);
            response.put("message", "Strategy updated successfully");
            return ResponseEntity.ok(response);
        } catch (ApiResourceNotFoundException e) {
            return ApiErrors.response(HttpStatus.NOT_FOUND, e.getError(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return ApiErrors.response(HttpStatus.FORBIDDEN, "Algorithm disabled", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Unexpected error updating assistant strategy for session " + sessionId);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE, "Failed to update strategy");
        }
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        try {
            manualAssistantService.getSession(sessionId);
            manualAssistantService.deleteSession(sessionId);
            Map<String, String> response = new HashMap<>();
            response.put("sessionId", sessionId);
            response.put("message", "Assistant session deleted successfully");
            return ResponseEntity.ok(response);
        } catch (ApiResourceNotFoundException e) {
            return ApiErrors.response(HttpStatus.NOT_FOUND, e.getError(), e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, e, () -> "Unexpected error deleting assistant session " + sessionId);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                INTERNAL_SERVER_ERROR_TITLE, "Failed to delete assistant session");
        }
    }

    private GameResponse.DictionaryMetrics buildDictionaryMetrics(Dictionary dictionary) {
        DictionaryAnalytics analyser = new DictionaryAnalytics(dictionary);
        GameResponse.DictionaryMetrics metrics = new GameResponse.DictionaryMetrics(
            dictionary.getLetterCount(),
            dictionary.getUniqueCharacters().size(),
            dictionary.getColumnLengths());
        metrics.setOccurrenceCountByPosition(analyser.getOccurrenceCountByPosition());
        metrics.setMostFrequentCharByPosition(analyser.getMostFrequentCharByPosition());
        return metrics;
    }
}
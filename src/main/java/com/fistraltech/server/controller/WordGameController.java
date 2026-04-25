package com.fistraltech.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.server.AlgorithmFeatureService;
import com.fistraltech.server.GameHistoryService;
import com.fistraltech.server.GameResponseShaper;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.CreateGameRequest;
import com.fistraltech.server.dto.GuessRequest;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.web.ApiErrors;
import com.fistraltech.web.ApiResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * REST controller for the WordAI HTTP API.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Games</strong>: create/delete sessions, make guesses, get suggestions</li>
 *   <li><strong>Service health</strong>: verify the API is running and report active-session count</li>
 * </ul>
 *
 * <p><strong>Typical flow</strong>
 * <ol>
 *   <li>Create a game: {@code POST /games}</li>
 *   <li>Make guesses: {@code POST /games/{gameId}/guess}</li>
 *   <li>Request suggestions: {@code GET /games/{gameId}/suggestion}</li>
 *   <li>Clean up: {@code DELETE /games/{gameId}}</li>
 * </ol>
 *
 * <p><strong>Example: create game</strong>
 * <pre>{@code
 * POST /api/wordai/games
 * Content-Type: application/json
 *
 * {
 *   "dictionaryId": "5",
 *   "wordLength": 5,
 *   "targetWord": null
 * }
 * }</pre>
 *
 * <p><strong>Example: make guess</strong>
 * <pre>{@code
 * POST /api/wordai/games/{gameId}/guess
 * Content-Type: application/json
 *
 * { "word": "CRANE" }
 * }</pre>
 *
 * <p><strong>Error shape</strong> (most endpoints)
 * <pre>{@code
 * {
 *   "error": "Invalid request",
 *   "message": "Word is required"
 * }
 * }</pre>
 *
 * @author Fistral Technologies
 * @see com.fistraltech.server.WordGameService
 */
@RestController
@RequestMapping({ApiRoutes.LEGACY_ROOT, ApiRoutes.V1_ROOT})
public class WordGameController {
    
    private static final Logger logger = Logger.getLogger(WordGameController.class.getName());

    private final WordGameService gameService;
    private final AlgorithmFeatureService algorithmFeatureService;
    private final GameHistoryService gameHistoryService;
    private final GameResponseShaper responseShaper;

    public WordGameController(WordGameService gameService,
                              AlgorithmFeatureService algorithmFeatureService,
                              GameHistoryService gameHistoryService,
                              GameResponseShaper responseShaper) {
        this.gameService = gameService;
        this.algorithmFeatureService = algorithmFeatureService;
        this.gameHistoryService = gameHistoryService;
        this.responseShaper = responseShaper;
    }

    /**
     * Health check endpoint
     * GET /api/wordai/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "WordAI Game Server");
        response.put("activeSessions", gameService.getActiveSessionCount());
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a new game session
     * POST /api/wordai/games
     */
    @PostMapping("/games")
    public ResponseEntity<?> createGame(@Valid @RequestBody(required = false) CreateGameRequest request,
            Authentication authentication) {
        try {
            String targetWord = null;
            Integer wordLength = null;
            String dictionaryId = null;

            if (request != null) {
                targetWord = request.getTargetWord();
                wordLength = request.getWordLength();
                dictionaryId = request.getDictionaryId();
            }

            Long userId = gameHistoryService.resolveUser(authentication).map(u -> u.getId()).orElse(null);
            String browserSessionId = request != null ? request.getBrowserSessionId() : null;
        boolean resumeExisting = request != null && Boolean.TRUE.equals(request.getResumeExisting());
            String gameId = gameService.createGame(targetWord, wordLength, dictionaryId, userId,
            browserSessionId, resumeExisting);
            GameSession session = gameService.getGameSession(gameId);
            var response = responseShaper.buildCreateGameResponse(session);
            
            logger.log(Level.INFO, "Game created: {0}", gameId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (InvalidWordException e) {
            logger.log(Level.WARNING, "Failed to create game: {0}", e.getMessage());
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating game", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to create game");
        }
    }
    
    /**
     * Make a guess for a specific game
     * POST /api/wordai/games/{gameId}/guess
     */
    @PostMapping("/games/{gameId}/guess")
    public ResponseEntity<?> makeGuess(@PathVariable String gameId,
            @Valid @RequestBody GuessRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Response gameResponse = gameService.makeGuess(gameId, request.getWord().trim());
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            var response = responseShaper.buildGameResponse(gameId, gameResponse, session);
            
            // Persist the completed game for either the authenticated user or an anonymous IP.
            gameHistoryService.saveIfEnded(session, authentication, getClientIpAddress(httpRequest));

            logger.info("Guess made for game " + gameId + ": " + request.getWord());
            return ResponseEntity.ok(response);
            
        } catch (InvalidWordException e) {
            logger.warning("Invalid guess for game " + gameId + ": " + e.getMessage());
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid word", e.getMessage());
        } catch (ApiResourceNotFoundException e) {
            return ApiErrors.response(HttpStatus.NOT_FOUND, e.getError(), e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing guess for game " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to process guess");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
    
    /**
     * Get the current state of a game
     * GET /api/wordai/games/{gameId}
     */
    @GetMapping("/games/{gameId}")
    public ResponseEntity<?> getGameState(@PathVariable String gameId) {
        try {
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            
            Map<String, Object> gameState = new HashMap<>();
            gameState.put("gameId", gameId);
            gameState.put("attempts", session.getCurrentAttempts());
            gameState.put("maxAttempts", session.getMaxAttempts());
            gameState.put("gameEnded", session.isGameEnded());
            gameState.put("wordLength", session.getWordGame().getDictionary().getWordLength());
            
            // Include target word if game has ended
            if (session.isGameEnded()) {
                gameState.put("targetWord", session.getWordGame().getTargetWord());
            }
            
            // Add guess history
            gameState.put("guesses", session.getWordGame().getGuesses());
            
            return ResponseEntity.ok(gameState);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error getting game state for " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to get game state");
        }
    }
    
    /**
     * Delete a game session
     * DELETE /api/wordai/games/{gameId}
     */
    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<?> deleteGame(@PathVariable String gameId) {
        try {
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            
            gameService.removeGameSession(gameId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Game session deleted successfully");
            response.put("gameId", gameId);
            
            logger.info("Game deleted: " + gameId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error deleting game " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to delete game");
        }
    }
    
    /**
     * Get the remaining valid words in the dictionary
     * GET /api/wordai/games/{gameId}/words
     */
    @GetMapping("/games/{gameId}/words")
    public ResponseEntity<?> getDictionaryWords(@PathVariable String gameId) {
        try {
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            
            // Get the filtered dictionary based on current game state
            Dictionary filteredDictionary = session.getWordFilter().apply(session.getWordGame().getDictionary());
            List<String> words = new ArrayList<>(filteredDictionary.getMasterSetOfWords());
            words.sort(String::compareTo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("gameId", gameId);
            response.put("words", words);
            response.put("count", words.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error getting dictionary words for game " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to get dictionary words");
        }
    }
    
    /**
     * Get a word suggestion for the current game state
     * GET /api/wordai/games/{gameId}/suggestion
     */
    @GetMapping("/games/{gameId}/suggestion")
    public ResponseEntity<?> getSuggestion(@PathVariable String gameId) {
        try {
            long startTime = System.currentTimeMillis();
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            
            String suggestion = session.suggestWord();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("gameId", gameId);
            response.put("suggestion", suggestion);
            response.put("strategy", session.getSelectedStrategy());
            response.put("remainingWords", session.getRemainingWordsCount());
            
            logger.info(String.format("Suggestion for game %s: '%s' using %s strategy (remaining=%d, %dms)", 
                       gameId, suggestion, session.getSelectedStrategy(), 
                       session.getRemainingWordsCount(), duration));
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error getting suggestion for game " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to get suggestion");
        }
    }
    
    /**
     * Set the strategy for word suggestions
     * PUT /api/wordai/games/{gameId}/strategy
     */
    @PutMapping("/games/{gameId}/strategy")
    public ResponseEntity<?> setStrategy(@PathVariable String gameId, @RequestBody Map<String, String> request) {
        try {
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                return ApiErrors.response(HttpStatus.NOT_FOUND,
                        "Game not found", "Game session " + gameId + " does not exist");
            }
            
            String strategy = request.get("strategy");
            if (strategy == null || strategy.trim().isEmpty()) {
                return ApiErrors.response(HttpStatus.BAD_REQUEST,
                        "Invalid request", "Strategy is required");
            }
            
            // Validate that the algorithm is enabled
            if (!algorithmFeatureService.isAlgorithmEnabled(strategy)) {
                logger.warning("Attempt to use disabled algorithm '" + strategy + "' for game " + gameId);
                return ApiErrors.response(HttpStatus.FORBIDDEN,
                        "Algorithm disabled", "Algorithm '" + strategy + "' is not enabled on this server");
            }
            
            session.setSelectedStrategy(strategy);
            
            Map<String, String> response = new HashMap<>();
            response.put("gameId", gameId);
            response.put("strategy", session.getSelectedStrategy());
            response.put("message", "Strategy updated successfully");
            
            logger.info("Strategy for game " + gameId + " set to: " + strategy);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error setting strategy for game " + gameId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to set strategy");
        }
    }
    
}
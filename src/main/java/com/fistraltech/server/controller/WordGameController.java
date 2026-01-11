package com.fistraltech.server.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.analysis.WordEntropy;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.server.DictionaryService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.CreateGameRequest;
import com.fistraltech.server.dto.CreateGameResponse;
import com.fistraltech.server.dto.DictionaryOption;
import com.fistraltech.server.dto.GameResponse;
import com.fistraltech.server.dto.GuessRequest;
import com.fistraltech.server.model.GameSession;

/**
 * REST controller for the WordAI HTTP API.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai}
 *
 * <p><strong>Primary resources</strong>
 * <ul>
 *   <li><strong>Games</strong>: create/delete sessions, make guesses, get suggestions</li>
 *   <li><strong>Dictionaries</strong>: list dictionary options and (optionally) fetch full word lists</li>
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
@RequestMapping("/api/wordai")
@CrossOrigin(origins = "*") // Enable CORS for web frontend
public class WordGameController {
    
    private static final Logger logger = Logger.getLogger(WordGameController.class.getName());
    
    @Autowired
    private WordGameService gameService;
    
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
     * Get available dictionaries
     * GET /api/wordai/dictionaries
     */
    @GetMapping("/dictionaries")
    public ResponseEntity<List<DictionaryOption>> getDictionaries() {
        try {
            DictionaryService dictionaryService = gameService.getDictionaryService();
            List<DictionaryOption> dictionaries = dictionaryService.getAvailableDictionaries();
            return ResponseEntity.ok(dictionaries);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting dictionaries: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get dictionary words with entropy values
     * GET /api/wordai/dictionaries/{dictionaryId}
     */
    @GetMapping("/dictionaries/{dictionaryId}")
    public ResponseEntity<Map<String, Object>> getDictionary(@PathVariable String dictionaryId) {
        try {
            DictionaryService dictionaryService = gameService.getDictionaryService();
            Dictionary dictionary = dictionaryService.getMasterDictionary(dictionaryId);
            
            if (dictionary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            // Get pre-computed entropy values from DictionaryService
            WordEntropy wordEntropy = dictionaryService.getWordEntropy(dictionaryId);
            
            logger.info("Dictionary ID: " + dictionaryId + ", Word Length: " + dictionary.getWordLength() + 
                       ", WordEntropy found: " + (wordEntropy != null));
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", dictionaryId);
            response.put("wordLength", dictionary.getWordLength());
            response.put("wordCount", dictionary.getWordCount());
            response.put("words", new ArrayList<>(dictionary.getMasterSetOfWords()));
            
            // Add entropy values if available
            if (wordEntropy != null) {
                Map<String, Float> entropyMap = new HashMap<>();
                for (String word : dictionary.getMasterSetOfWords()) {
                    float entropy = wordEntropy.getEntropy(word);
                    entropyMap.put(word, entropy);
                }
                logger.info("Added entropy values for " + entropyMap.size() + " words");
                response.put("entropy", entropyMap);
            } else {
                logger.warning("WordEntropy is null for dictionary: " + dictionaryId);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading dictionary: {0}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    /**
     * Create a new game session
     * POST /api/wordai/games
     */
    @PostMapping("/games")
    public ResponseEntity<?> createGame(@RequestBody(required = false) CreateGameRequest request) {
        try {
            String targetWord = null;
            Integer wordLength = null;
            String dictionaryId = null;
            
            if (request != null) {
                targetWord = request.getTargetWord();
                wordLength = request.getWordLength();
                dictionaryId = request.getDictionaryId();
            }
            
            String gameId = gameService.createGame(targetWord, wordLength, dictionaryId);
            GameSession session = gameService.getGameSession(gameId);
            
            CreateGameResponse response = new CreateGameResponse(
                gameId,
                session.getWordGame().getDictionary().getWordLength(),
                session.getMaxAttempts()
            );
            
            // Add initial dictionary metrics
            com.fistraltech.core.Dictionary filteredDict = session.getFilteredDictionary();
            com.fistraltech.analysis.DictionaryAnalytics analyser = new com.fistraltech.analysis.DictionaryAnalytics(filteredDict);
            
            CreateGameResponse.DictionaryMetrics metrics = new CreateGameResponse.DictionaryMetrics(
                filteredDict.getWordCount(),
                filteredDict.getLetterCount(),
                filteredDict.getUniqueCharacters().size(),
                filteredDict.getColumnLengths()
            );
            metrics.setOccurrenceCountByPosition(analyser.getOccurrenceCountByPosition());
            metrics.setMostFrequentCharByPosition(analyser.getMostFrequentCharByPosition());
            response.setDictionaryMetrics(metrics);
            
            logger.log(Level.INFO, "Game created: {0}", gameId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (InvalidWordException e) {
            logger.log(Level.WARNING, "Failed to create game: {0}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid request");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating game: {0}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to create game");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Make a guess for a specific game
     * POST /api/wordai/games/{gameId}/guess
     */
    @PostMapping("/games/{gameId}/guess")
    public ResponseEntity<?> makeGuess(@PathVariable String gameId, @RequestBody GuessRequest request) {
        try {
            if (request.getWord() == null || request.getWord().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid request");
                error.put("message", "Word is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            Response gameResponse = gameService.makeGuess(gameId, request.getWord().trim());
            GameSession session = gameService.getGameSession(gameId);
            
            if (session == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
            }
            
            GameResponse response = new GameResponse(
                gameId,
                gameResponse,
                session.getCurrentAttempts(),
                session.getMaxAttempts()
            );
            
            // Add dictionary metrics
            com.fistraltech.core.Dictionary filteredDict = session.getFilteredDictionary();
            com.fistraltech.analysis.DictionaryAnalytics analyser = new com.fistraltech.analysis.DictionaryAnalytics(filteredDict);
            
            GameResponse.DictionaryMetrics metrics = new GameResponse.DictionaryMetrics(
                filteredDict.getLetterCount(),
                filteredDict.getUniqueCharacters().size(),
                filteredDict.getColumnLengths()
            );
            metrics.setOccurrenceCountByPosition(analyser.getOccurrenceCountByPosition());
            metrics.setMostFrequentCharByPosition(analyser.getMostFrequentCharByPosition());
            response.setDictionaryMetrics(metrics);
            
            logger.info("Guess made for game " + gameId + ": " + request.getWord());
            return ResponseEntity.ok(response);
            
        } catch (InvalidWordException e) {
            logger.warning("Invalid guess for game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid word");
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.severe("Unexpected error processing guess for game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to process guess");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
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
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
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
            logger.severe("Unexpected error getting game state for " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to get game state");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
            }
            
            gameService.removeGameSession(gameId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Game session deleted successfully");
            response.put("gameId", gameId);
            
            logger.info("Game deleted: " + gameId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Unexpected error deleting game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to delete game");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
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
            logger.severe("Unexpected error getting dictionary words for game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to get dictionary words");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
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
            logger.severe("Unexpected error getting suggestion for game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to get suggestion");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                Map<String, String> error = new HashMap<>();
                error.put("error", "Game not found");
                error.put("message", "Game session " + gameId + " does not exist");
                return ResponseEntity.notFound().build();
            }
            
            String strategy = request.get("strategy");
            if (strategy == null || strategy.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid request");
                error.put("message", "Strategy is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            session.setSelectedStrategy(strategy);
            
            Map<String, String> response = new HashMap<>();
            response.put("gameId", gameId);
            response.put("strategy", session.getSelectedStrategy());
            response.put("message", "Strategy updated successfully");
            
            logger.info("Strategy for game " + gameId + " set to: " + strategy);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.severe("Unexpected error setting strategy for game " + gameId + ": " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            error.put("message", "Failed to set strategy");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Run full dictionary analysis with specified algorithm
     * POST /api/wordai/analysis
     */
    @PostMapping("/analysis")
    public ResponseEntity<?> runAnalysis(@RequestBody com.fistraltech.server.dto.AnalysisRequest request) {
        try {
            logger.info("Starting analysis with algorithm: " + request.getAlgorithm() + 
                       ", dictionary: " + request.getDictionaryId());
            
            com.fistraltech.server.dto.AnalysisResponse response = 
                gameService.runAnalysis(request.getAlgorithm(), request.getDictionaryId(), request.getMaxGames());
            
            logger.info("Analysis completed: " + response.getTotalGames() + " games, " + 
                       response.getWinRate() + "% win rate");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error running analysis: {0}", e.getMessage());
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Analysis failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * Get available selection algorithms
     * GET /api/wordai/algorithms
     */
    @GetMapping("/algorithms")
    public ResponseEntity<List<Map<String, String>>> getAlgorithms() {
        List<Map<String, String>> algorithms = new ArrayList<>();
        
        algorithms.add(createAlgorithmInfo("RANDOM", "Random Selection", 
            "Selects words randomly from valid options"));
        algorithms.add(createAlgorithmInfo("ENTROPY", "Maximum Entropy", 
            "Chooses words that maximize information gain"));
        algorithms.add(createAlgorithmInfo("MOST_COMMON_LETTERS", "Most Common Letters", 
            "Selects words with most frequently occurring letters"));
        algorithms.add(createAlgorithmInfo("MINIMISE_COLUMN_LENGTHS", "Minimise Column Lengths", 
            "Reduces possible letters at each position"));
        algorithms.add(createAlgorithmInfo("DICTIONARY_REDUCTION", "Dictionary Reduction", 
            "Maximizes expected reduction in remaining possibilities"));
        
        return ResponseEntity.ok(algorithms);
    }
    
    private Map<String, String> createAlgorithmInfo(String id, String name, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("description", description);
        return info;
    }
}
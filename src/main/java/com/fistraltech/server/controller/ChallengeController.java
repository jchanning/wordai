package com.fistraltech.server.controller;

import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.server.ChallengeService;
import com.fistraltech.server.GameHistoryService;
import com.fistraltech.server.dto.ChallengeAssistRequest;
import com.fistraltech.server.dto.ChallengeLeaderboardResponse;
import com.fistraltech.server.dto.ChallengeStateResponse;
import com.fistraltech.server.dto.CreateChallengeRequest;
import com.fistraltech.server.dto.GuessRequest;
import com.fistraltech.server.model.ChallengeResultEntity;
import com.fistraltech.server.model.ChallengeSession;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.web.ApiErrors;

import jakarta.validation.Valid;

/**
 * REST controller for Challenge Mode endpoints.
 *
 * <p><strong>Base path</strong>: {@code /api/wordai/challenges}
 *
 * <p><strong>Resources</strong>
 * <ul>
 *   <li>Create and read challenge sessions</li>
 *   <li>Submit guesses for the active challenge puzzle</li>
 *   <li>Consume AI assists, pause, and skip actions</li>
 *   <li>Read the completed challenge leaderboard</li>
 * </ul>
 */
@RestController
@CrossOrigin
@RequestMapping({ApiRoutes.LEGACY_ROOT + "/challenges", ApiRoutes.V1_ROOT + "/challenges"})
public class ChallengeController {
    private static final Logger logger = Logger.getLogger(ChallengeController.class.getName());

    private final ChallengeService challengeService;
    private final GameHistoryService gameHistoryService;
    private final Clock clock;

    @Autowired
    public ChallengeController(ChallengeService challengeService, GameHistoryService gameHistoryService) {
        this(challengeService, gameHistoryService, Clock.systemUTC());
    }

    ChallengeController(ChallengeService challengeService, GameHistoryService gameHistoryService, Clock clock) {
        this.challengeService = challengeService;
        this.gameHistoryService = gameHistoryService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<?> createChallenge(@Valid @RequestBody(required = false) CreateChallengeRequest request,
            Authentication authentication) {
        try {
            Long userId = gameHistoryService.resolveUser(authentication).map(user -> user.getId()).orElse(null);
            String dictionaryId = request != null ? request.getDictionaryId() : null;
            Integer wordLength = request != null ? request.getWordLength() : null;
            String browserSessionId = request != null ? request.getBrowserSessionId() : null;

            ChallengeSession challenge = challengeService.startChallenge(dictionaryId, wordLength, userId,
                    browserSessionId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(toStateResponse(challenge, null, null, "Challenge created."));
        } catch (InvalidWordException e) {
            logger.log(Level.WARNING, "Failed to create challenge: {0}", e.getMessage());
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error creating challenge", e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to create challenge");
        }
    }

    @GetMapping("/{challengeId}")
    public ResponseEntity<?> getChallenge(@PathVariable String challengeId) {
        ChallengeSession challenge = challengeService.getChallenge(challengeId);
        if (challenge == null) {
            return ApiErrors.response(HttpStatus.NOT_FOUND,
                "Challenge not found", "Challenge session " + challengeId + " does not exist");
        }
        return ResponseEntity.ok(toStateResponse(challenge, null, null, null));
    }

    @PostMapping("/{challengeId}/guess")
    public ResponseEntity<?> makeGuess(@PathVariable String challengeId,
            @Valid @RequestBody GuessRequest request) {
        try {
            ChallengeService.ChallengeActionResult result = challengeService.makeGuess(challengeId,
                    request.getWord().trim());
            return ResponseEntity.ok(toStateResponse(result.getChallengeSession(), result.getGuessResponse(),
                    result.getSuggestedWord(), result.getMessage()));
        } catch (InvalidWordException e) {
            logger.log(Level.WARNING, "Failed challenge guess for {0}: {1}", new Object[] { challengeId, e.getMessage() });
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid word", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing challenge guess " + challengeId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to process challenge guess");
        }
    }

    @PostMapping("/{challengeId}/assist")
    public ResponseEntity<?> useAssist(@PathVariable String challengeId,
            @Valid @RequestBody(required = false) ChallengeAssistRequest request) {
        try {
            String strategy = request != null ? request.getStrategy() : null;
            ChallengeService.ChallengeActionResult result = challengeService.useAssist(challengeId, strategy);
            return ResponseEntity.ok(toStateResponse(result.getChallengeSession(), null, result.getSuggestedWord(),
                    result.getMessage()));
        } catch (InvalidWordException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing challenge assist " + challengeId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to process challenge assist");
        }
    }

    @PostMapping("/{challengeId}/pause")
    public ResponseEntity<?> pauseChallenge(@PathVariable String challengeId) {
        try {
            ChallengeService.ChallengeActionResult result = challengeService.pauseChallenge(challengeId);
            return ResponseEntity.ok(toStateResponse(result.getChallengeSession(), null, null, result.getMessage()));
        } catch (InvalidWordException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error pausing challenge " + challengeId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to pause challenge");
        }
    }

    @PostMapping("/{challengeId}/skip")
    public ResponseEntity<?> skipPuzzle(@PathVariable String challengeId) {
        try {
            ChallengeService.ChallengeActionResult result = challengeService.skipPuzzle(challengeId);
            return ResponseEntity.ok(toStateResponse(result.getChallengeSession(), null, null, result.getMessage()));
        } catch (InvalidWordException e) {
            return ApiErrors.response(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error skipping challenge puzzle " + challengeId, e);
            return ApiErrors.response(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Internal server error", "Failed to skip puzzle");
        }
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ChallengeLeaderboardResponse> getLeaderboard() {
        List<ChallengeLeaderboardResponse.Entry> entries = challengeService.getLeaderboard(20).stream()
                .map(this::toLeaderboardEntry)
                .toList();
        return ResponseEntity.ok(new ChallengeLeaderboardResponse(entries));
    }

    private ChallengeLeaderboardResponse.Entry toLeaderboardEntry(ChallengeResultEntity entity) {
        ChallengeLeaderboardResponse.Entry entry = new ChallengeLeaderboardResponse.Entry();
        entry.setChallengeId(entity.getChallengeId());
        entry.setUsername(entity.getUsernameSnapshot());
        entry.setTotalScore(entity.getTotalScore());
        entry.setPuzzlesCompleted(entity.getPuzzlesCompleted());
        entry.setStatus(entity.getStatus());
        entry.setCompletedAt(entity.getCompletedAt().toString());
        return entry;
    }

    private ChallengeStateResponse toStateResponse(ChallengeSession challenge, Response guessResponse,
            String suggestedWord, String message) {
        ChallengeStateResponse response = new ChallengeStateResponse();
        response.setChallengeId(challenge.getChallengeId());
        response.setDictionaryId(challenge.getDictionaryId());
        response.setStatus(challenge.getStatus());
        response.setTotalScore(challenge.getTotalScore());
        response.setTotalPuzzles(ChallengeSession.TOTAL_PUZZLES);
        response.setCurrentPuzzleNumber(challenge.getCurrentPuzzleNumber());
        response.setPuzzlesCompleted(challenge.getPuzzlesCompleted());
        response.setCurrentPuzzleTimeLimitSeconds(challenge.getCurrentPuzzleTimeLimitSeconds());
        response.setSecondsRemaining(challenge.getSecondsRemaining(clock.instant()));
        response.setCurrentPuzzleAssistsRemaining(challenge.getCurrentPuzzleAssistsRemaining());
        response.setPauseUsed(challenge.isPauseUsed());
        response.setSkipUsed(challenge.isSkipUsed());
        response.setChallengeComplete(challenge.isChallengeComplete());
        response.setChallengeFailed(challenge.isChallengeFailed());
        response.setSuggestedWord(suggestedWord);
        response.setMessage(message);
        response.setCompletedPuzzles(challenge.getCompletedPuzzles().stream()
                .map(this::toPuzzleSummary)
                .toList());

        GameSession currentPuzzle = challenge.getCurrentPuzzleSession();
        if (currentPuzzle != null) {
            response.setCurrentAttempts(currentPuzzle.getCurrentAttempts());
            response.setMaxAttempts(currentPuzzle.getMaxAttempts());
        }

        if (!challenge.getCompletedPuzzles().isEmpty() && (challenge.isChallengeComplete() || challenge.isChallengeFailed())) {
            ChallengeSession.PuzzleSummary lastPuzzle = challenge.getCompletedPuzzles()
                    .get(challenge.getCompletedPuzzles().size() - 1);
            response.setRevealedTargetWord(lastPuzzle.getTargetWord());
        }

        if (guessResponse != null) {
            response.setLastGuess(toLastGuess(guessResponse, currentPuzzle));
        }
        return response;
    }

    private ChallengeStateResponse.PuzzleSummary toPuzzleSummary(ChallengeSession.PuzzleSummary summary) {
        ChallengeStateResponse.PuzzleSummary dto = new ChallengeStateResponse.PuzzleSummary();
        dto.setPuzzleNumber(summary.getPuzzleNumber());
        dto.setTargetWord(summary.getTargetWord());
        dto.setStatus(summary.getStatus());
        dto.setScoreAwarded(summary.getScoreAwarded());
        dto.setAttemptsUsed(summary.getAttemptsUsed());
        dto.setMaxAttempts(summary.getMaxAttempts());
        dto.setTimeTakenSeconds(summary.getTimeTakenSeconds());
        dto.setTimeLimitSeconds(summary.getTimeLimitSeconds());
        return dto;
    }

    private ChallengeStateResponse.LastGuessResult toLastGuess(Response response, GameSession session) {
        ChallengeStateResponse.LastGuessResult dto = new ChallengeStateResponse.LastGuessResult();
        dto.setGuessedWord(response.getWord());
        dto.setPuzzleSolved(response.getWinner());
        dto.setPuzzleOver(response.getWinner() || (session != null && session.isMaxAttemptsReached()));
        dto.setAttemptNumber(session != null ? session.getCurrentAttempts() : 0);
        dto.setMaxAttempts(session != null ? session.getMaxAttempts() : 0);
        dto.setRemainingWordsCount(response.getRemainingWordsCount() >= 0 ? response.getRemainingWordsCount() : null);
        dto.setResults(response.getStatuses().stream()
                .map(entry -> new ChallengeStateResponse.LetterResult(entry.letter, entry.status))
                .toList());
        return dto;
    }

    private Map<String, String> error(String title, String detail) {
        Map<String, String> error = new HashMap<>();
        error.put("error", title);
        error.put("message", detail);
        return error;
    }
}

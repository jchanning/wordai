package com.fistraltech.server;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ChallengeResultEntity;
import com.fistraltech.server.model.ChallengeSession;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.server.repository.ChallengeResultRepository;
import com.fistraltech.util.Config;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Service layer for Challenge Mode orchestration.
 *
 * <p>This service composes the existing single-puzzle game engine into a 10-puzzle challenge with
 * server-authoritative timing, assist usage, pause/skip rules, and persisted completed results.
 */
@Service
public class ChallengeService {
    private static final Logger logger = Logger.getLogger(ChallengeService.class.getName());
    private static final String DEFAULT_DICTIONARY_ID = "default";
    private static final String DEFAULT_ASSIST_STRATEGY = "ENTROPY";

    private final DictionaryService dictionaryService;
    private final AlgorithmRegistry algorithmRegistry;
    private final ChallengeResultRepository challengeResultRepository;
    private final UserRepository userRepository;
    private final Cache<String, ChallengeSession> activeChallenges;
    private final Clock clock;
    private final Random random;

    @Autowired
    public ChallengeService(DictionaryService dictionaryService,
            AlgorithmRegistry algorithmRegistry,
            ChallengeResultRepository challengeResultRepository,
            UserRepository userRepository,
            @Value("${wordai.challenge.session.ttl-minutes:120}") int challengeSessionTtlMinutes) {
        this(dictionaryService, algorithmRegistry, challengeResultRepository, userRepository,
                Caffeine.newBuilder().expireAfterAccess(challengeSessionTtlMinutes, java.util.concurrent.TimeUnit.MINUTES)
                        .maximumSize(2_000)
                        .build(),
                Clock.systemUTC(),
                new Random());
    }

    ChallengeService(DictionaryService dictionaryService,
            AlgorithmRegistry algorithmRegistry,
            ChallengeResultRepository challengeResultRepository,
            UserRepository userRepository,
            Cache<String, ChallengeSession> activeChallenges,
            Clock clock,
            Random random) {
        this.dictionaryService = dictionaryService;
        this.algorithmRegistry = algorithmRegistry;
        this.challengeResultRepository = challengeResultRepository;
        this.userRepository = userRepository;
        this.activeChallenges = activeChallenges;
        this.clock = clock;
        this.random = random;
    }

    public ChallengeSession startChallenge(String dictionaryId, Integer wordLength, Long userId,
            String browserSessionId) throws InvalidWordException {
        String effectiveDictionaryId = resolveDictionaryId(dictionaryId, wordLength);
        Dictionary masterDictionary = dictionaryService.getMasterDictionary(effectiveDictionaryId);
        if (masterDictionary == null) {
            throw new InvalidWordException("Dictionary not found: " + effectiveDictionaryId);
        }
        if (masterDictionary.getWordCount() < ChallengeSession.TOTAL_PUZZLES) {
            throw new InvalidWordException("Dictionary does not contain enough words for Challenge Mode");
        }

        List<String> targetWords = selectTargetWords(masterDictionary);
        Instant now = clock.instant();
        String challengeId = UUID.randomUUID().toString();
        ChallengeSession challenge = new ChallengeSession(challengeId, effectiveDictionaryId, userId,
                normalizeBrowserSessionId(browserSessionId), targetWords, now);
        startPuzzle(challenge, 0, now);
        activeChallenges.put(challengeId, challenge);
        logger.info(() -> "Created challenge session: " + challengeId);
        return challenge;
    }

    public ChallengeSession getChallenge(String challengeId) {
        ChallengeSession challenge = activeChallenges.getIfPresent(challengeId);
        if (challenge == null) {
            return null;
        }
        synchronized (challenge) {
            expireIfTimedOut(challenge, clock.instant());
            return challenge;
        }
    }

    public ChallengeActionResult makeGuess(String challengeId, String word) throws InvalidWordException {
        ChallengeSession challenge = requireChallenge(challengeId);
        synchronized (challenge) {
            Instant now = clock.instant();
            ensureActiveChallenge(challenge, now);

            GameSession puzzleSession = challenge.getCurrentPuzzleSession();
            Response response = puzzleSession.getWordGame().guess(word.toLowerCase());
            puzzleSession.getWordFilter().update(response);
            response.setRemainingWordsCount(puzzleSession.getRemainingWordsCount());

            String message = null;
            if (response.getWinner()) {
                int scoreAwarded = calculatePuzzleScore(challenge, now);
                recordCompletedPuzzle(challenge, "SOLVED", scoreAwarded, now);
                challenge.setTotalScore(challenge.getTotalScore() + scoreAwarded);
                message = "Puzzle solved.";
                if (challenge.getCurrentPuzzleIndex() >= ChallengeSession.TOTAL_PUZZLES - 1) {
                    completeChallenge(challenge, "COMPLETED");
                    message = "Challenge completed.";
                } else {
                    startPuzzle(challenge, challenge.getCurrentPuzzleIndex() + 1, now);
                }
            } else if (puzzleSession.isMaxAttemptsReached()) {
                recordCompletedPuzzle(challenge, "FAILED_ATTEMPTS", 0, now);
                completeChallenge(challenge, "FAILED_ATTEMPTS");
                message = "Challenge ended: maximum attempts reached.";
            }

            challenge.setUpdatedAt(now);
            return new ChallengeActionResult(challenge, response, null, message);
        }
    }

    public ChallengeActionResult useAssist(String challengeId, String strategy) throws InvalidWordException {
        ChallengeSession challenge = requireChallenge(challengeId);
        synchronized (challenge) {
            Instant now = clock.instant();
            ensureActiveChallenge(challenge, now);
            if (challenge.getCurrentPuzzleAssistsRemaining() <= 0) {
                throw new InvalidWordException("No AI assists remaining for this puzzle");
            }

            GameSession puzzleSession = challenge.getCurrentPuzzleSession();
            String selectedStrategy = StringUtils.hasText(strategy) ? strategy.trim() : DEFAULT_ASSIST_STRATEGY;
            puzzleSession.setSelectedStrategy(selectedStrategy);
            String suggestion = puzzleSession.suggestWord();
            challenge.setCurrentPuzzleAssistsRemaining(challenge.getCurrentPuzzleAssistsRemaining() - 1);
            challenge.setUpdatedAt(now);
            return new ChallengeActionResult(challenge, null, suggestion, "AI assist used.");
        }
    }

    public ChallengeActionResult pauseChallenge(String challengeId) throws InvalidWordException {
        ChallengeSession challenge = requireChallenge(challengeId);
        synchronized (challenge) {
            Instant now = clock.instant();
            ensureActiveChallenge(challenge, now);
            if (challenge.isPauseUsed()) {
                throw new InvalidWordException("Pause has already been used for this challenge");
            }
            challenge.setPauseUsed(true);
            challenge.setCurrentPuzzleDeadline(challenge.getCurrentPuzzleDeadline()
                    .plusSeconds(ChallengeSession.PAUSE_EXTENSION_SECONDS));
            challenge.setUpdatedAt(now);
            return new ChallengeActionResult(challenge, null, null, "Pause applied.");
        }
    }

    public ChallengeActionResult skipPuzzle(String challengeId) throws InvalidWordException {
        ChallengeSession challenge = requireChallenge(challengeId);
        synchronized (challenge) {
            Instant now = clock.instant();
            ensureActiveChallenge(challenge, now);
            if (challenge.isSkipUsed()) {
                throw new InvalidWordException("Skip has already been used for this challenge");
            }
            challenge.setSkipUsed(true);
            challenge.setTotalScore(challenge.getTotalScore() - ChallengeSession.SKIP_PENALTY_POINTS);
            recordCompletedPuzzle(challenge, "SKIPPED", -ChallengeSession.SKIP_PENALTY_POINTS, now);

            String message = "Puzzle skipped.";
            if (challenge.getCurrentPuzzleIndex() >= ChallengeSession.TOTAL_PUZZLES - 1) {
                completeChallenge(challenge, "COMPLETED");
                message = "Challenge completed.";
            } else {
                startPuzzle(challenge, challenge.getCurrentPuzzleIndex() + 1, now);
            }
            challenge.setUpdatedAt(now);
            return new ChallengeActionResult(challenge, null, null, message);
        }
    }

    public List<ChallengeResultEntity> getLeaderboard(int limit) {
        int pageSize = Math.max(1, limit);
        return challengeResultRepository.findAllByOrderByTotalScoreDescCompletedAtAsc(PageRequest.of(0, pageSize))
                .getContent();
    }

    private ChallengeSession requireChallenge(String challengeId) throws InvalidWordException {
        ChallengeSession challenge = activeChallenges.getIfPresent(challengeId);
        if (challenge == null) {
            throw new InvalidWordException("Challenge session not found: " + challengeId);
        }
        return challenge;
    }

    private void ensureActiveChallenge(ChallengeSession challenge, Instant now) throws InvalidWordException {
        expireIfTimedOut(challenge, now);
        if (!challenge.isActive()) {
            throw new InvalidWordException("Challenge is not active");
        }
    }

    private void expireIfTimedOut(ChallengeSession challenge, Instant now) {
        if (!challenge.isActive() || challenge.getCurrentPuzzleDeadline() == null) {
            return;
        }
        if (now.isAfter(challenge.getCurrentPuzzleDeadline())) {
            recordCompletedPuzzle(challenge, "FAILED_TIMEOUT", 0, challenge.getCurrentPuzzleDeadline());
            completeChallenge(challenge, "FAILED_TIMEOUT");
        }
    }

    private void completeChallenge(ChallengeSession challenge, String status) {
        challenge.setStatus(status);
        challenge.setUpdatedAt(clock.instant());
        persistResultIfEligible(challenge);
    }

    private void persistResultIfEligible(ChallengeSession challenge) {
        if (challenge.isResultPersisted() || challenge.getUserId() == null) {
            return;
        }
        User user = userRepository.findById(challenge.getUserId()).orElse(null);
        if (user == null || !StringUtils.hasText(user.getUsername())) {
            return;
        }

        ChallengeResultEntity entity = new ChallengeResultEntity();
        entity.setChallengeId(challenge.getChallengeId());
        entity.setUserId(challenge.getUserId());
        entity.setUsernameSnapshot(user.getUsername());
        entity.setDictionaryId(challenge.getDictionaryId());
        entity.setTotalScore(challenge.getTotalScore());
        entity.setPuzzlesCompleted(challenge.getPuzzlesCompleted());
        entity.setStatus(challenge.getStatus());
        entity.setCompletedAt(LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault()));
        challengeResultRepository.save(entity);
        challenge.setResultPersisted(true);
    }

    private void startPuzzle(ChallengeSession challenge, int puzzleIndex, Instant now) throws InvalidWordException {
        challenge.setCurrentPuzzleIndex(puzzleIndex);
        challenge.setCurrentPuzzleSession(createPuzzleSession(challenge, puzzleIndex));
        challenge.setCurrentPuzzleAssistsRemaining(ChallengeSession.computeAssistAllowance(puzzleIndex));
        challenge.setStatus("ACTIVE");
        challenge.setCurrentPuzzleStartedAt(now);
        challenge.setCurrentPuzzleDeadline(now.plusSeconds(ChallengeSession.computeTimeLimitSeconds(puzzleIndex)));
        challenge.setUpdatedAt(now);
    }

    private GameSession createPuzzleSession(ChallengeSession challenge, int puzzleIndex) throws InvalidWordException {
        Dictionary dictionary = dictionaryService.getDictionaryForGame(challenge.getDictionaryId());
        if (dictionary == null) {
            throw new InvalidWordException("Dictionary not found: " + challenge.getDictionaryId());
        }

        Config gameConfig = new Config();
        gameConfig.setWordLength(dictionary.getWordLength());
        gameConfig.setMaxAttempts(ChallengeSession.MAX_ATTEMPTS_PER_PUZZLE);
        WordGame wordGame = new WordGame(dictionary, gameConfig);
        wordGame.setTargetWord(challenge.getTargetWords().get(puzzleIndex));

        GameSession puzzleSession = new GameSession(
                challenge.getChallengeId() + "-p" + (puzzleIndex + 1),
                wordGame,
                gameConfig,
                dictionary,
                algorithmRegistry);
        puzzleSession.setDictionaryId(challenge.getDictionaryId());
        puzzleSession.setUserId(challenge.getUserId());
        puzzleSession.setBrowserSessionId(challenge.getBrowserSessionId());
        puzzleSession.setCachedWordEntropy(dictionaryService.getWordEntropy(challenge.getDictionaryId()));
        return puzzleSession;
    }

    private void recordCompletedPuzzle(ChallengeSession challenge, String status, int scoreAwarded, Instant now) {
        GameSession puzzleSession = challenge.getCurrentPuzzleSession();
        long timeTakenSeconds = Math.max(0,
                java.time.Duration.between(challenge.getCurrentPuzzleStartedAt(), now).getSeconds());
        ChallengeSession.PuzzleSummary summary = new ChallengeSession.PuzzleSummary(
                challenge.getCurrentPuzzleIndex() + 1,
                puzzleSession.getWordGame().getTargetWord(),
                status,
                scoreAwarded,
                puzzleSession.getCurrentAttempts(),
                puzzleSession.getMaxAttempts(),
                timeTakenSeconds,
                challenge.getCurrentPuzzleTimeLimitSeconds());
        challenge.addCompletedPuzzle(summary);
    }

    private int calculatePuzzleScore(ChallengeSession challenge, Instant now) {
        GameSession puzzleSession = challenge.getCurrentPuzzleSession();
        int attemptsUsed = Math.max(1, puzzleSession.getCurrentAttempts());
        int timeLimit = challenge.getCurrentPuzzleTimeLimitSeconds();
        long timeTaken = Math.max(0, java.time.Duration.between(challenge.getCurrentPuzzleStartedAt(), now).getSeconds());
        int attemptPenalty = (attemptsUsed - 1) * 10;
        int timePenalty = (int) Math.min(50, (timeTaken * 50) / Math.max(1, timeLimit));
        return Math.max(0, 100 - attemptPenalty - timePenalty);
    }

    private String resolveDictionaryId(String dictionaryId, Integer wordLength) {
        if (StringUtils.hasText(dictionaryId)) {
            return dictionaryId.trim();
        }
        if (wordLength != null) {
            return dictionaryService.getAvailableDictionaries().stream()
                    .filter(option -> option.getWordLength() == wordLength)
                    .map(com.fistraltech.util.DictionaryOption::getId)
                    .findFirst()
                    .orElse(DEFAULT_DICTIONARY_ID);
        }
        return DEFAULT_DICTIONARY_ID;
    }

    private String normalizeBrowserSessionId(String browserSessionId) {
        return StringUtils.hasText(browserSessionId) ? browserSessionId.trim() : null;
    }

    private List<String> selectTargetWords(Dictionary dictionary) {
        List<String> words = new ArrayList<>(dictionary.getMasterSetOfWords());
        Collections.shuffle(words, random);
        return new ArrayList<>(words.subList(0, ChallengeSession.TOTAL_PUZZLES));
    }

    public static class ChallengeActionResult {
        private final ChallengeSession challengeSession;
        private final Response guessResponse;
        private final String suggestedWord;
        private final String message;

        public ChallengeActionResult(ChallengeSession challengeSession, Response guessResponse,
                String suggestedWord, String message) {
            this.challengeSession = challengeSession;
            this.guessResponse = guessResponse;
            this.suggestedWord = suggestedWord;
            this.message = message;
        }

        public ChallengeSession getChallengeSession() {
            return challengeSession;
        }

        public Response getGuessResponse() {
            return guessResponse;
        }

        public String getSuggestedWord() {
            return suggestedWord;
        }

        public String getMessage() {
            return message;
        }
    }
}

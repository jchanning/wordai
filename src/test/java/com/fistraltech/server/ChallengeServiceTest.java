package com.fistraltech.server;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.model.ChallengeSession;
import com.fistraltech.server.repository.ChallengeResultRepository;
import com.github.benmanes.caffeine.cache.Caffeine;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService")
class ChallengeServiceTest {

    @Mock DictionaryService dictionaryService;
    @Mock ChallengeResultRepository challengeResultRepository;
    @Mock UserRepository userRepository;

    private ChallengeService service;
    private Dictionary dictionary;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        dictionary = new Dictionary(5);
        Set<String> words = new LinkedHashSet<>(List.of(
                "arose", "stare", "crane", "slate", "raise",
                "later", "cater", "rates", "tears", "store",
                "shore", "spore"));
        dictionary.addWords(words);

        when(dictionaryService.getMasterDictionary("default")).thenReturn(dictionary);
        when(dictionaryService.getDictionaryForGame("default")).thenAnswer(invocation -> dictionary.clone());
        when(dictionaryService.getWordEntropy("default")).thenReturn(null);

        clock = new MutableClock(Instant.parse("2026-03-26T10:00:00Z"));
        service = new ChallengeService(
                dictionaryService,
                AlgorithmRegistry.withDefaults(),
                challengeResultRepository,
                userRepository,
                Caffeine.newBuilder().build(),
                clock,
                new Random(7));
    }

    @Test
    @DisplayName("startChallenge_initializesChallengeState")
    void startChallenge_initializesChallengeState() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");

        assertThat(challenge.getChallengeId()).isNotBlank();
        assertThat(challenge.getTargetWords()).hasSize(ChallengeSession.TOTAL_PUZZLES);
        assertThat(challenge.getCurrentPuzzleNumber()).isEqualTo(1);
        assertThat(challenge.getCurrentPuzzleTimeLimitSeconds()).isEqualTo(120);
        assertThat(challenge.getCurrentPuzzleAssistsRemaining()).isEqualTo(3);
        assertThat(challenge.getCurrentPuzzleSession()).isNotNull();
    }

    @Test
    @DisplayName("computeTimeLimitSeconds_uses120SecondStartAnd10SecondSteps")
    void computeTimeLimitSeconds_uses120SecondStartAnd10SecondSteps() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");

        assertThat(challenge.getCurrentPuzzleTimeLimitSeconds()).isEqualTo(120);
        assertThat(ChallengeSession.computeTimeLimitSeconds(0)).isEqualTo(120);
        assertThat(ChallengeSession.computeTimeLimitSeconds(1)).isEqualTo(110);
        assertThat(ChallengeSession.computeTimeLimitSeconds(8)).isEqualTo(40);
        assertThat(ChallengeSession.computeTimeLimitSeconds(9)).isEqualTo(30);
        assertThat(ChallengeSession.computeTimeLimitSeconds(10)).isEqualTo(20);
    }

    @Test
    @DisplayName("useAssist_consumesOneAssistAndReturnsSuggestion")
    void useAssist_consumesOneAssistAndReturnsSuggestion() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");

        ChallengeService.ChallengeActionResult result = service.useAssist(challenge.getChallengeId(), "RANDOM");

        assertThat(result.getSuggestedWord()).isNotBlank();
        assertThat(result.getChallengeSession().getCurrentPuzzleAssistsRemaining()).isEqualTo(2);
    }

    @Test
    @DisplayName("pauseChallenge_secondPauseRejected")
    void pauseChallenge_secondPauseRejected() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");

        service.pauseChallenge(challenge.getChallengeId());

        InvalidWordException exception = assertThrows(InvalidWordException.class,
                () -> service.pauseChallenge(challenge.getChallengeId()));
        assertThat(exception.getMessage()).contains("Pause has already been used");
    }

    @Test
    @DisplayName("skipPuzzle_appliesPenaltyAndAdvancesPuzzle")
    void skipPuzzle_appliesPenaltyAndAdvancesPuzzle() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");

        ChallengeService.ChallengeActionResult result = service.skipPuzzle(challenge.getChallengeId());

        assertThat(result.getChallengeSession().getTotalScore()).isEqualTo(-20);
        assertThat(result.getChallengeSession().getCurrentPuzzleNumber()).isEqualTo(2);
        assertThat(result.getChallengeSession().getCompletedPuzzles()).hasSize(1);
        assertThat(result.getChallengeSession().getCompletedPuzzles().get(0).getStatus()).isEqualTo("SKIPPED");
    }

    @Test
    @DisplayName("makeGuess_solvingPuzzleAdvancesChallenge")
    void makeGuess_solvingPuzzleAdvancesChallenge() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");
        String targetWord = challenge.getCurrentPuzzleSession().getWordGame().getTargetWord();

        ChallengeService.ChallengeActionResult result = service.makeGuess(challenge.getChallengeId(), targetWord);

        assertThat(result.getGuessResponse()).isNotNull();
        assertThat(result.getGuessResponse().getWinner()).isTrue();
        assertThat(result.getChallengeSession().getCurrentPuzzleNumber()).isEqualTo(2);
        assertThat(result.getChallengeSession().getCompletedPuzzles()).hasSize(1);
        assertThat(result.getChallengeSession().getTotalScore()).isPositive();
    }

    @Test
    @DisplayName("makeGuess_afterTimeoutFailsChallengeAndPersistsResult")
    void makeGuess_afterTimeoutFailsChallengeAndPersistsResult() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setUsername("alice");
        when(userRepository.findById(42L)).thenReturn(java.util.Optional.of(user));

        ChallengeSession challenge = service.startChallenge(null, null, 42L, "browser-1");
        clock.advanceSeconds(121);

        InvalidWordException exception = assertThrows(InvalidWordException.class,
                () -> service.makeGuess(challenge.getChallengeId(), "arose"));

        assertThat(exception.getMessage()).contains("Challenge is not active");
        ChallengeSession stored = service.getChallenge(challenge.getChallengeId());
        assertThat(stored.getStatus()).isEqualTo("FAILED_TIMEOUT");
        verify(challengeResultRepository).save(any());
    }

    @Test
    @DisplayName("completedChallenge_withoutAuthenticatedUserDoesNotPersist")
    void completedChallenge_withoutAuthenticatedUserDoesNotPersist() throws Exception {
        ChallengeSession challenge = service.startChallenge(null, null, null, "browser-1");
        for (int index = 0; index < ChallengeSession.TOTAL_PUZZLES; index++) {
            String targetWord = challenge.getCurrentPuzzleSession().getWordGame().getTargetWord();
            service.makeGuess(challenge.getChallengeId(), targetWord);
        }

        ChallengeSession finished = service.getChallenge(challenge.getChallengeId());
        assertThat(finished.isChallengeComplete()).isTrue();
        verify(challengeResultRepository, never()).save(any());
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }
    }
}

package com.fistraltech.server.controller;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.core.InvalidWordException;
import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.server.ChallengeService;
import com.fistraltech.server.GameHistoryService;
import com.fistraltech.server.model.ChallengeResultEntity;
import com.fistraltech.server.model.ChallengeSession;

@WebMvcTest(ChallengeController.class)
@Import(SecurityConfig.class)
@DisplayName("ChallengeController Tests")
class ChallengeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChallengeService challengeService;

    @MockitoBean
    @SuppressWarnings("unused")
    private GameHistoryService gameHistoryService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("createChallenge_returnsCreatedState")
    void createChallenge_returnsCreatedState() throws Exception {
        ChallengeSession challenge = new ChallengeSession(
                "challenge-1",
                "default",
                null,
                "browser-1",
                List.of("arose", "stare", "crane", "slate", "raise", "later", "cater", "rates", "tears", "store"),
                Instant.parse("2026-03-26T10:00:00Z"));
        challenge.setCurrentPuzzleAssistsRemaining(3);
        challenge.setCurrentPuzzleIndex(0);
        challenge.setCurrentPuzzleStartedAt(Instant.parse("2026-03-26T10:00:00Z"));
        challenge.setCurrentPuzzleDeadline(Instant.parse("2026-03-26T10:01:00Z"));
        when(challengeService.startChallenge(any(), any(), any(), any())).thenReturn(challenge);

        mockMvc.perform(post("/api/wordai/challenges")
                        .contentType("application/json")
                        .content("""
                                {"dictionaryId":"default","browserSessionId":"browser-1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"challengeId\":\"challenge-1\"")))
                .andExpect(content().string(containsString("\"currentPuzzleAssistsRemaining\":3")));
    }

        @Test
        @DisplayName("createChallenge_invalidWordLength_returnsBadRequest")
        void createChallenge_invalidWordLength_returnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/wordai/challenges")
                                                .contentType("application/json")
                                                .content("""
                                                                {"wordLength":1}
                                                                """))
                                .andExpect(status().isBadRequest());
        }

    @Test
    @DisplayName("makeGuess_withBlankWord_returnsBadRequest")
    void makeGuess_withBlankWord_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/wordai/challenges/challenge-1/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":""}
                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("useAssist_withBlankStrategy_returnsBadRequest")
        void useAssist_withBlankStrategy_returnsBadRequest() throws Exception {
                mockMvc.perform(post("/api/wordai/challenges/challenge-1/assist")
                                                .contentType("application/json")
                                                .content("""
                                                                {"strategy":""}
                                                                """))
                                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("leaderboard_returnsEntries")
    void leaderboard_returnsEntries() throws Exception {
        ChallengeResultEntity entity = new ChallengeResultEntity();
        entity.setChallengeId("challenge-1");
        entity.setUsernameSnapshot("alice");
        entity.setTotalScore(320);
        entity.setPuzzlesCompleted(4);
        entity.setStatus("FAILED_TIMEOUT");
        entity.setCompletedAt(java.time.LocalDateTime.parse("2026-03-26T10:15:00"));
        when(challengeService.getLeaderboard(20)).thenReturn(List.of(entity));

        mockMvc.perform(get("/api/wordai/challenges/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"username\":\"alice\"")))
                .andExpect(content().string(containsString("\"totalScore\":320")));
    }

        @Test
        @DisplayName("leaderboard_versionedRoute_returnsEntries")
        void leaderboard_versionedRoute_returnsEntries() throws Exception {
                ChallengeResultEntity entity = new ChallengeResultEntity();
                entity.setChallengeId("challenge-1");
                entity.setUsernameSnapshot("alice");
                entity.setTotalScore(320);
                entity.setPuzzlesCompleted(4);
                entity.setStatus("FAILED_TIMEOUT");
                entity.setCompletedAt(java.time.LocalDateTime.parse("2026-03-26T10:15:00"));
                when(challengeService.getLeaderboard(20)).thenReturn(List.of(entity));

                mockMvc.perform(get("/api/v1/wordai/challenges/leaderboard"))
                                .andExpect(status().isOk())
                                .andExpect(content().string(containsString("\"username\":\"alice\"")))
                                .andExpect(content().string(containsString("\"totalScore\":320")));
        }

    @Test
    @DisplayName("makeGuess_invalidChallengeRequest_returnsBadRequest")
    void makeGuess_invalidChallengeRequest_returnsBadRequest() throws Exception {
        when(challengeService.makeGuess("challenge-1", "crane"))
                .thenThrow(new InvalidWordException("Challenge is not active"));

        mockMvc.perform(post("/api/wordai/challenges/challenge-1/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":"crane"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Challenge is not active")));
    }
}

package com.fistraltech.server.controller;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.core.Dictionary;
import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.core.WordGame;
import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.server.AlgorithmFeatureService;
import com.fistraltech.server.GameHistoryService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.model.GameSession;
import com.fistraltech.web.ApiResourceNotFoundException;

@WebMvcTest(WordGameController.class)
@Import(SecurityConfig.class)
@DisplayName("WordGameController Tests")
class WordGameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordGameService gameService;

    @MockitoBean
    @SuppressWarnings("unused")
    private AlgorithmFeatureService algorithmFeatureService;

    @MockitoBean
    private GameHistoryService gameHistoryService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomOAuth2UserService customOAuth2UserService;

    private GameSession mockSession;
    private Dictionary testDictionary;

    @BeforeEach
    void setUp() throws Exception {
        testDictionary = new Dictionary(5);
        Set<String> words = new HashSet<>();
        words.add("arose");
        words.add("stare");
        words.add("crane");
        words.add("slate");
        words.add("raise");
        testDictionary.addWords(words);

        WordGame mockWordGame = mock(WordGame.class);
        mockSession = mock(GameSession.class);
        when(mockSession.getWordGame()).thenReturn(mockWordGame);
        when(mockWordGame.getDictionary()).thenReturn(testDictionary);
        when(mockSession.getMaxAttempts()).thenReturn(6);
        when(mockSession.getCurrentAttempts()).thenReturn(1);
        when(mockSession.getFilteredDictionary()).thenReturn(testDictionary);

        when(gameHistoryService.resolveUser(any())).thenReturn(Optional.empty());
        when(gameService.createGame(any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn("test-game-1");
        when(gameService.getGameSession("test-game-1")).thenReturn(mockSession);
    }

    @Test
    @DisplayName("createGame_noBody_returns201WithDictionaryMetrics")
    void createGame_noBody_returns201WithDictionaryMetrics() throws Exception {
        mockMvc.perform(post("/api/wordai/games")
                        .contentType("application/json"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("test-game-1"))
                .andExpect(jsonPath("$.dictionaryMetrics").value(notNullValue()))
                .andExpect(jsonPath("$.dictionaryMetrics.totalWords").value(5));
    }

        @Test
        @DisplayName("createGame_versionedRoute_returns201WithDictionaryMetrics")
        void createGame_versionedRoute_returns201WithDictionaryMetrics() throws Exception {
                mockMvc.perform(post("/api/v1/wordai/games")
                                                .contentType("application/json"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.gameId").value("test-game-1"))
                                .andExpect(jsonPath("$.dictionaryMetrics").value(notNullValue()))
                                .andExpect(jsonPath("$.dictionaryMetrics.totalWords").value(5));
        }

    @Test
    @DisplayName("createGame_withDictionaryId_returns201")
    void createGame_withDictionaryId_returns201() throws Exception {
        mockMvc.perform(post("/api/wordai/games")
                        .contentType("application/json")
                        .content("""
                                {"dictionaryId":"5"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId").value("test-game-1"));
    }

        @Test
        @DisplayName("createGame_invalidWordLength_returns400")
        void createGame_invalidWordLength_returns400() throws Exception {
                mockMvc.perform(post("/api/wordai/games")
                                                .contentType("application/json")
                                                .content("""
                                                                {"wordLength":1}
                                                                """))
                                .andExpect(status().isBadRequest());
        }

    @Test
    @DisplayName("createGame_returnsMetricsWithOccurrenceCountByPosition")
    void createGame_returnsMetricsWithOccurrenceCountByPosition() throws Exception {
        mockMvc.perform(post("/api/wordai/games")
                        .contentType("application/json"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dictionaryMetrics.occurrenceCountByPosition").value(notNullValue()))
                .andExpect(jsonPath("$.dictionaryMetrics.columnLengths").isArray());
    }

    @Test
    @DisplayName("createGame_serviceThrowsInvalidWordException_returns400")
    void createGame_serviceThrowsInvalidWordException_returns400() throws Exception {
        when(gameService.createGame(any(), any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new InvalidWordException("Unknown dictionary"));

        mockMvc.perform(post("/api/wordai/games")
                        .contentType("application/json")
                        .content("""
                                {"dictionaryId":"unknown"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    @DisplayName("makeGuess_validWord_returns200WithMetrics")
    void makeGuess_validWord_returns200WithMetrics() throws Exception {
        Response guessResponse = new Response("arose");
        guessResponse.setStatus('a', 'G');
        guessResponse.setStatus('r', 'G');
        guessResponse.setStatus('o', 'G');
        guessResponse.setStatus('s', 'G');
        guessResponse.setStatus('e', 'G');
        guessResponse.setWinner(true);
        when(gameService.makeGuess("test-game-1", "arose")).thenReturn(guessResponse);

        mockMvc.perform(post("/api/wordai/games/test-game-1/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":"arose"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value("test-game-1"))
                .andExpect(jsonPath("$.dictionaryMetrics").value(notNullValue()));
    }

    @Test
    @DisplayName("makeGuess_blankWord_returns400")
    void makeGuess_blankWord_returns400() throws Exception {
        mockMvc.perform(post("/api/wordai/games/test-game-1/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("makeGuess_invalidWord_returns400")
    void makeGuess_invalidWord_returns400() throws Exception {
        when(gameService.makeGuess("test-game-1", "zzzzz"))
                .thenThrow(new InvalidWordException("Word not in dictionary"));

        mockMvc.perform(post("/api/wordai/games/test-game-1/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":"zzzzz"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid word"));
    }

    @Test
    @DisplayName("makeGuess_gameNotFound_returns404")
    void makeGuess_gameNotFound_returns404() throws Exception {
        when(gameService.makeGuess("missing-game", "arose"))
                .thenThrow(new ApiResourceNotFoundException("Game not found",
                        "Game session missing-game does not exist"));

        mockMvc.perform(post("/api/wordai/games/missing-game/guess")
                        .contentType("application/json")
                        .content("""
                                {"word":"arose"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Game not found"))
                .andExpect(jsonPath("$.message").value("Game session missing-game does not exist"));
    }

    @Test
    @DisplayName("getGameState_reconstructionFailure_returns500WithErrorBody")
    void getGameState_reconstructionFailure_returns500WithErrorBody() throws Exception {
        when(gameService.getGameSession("broken-game"))
                .thenThrow(new IllegalStateException("Failed to reconstruct session broken-game"));

        mockMvc.perform(get("/api/wordai/games/broken-game"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.message").value("Failed to get game state"));
    }
}

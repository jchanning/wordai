package com.fistraltech.server.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.analysis.AnalysisResponse;
import com.fistraltech.core.Dictionary;
import com.fistraltech.core.WordEntropy;
import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.server.AlgorithmFeatureService;
import com.fistraltech.server.DictionaryService;
import com.fistraltech.server.GameHistoryService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.algo.AlgorithmRegistry;
import com.fistraltech.server.algo.BellmanFullDictAlgorithmDescriptor;
import com.fistraltech.server.algo.EntropyAlgorithmDescriptor;
import com.fistraltech.server.algo.RandomAlgorithmDescriptor;
import com.fistraltech.server.dto.GameHistoryDto;
import com.fistraltech.util.DictionaryOption;

@WebMvcTest({DictionaryController.class, AlgorithmController.class, AnalysisController.class, HistoryController.class})
@Import({SecurityConfig.class, AlgorithmFeatureService.class, AlgorithmRegistry.class,
    RandomAlgorithmDescriptor.class, EntropyAlgorithmDescriptor.class, BellmanFullDictAlgorithmDescriptor.class})
@DisplayName("Resource Controller Tests")
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DictionaryService dictionaryService;

    @MockitoBean
    private WordGameService wordGameService;

    @MockitoBean
    private GameHistoryService gameHistoryService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("getDictionaries_returnsCatalog")
    void getDictionaries_returnsCatalog() throws Exception {
        DictionaryOption option = new DictionaryOption();
        option.setId("5");
        option.setName("Default");
        option.setWordLength(5);
        option.setAvailable(true);

        when(dictionaryService.getAvailableDictionaries()).thenReturn(List.of(option));

        mockMvc.perform(get("/api/wordai/dictionaries"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"id\":\"5\"")))
            .andExpect(content().string(containsString("\"wordLength\":5")));
    }

            @Test
            @DisplayName("getDictionary_returnsDictionaryDetailAndEntropy")
            void getDictionary_returnsDictionaryDetailAndEntropy() throws Exception {
            Dictionary dictionary = new Dictionary(5);
            dictionary.addWords(Set.of("arose", "stare"));

            WordEntropy wordEntropy = mock(WordEntropy.class);
            when(wordEntropy.getEntropy("arose")).thenReturn(3.5f);
            when(wordEntropy.getEntropy("stare")).thenReturn(3.0f);

            when(dictionaryService.getMasterDictionary("5")).thenReturn(dictionary);
            when(dictionaryService.getWordEntropy("5")).thenReturn(wordEntropy);

            mockMvc.perform(get("/api/wordai/dictionaries/5"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"id\":\"5\"")))
                .andExpect(content().string(containsString("\"wordLength\":5")))
                .andExpect(content().string(containsString("\"wordCount\":2")))
                .andExpect(content().string(containsString("\"entropy\"")));
            }

            @Test
            @DisplayName("getDictionary_returnsNotFound_whenDictionaryMissing")
            void getDictionary_returnsNotFound_whenDictionaryMissing() throws Exception {
            when(dictionaryService.getMasterDictionary("missing")).thenReturn(null);

            mockMvc.perform(get("/api/wordai/dictionaries/missing"))
                .andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("getDictionary_returnsNotFound_whenServiceThrows")
            void getDictionary_returnsNotFound_whenServiceThrows() throws Exception {
            when(dictionaryService.getMasterDictionary("bad"))
                .thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/api/wordai/dictionaries/bad"))
                .andExpect(status().isNotFound());
            }

    @Test
    @DisplayName("getAlgorithms_returnsServiceMetadata")
    void getAlgorithms_returnsServiceMetadata() throws Exception {
        mockMvc.perform(get("/api/wordai/algorithms"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"id\":\"RANDOM\"")))
            .andExpect(content().string(containsString("Picks words at random from valid candidates")))
            .andExpect(content().string(containsString("\"id\":\"ENTROPY\"")))
            .andExpect(content().string(containsString("\"id\":\"BELLMAN_FULL_DICTIONARY\"")))
            .andExpect(content().string(containsString("\"stateful\":\"true\"")))
            .andExpect(content().string(containsString("\"enabled\":\"true\"")));
    }

    @Test
    @DisplayName("runAnalysis_authenticatedRequest_returnsAnalysisResponse")
    @WithMockUser
    void runAnalysis_authenticatedRequest_returnsAnalysisResponse() throws Exception {
        AnalysisResponse response = new AnalysisResponse();
        response.setAlgorithm("ENTROPY");
        response.setDictionaryId("5");
        response.setTotalGames(10);
        response.setWinRate(90.0);
        response.setCompleted(true);

        when(wordGameService.runAnalysis("ENTROPY", "5", 10)).thenReturn(response);

        mockMvc.perform(post("/api/wordai/analysis")
                        .contentType("application/json")
                        .content("""
                                {"algorithm":"ENTROPY","dictionaryId":"5","maxGames":10}
                                """))
                .andExpect(status().isOk())
        .andExpect(content().string(containsString("\"algorithm\":\"ENTROPY\"")))
        .andExpect(content().string(containsString("\"totalGames\":10")));
    }

    @Test
    @DisplayName("getPlayerHistory_authenticatedRequest_returnsPersistedGames")
    @WithMockUser(username = "alice")
    void getPlayerHistory_authenticatedRequest_returnsPersistedGames() throws Exception {
        GameHistoryDto dto = new GameHistoryDto();
        dto.setId(1L);
        dto.setTargetWord("crane");
        dto.setResult("WON");

        when(gameHistoryService.getHistory(any())).thenReturn(Optional.of(
                new GameHistoryService.UserHistory("alice", List.of(dto))));

        mockMvc.perform(get("/api/wordai/history"))
                .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"username\":\"alice\"")))
            .andExpect(content().string(containsString("\"total\":1")))
            .andExpect(content().string(containsString("\"targetWord\":\"crane\"")));
    }

    @Test
    @DisplayName("getPlayerHistory_unauthenticatedRequest_redirectsToLogin")
    void getPlayerHistory_unauthenticatedRequest_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/wordai/history"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login.html"));
    }

    @Test
    @DisplayName("placeholderStatsRoute_returnsNotFound")
    void placeholderStatsRoute_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/wordai/stats/user"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("placeholderAnalyticsRoute_returnsNotFound")
    void placeholderAnalyticsRoute_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/wordai/analytics/global"))
                .andExpect(status().isNotFound());
    }
}
package com.fistraltech.server.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.core.InvalidWordException;
import com.fistraltech.core.Response;
import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.server.ManualAssistantService;
import com.fistraltech.server.model.ManualAssistantSession;
import com.fistraltech.web.ApiResourceNotFoundException;

@WebMvcTest(ManualAssistantController.class)
@Import(SecurityConfig.class)
@WithMockUser
@DisplayName("ManualAssistantController Tests")
class ManualAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ManualAssistantService manualAssistantService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("createSession_returns201")
    void createSession_returns201() throws Exception {
        ManualAssistantSession session = org.mockito.Mockito.mock(ManualAssistantSession.class);
        when(session.getSessionId()).thenReturn("assistant-1");
        when(session.getDictionaryId()).thenReturn("default");
        when(session.getWordLength()).thenReturn(5);
        when(session.getSelectedStrategy()).thenReturn("RANDOM");
        when(session.getRemainingWordsCount()).thenReturn(2315);
        when(manualAssistantService.createSession("default", "RANDOM")).thenReturn(session);

        mockMvc.perform(post("/api/v1/wordai/assistant/sessions")
                .contentType("application/json")
                .content("""
                    {"dictionaryId":"default","strategy":"RANDOM"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sessionId").value("assistant-1"))
            .andExpect(jsonPath("$.remainingWords").value(2315));
    }

            @Test
            @DisplayName("createSession_legacyRoute_returns201")
            void createSession_legacyRoute_returns201() throws Exception {
            ManualAssistantSession session = org.mockito.Mockito.mock(ManualAssistantSession.class);
            when(session.getSessionId()).thenReturn("assistant-legacy");
            when(session.getDictionaryId()).thenReturn("default");
            when(session.getWordLength()).thenReturn(5);
            when(session.getSelectedStrategy()).thenReturn("RANDOM");
            when(session.getRemainingWordsCount()).thenReturn(42);
            when(manualAssistantService.createSession("default", "RANDOM")).thenReturn(session);

            mockMvc.perform(post("/api/wordai/assistant/sessions")
                .contentType("application/json")
                .content("""
                    {"dictionaryId":"default","strategy":"RANDOM"}
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").value("assistant-legacy"))
                .andExpect(jsonPath("$.remainingWords").value(42));
            }

    @Test
    @DisplayName("submitFeedback_returns200")
    void submitFeedback_returns200() throws Exception {
        Response response = new Response("crane");
        response.setStatus('c', 'R');
        response.setStatus('r', 'A');
        response.setStatus('a', 'R');
        response.setStatus('n', 'R');
        response.setStatus('e', 'G');

        ManualAssistantSession session = org.mockito.Mockito.mock(ManualAssistantSession.class);
        when(session.getAttemptCount()).thenReturn(1);
        when(session.getRemainingWordsCount()).thenReturn(120);

        when(manualAssistantService.submitFeedback("assistant-1", "crane", "⬛🟨⬛⬛🟩")).thenReturn(response);
        when(manualAssistantService.getSession("assistant-1")).thenReturn(session);

        mockMvc.perform(post("/api/v1/wordai/assistant/sessions/assistant-1/feedback")
                .contentType("application/json")
                .content("""
                    {"guessedWord":"crane","feedbackPattern":"⬛🟨⬛⬛🟩"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.normalizedFeedback").value("RARRG"))
            .andExpect(jsonPath("$.attemptNumber").value(1));
    }

    @Test
    @DisplayName("submitFeedback_invalidRequest_returns400")
    void submitFeedback_invalidRequest_returns400() throws Exception {
        when(manualAssistantService.submitFeedback("assistant-1", "crane", "bad"))
            .thenThrow(new InvalidWordException("Invalid feedback"));

        mockMvc.perform(post("/api/v1/wordai/assistant/sessions/assistant-1/feedback")
                .contentType("application/json")
                .content("""
                    {"guessedWord":"crane","feedbackPattern":"bad"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid request"));
    }

    @Test
    @DisplayName("getSuggestion_missingSession_returns404")
    void getSuggestion_missingSession_returns404() throws Exception {
        when(manualAssistantService.getSession("missing"))
            .thenThrow(new ApiResourceNotFoundException("Assistant session not found",
                "Assistant session missing does not exist"));

        mockMvc.perform(get("/api/v1/wordai/assistant/sessions/missing/suggestion"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Assistant session not found"));
    }

    @Test
    @DisplayName("getSuggestion_returns200")
    void getSuggestion_returns200() throws Exception {
        ManualAssistantSession session = org.mockito.Mockito.mock(ManualAssistantSession.class);
        when(session.getSelectedStrategy()).thenReturn("ENTROPY");
        when(session.getRemainingWordsCount()).thenReturn(17);

        when(manualAssistantService.getSession("assistant-1")).thenReturn(session);
        when(manualAssistantService.suggestWord("assistant-1")).thenReturn("stare");

        mockMvc.perform(get("/api/v1/wordai/assistant/sessions/assistant-1/suggestion"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("assistant-1"))
            .andExpect(jsonPath("$.suggestion").value("stare"))
            .andExpect(jsonPath("$.strategy").value("ENTROPY"))
            .andExpect(jsonPath("$.remainingWords").value(17));
    }

    @Test
    @DisplayName("setStrategy_returns200")
    void setStrategy_returns200() throws Exception {
        when(manualAssistantService.setStrategy("assistant-1", "ENTROPY")).thenReturn("ENTROPY");

        mockMvc.perform(put("/api/v1/wordai/assistant/sessions/assistant-1/strategy")
                .contentType("application/json")
                .content("""
                    {"strategy":"ENTROPY"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.strategy").value("ENTROPY"));
    }

    @Test
    @DisplayName("deleteSession_returns200")
    void deleteSession_returns200() throws Exception {
        ManualAssistantSession session = org.mockito.Mockito.mock(ManualAssistantSession.class);
        when(manualAssistantService.getSession("assistant-1")).thenReturn(session);

        mockMvc.perform(delete("/api/v1/wordai/assistant/sessions/assistant-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("assistant-1"));
    }
}
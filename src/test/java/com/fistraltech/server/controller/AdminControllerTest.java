package com.fistraltech.server.controller;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fistraltech.security.config.SecurityConfig;
import com.fistraltech.security.service.CustomOAuth2UserService;
import com.fistraltech.server.ActivityService;
import com.fistraltech.server.AlgorithmFeatureService;
import com.fistraltech.server.SessionTrackingService;
import com.fistraltech.server.WordGameService;
import com.fistraltech.server.dto.UserActivityDto;
import com.fistraltech.server.model.SessionInfo;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
@DisplayName("Admin Controller Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionTrackingService sessionTrackingService;

    @MockitoBean
    private WordGameService wordGameService;

    @MockitoBean
    private ActivityService activityService;

    @MockitoBean
    private AlgorithmFeatureService algorithmFeatureService;

    @MockitoBean
    @SuppressWarnings("unused")
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    @DisplayName("adminEndpoints_redirectUnauthenticated")
    void adminEndpoints_redirectUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/wordai/admin/sessions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login.html"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("adminEndpoints_forbidNonAdmin")
    void adminEndpoints_forbidNonAdmin() throws Exception {
        mockMvc.perform(get("/api/wordai/admin/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getActiveSessions_returnsSessionSummary")
    void getActiveSessions_returnsSessionSummary() throws Exception {
        SessionInfo session = new SessionInfo("session-1", "1", "alice", "127.0.0.1", "Mozilla/5.0");
        when(sessionTrackingService.getActiveSessions()).thenReturn(List.of(session));
        when(wordGameService.getActiveSessionCount()).thenReturn(3);

        mockMvc.perform(get("/api/wordai/admin/sessions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalSessions\":1")))
                .andExpect(content().string(containsString("\"activeGameSessions\":3")))
                .andExpect(content().string(containsString("\"sessionId\":\"session-1\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getActiveSessions_returnsServerError_whenServiceFails")
    void getActiveSessions_returnsServerError_whenServiceFails() throws Exception {
        when(sessionTrackingService.getActiveSessions()).thenThrow(new RuntimeException("session failure"));

        mockMvc.perform(get("/api/wordai/admin/sessions"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to retrieve session information")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getSystemStats_returnsStatsPayload")
    void getSystemStats_returnsStatsPayload() throws Exception {
        when(sessionTrackingService.getActiveSessionCount()).thenReturn(2);
        when(wordGameService.getActiveSessionCount()).thenReturn(1);

        mockMvc.perform(get("/api/wordai/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"activeSessions\":2")))
                .andExpect(content().string(containsString("\"activeGameSessions\":1")))
                .andExpect(content().string(containsString("\"memory\"")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getSystemStats_versionedRoute_returnsStatsPayload")
    void getSystemStats_versionedRoute_returnsStatsPayload() throws Exception {
        when(sessionTrackingService.getActiveSessionCount()).thenReturn(2);
        when(wordGameService.getActiveSessionCount()).thenReturn(1);

        mockMvc.perform(get("/api/v1/wordai/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"activeSessions\":2")))
                .andExpect(content().string(containsString("\"activeGameSessions\":1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("cleanupSessions_returnsRemovalSummary")
    void cleanupSessions_returnsRemovalSummary() throws Exception {
        when(sessionTrackingService.getActiveSessionCount()).thenReturn(5, 3);

        mockMvc.perform(post("/api/wordai/admin/cleanup-sessions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"sessionsRemoved\":2")))
                .andExpect(content().string(containsString("\"remainingSessions\":3")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("cleanupSessions_returnsServerError_whenCleanupFails")
    void cleanupSessions_returnsServerError_whenCleanupFails() throws Exception {
        when(sessionTrackingService.getActiveSessionCount()).thenReturn(4);
        doThrow(new RuntimeException("cleanup failure")).when(sessionTrackingService).cleanupInactiveSessions(30);

        mockMvc.perform(post("/api/wordai/admin/cleanup-sessions"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to cleanup sessions")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUserActivity_returnsActivitySummary")
    void getUserActivity_returnsActivitySummary() throws Exception {
        UserActivityDto active = new UserActivityDto();
        active.setUsername("alice");
        active.setGamesLast7Days(2);
        active.setGamesLast30Days(5);
        active.setLastGameDate(LocalDateTime.now());

        UserActivityDto inactive = new UserActivityDto();
        inactive.setUsername("bob");
        inactive.setGamesLast7Days(0);
        inactive.setGamesLast30Days(0);

        when(activityService.getUserActivityStats()).thenReturn(List.of(active, inactive));

        mockMvc.perform(get("/api/wordai/admin/activity"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"totalUsers\":2")))
                .andExpect(content().string(containsString("\"activeUsersLast7Days\":1")))
                .andExpect(content().string(containsString("\"activeUsersLast30Days\":1")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("getUserActivity_returnsServerError_whenActivityServiceFails")
    void getUserActivity_returnsServerError_whenActivityServiceFails() throws Exception {
        when(activityService.getUserActivityStats()).thenThrow(new RuntimeException("activity failure"));

        mockMvc.perform(get("/api/wordai/admin/activity"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to retrieve activity statistics")));
    }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("updateAlgorithmPolicy_returnsUpdatedRuntimeState")
        void updateAlgorithmPolicy_returnsUpdatedRuntimeState() throws Exception {
        when(algorithmFeatureService.updateAlgorithmEnabled("ENTROPY", false))
            .thenReturn(new AlgorithmFeatureService.AlgorithmInfo(
                "ENTROPY",
                "Maximum Entropy",
                "Chooses the guess with the highest expected information gain",
                false,
                "algorithm.features.entropy.enabled",
                false));

        mockMvc.perform(put("/api/wordai/admin/algorithms/ENTROPY")
                .contentType("application/json")
                .content("""
                    {"enabled":false}
                    """))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"id\":\"ENTROPY\"")))
            .andExpect(content().string(containsString("\"enabled\":false")));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("updateAlgorithmPolicy_returnsBadRequest_whenAlgorithmUnknown")
        void updateAlgorithmPolicy_returnsBadRequest_whenAlgorithmUnknown() throws Exception {
        when(algorithmFeatureService.updateAlgorithmEnabled("UNKNOWN", true))
            .thenThrow(new IllegalArgumentException("Unknown algorithm: UNKNOWN"));

        mockMvc.perform(put("/api/wordai/admin/algorithms/UNKNOWN")
                .contentType("application/json")
                .content("""
                    {"enabled":true}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Invalid algorithm")));
        }
}

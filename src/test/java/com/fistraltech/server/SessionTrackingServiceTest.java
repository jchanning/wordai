package com.fistraltech.server;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import com.fistraltech.server.model.SessionInfo;

@DisplayName("SessionTrackingService Tests")
class SessionTrackingServiceTest {

    @Test
    @DisplayName("trackSession_createsSessionFromForwardedRequest")
    void trackSession_createsSessionFromForwardedRequest() {
        SessionTrackingService service = new SessionTrackingService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession(null, "session-1"));
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.5");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0) Chrome/123.0");
        request.setRemoteAddr("10.0.0.5");

        service.trackSession(request, "42", "alice");

        SessionInfo sessionInfo = service.getSessionInfo("session-1");
        assertNotNull(sessionInfo);
        assertEquals("42", sessionInfo.getUserId());
        assertEquals("alice", sessionInfo.getUsername());
        assertEquals("203.0.113.10", sessionInfo.getIpAddress());
        assertEquals("Chrome", sessionInfo.getBrowser());
        assertEquals("Windows", sessionInfo.getOperatingSystem());
        assertEquals(1, service.getActiveSessionCount());
    }

    @Test
    @DisplayName("trackSession_existingSessionUpdatesIdentityAndActivity")
    void trackSession_existingSessionUpdatesIdentityAndActivity() {
        SessionTrackingService service = new SessionTrackingService();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession(null, "session-2"));
        request.addHeader("User-Agent", "Mozilla/5.0 (Mac OS X) Safari/605.1.15");
        request.setRemoteAddr("198.51.100.20");

        service.trackSession(request, null, null);
        SessionInfo sessionInfo = service.getSessionInfo("session-2");
        sessionInfo.setLastActivity(LocalDateTime.now().minusMinutes(10));

        service.trackSession(request, "7", "bob");

        assertEquals("7", sessionInfo.getUserId());
        assertEquals("bob", sessionInfo.getUsername());
        assertEquals("198.51.100.20", sessionInfo.getIpAddress());
        assertTrue(sessionInfo.getLastActivity().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("updateActivity_and_setCurrentGame_ignoreMissingSessions")
    void updateActivity_and_setCurrentGame_ignoreMissingSessions() {
        SessionTrackingService service = new SessionTrackingService();
        MockHttpServletRequest requestWithoutSession = new MockHttpServletRequest();

        service.updateActivity(requestWithoutSession);
        service.setCurrentGame(requestWithoutSession, "game-1");

        MockHttpServletRequest trackedRequest = new MockHttpServletRequest();
        trackedRequest.setSession(new MockHttpSession(null, "session-3"));
        trackedRequest.setRemoteAddr("192.0.2.24");
        service.trackSession(trackedRequest, "9", "carol");

        SessionInfo sessionInfo = service.getSessionInfo("session-3");
        sessionInfo.setLastActivity(LocalDateTime.now().minusMinutes(5));

        service.updateActivity(trackedRequest);
        service.setCurrentGame(trackedRequest, "game-99");

        assertEquals("game-99", sessionInfo.getCurrentGameId());
        assertTrue(sessionInfo.getLastActivity().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("cleanupInactiveSessions_and_removeSession_pruneTrackedSessions")
    void cleanupInactiveSessions_and_removeSession_pruneTrackedSessions() {
        SessionTrackingService service = new SessionTrackingService();

        MockHttpServletRequest staleRequest = new MockHttpServletRequest();
        staleRequest.setSession(new MockHttpSession(null, "stale-session"));
        staleRequest.setRemoteAddr("192.0.2.10");
        service.trackSession(staleRequest, "1", "alice");

        MockHttpServletRequest activeRequest = new MockHttpServletRequest();
        activeRequest.setSession(new MockHttpSession(null, "active-session"));
        activeRequest.setRemoteAddr("192.0.2.11");
        service.trackSession(activeRequest, "2", "bob");

        service.getSessionInfo("stale-session").setLastActivity(LocalDateTime.now().minusMinutes(45));
        service.getSessionInfo("active-session").setLastActivity(LocalDateTime.now().minusMinutes(5));

        service.cleanupInactiveSessions(30);

        assertNull(service.getSessionInfo("stale-session"));
        assertNotNull(service.getSessionInfo("active-session"));

        service.removeSession("active-session");

        assertEquals(0, service.getActiveSessionCount());
    }
}
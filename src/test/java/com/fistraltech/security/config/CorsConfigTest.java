package com.fistraltech.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TDD integration tests for the centralised CORS policy.
 *
 * <p>The tests verify that {@code wordai.cors.allowed-origins} controls which origins
 * receive CORS response headers, replacing the per-controller {@code @CrossOrigin(origins="*")}
 * annotations.
 *
 * <p>All three tests are RED before the {@code CorsConfigurationSource} bean is added to
 * {@code SecurityConfig}:
 * <ul>
 *   <li>T1 &amp; T3 fail because {@code @CrossOrigin(origins="*")} returns the literal
 *       wildcard {@code *}, not the echoed origin this test expects.
 *   <li>T2 fails because the wildcard allows every origin, so the disallowed origin
 *       still receives an {@code Access-Control-Allow-Origin} header.
 * </ul>
 *
 * <p>Spec: {@code docs/features/cors-policy.spec.md}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Fresh in-memory H2 — never touches the dev file-based DB
        "spring.datasource.url=jdbc:h2:mem:cors_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.h2.console.enabled=false",
        // Only this origin is explicitly allowed — used to verify echo and exclusion
        "wordai.cors.allowed-origins=http://test-origin.example.com"
})
class CorsConfigTest {

    private static final String ALLOWED_ORIGIN     = "http://test-origin.example.com";
    private static final String DISALLOWED_ORIGIN  = "http://evil-site.example.com";

    /** Public endpoint, no auth required — safe for CORS tests. */
    private static final String PUBLIC_PATH = "/api/wordai/dictionaries";

    @Autowired
    private MockMvc mockMvc;

    // -----------------------------------------------------------------------
    // T1 — Preflight from allowed origin receives echoed Access-Control-Allow-Origin
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: OPTIONS preflight from allowed origin returns Access-Control-Allow-Origin header")
    void preflight_allowedOrigin_returnsAllowOriginHeader() throws Exception {
        mockMvc.perform(options(PUBLIC_PATH)
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    // -----------------------------------------------------------------------
    // T2 — Preflight from disallowed origin receives no Access-Control-Allow-Origin
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: OPTIONS preflight from disallowed origin has no Access-Control-Allow-Origin header")
    void preflight_disallowedOrigin_noAllowOriginHeader() throws Exception {
        mockMvc.perform(options(PUBLIC_PATH)
                        .header("Origin", DISALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    // -----------------------------------------------------------------------
    // T3 — Simple GET from allowed origin has echoed Access-Control-Allow-Origin
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: GET from allowed origin reflects origin in Access-Control-Allow-Origin")
    void simpleGet_allowedOrigin_echoesOriginHeader() throws Exception {
        mockMvc.perform(get(PUBLIC_PATH)
                        .header("Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }
}

package com.fistraltech.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * TDD tests for {@link AdminCredentialsValidator}.
 *
 * <p>Spec: {@code docs/features/admin-credentials.spec.md}
 */
@ExtendWith(MockitoExtension.class)
class AdminCredentialsValidatorTest {

    private static final String KNOWN_DEFAULT_PASSWORD = "ChangeMe123!";
    private static final String KNOWN_DEFAULT_EMAIL    = "admin@wordai.local";

    @Mock
    private Environment env;

    // -----------------------------------------------------------------------
    // T1 — prod profile + known-default password → fail fast
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: prod profile with default password throws IllegalStateException")
    void prodProfile_defaultPassword_throwsIllegalStateException() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminCredentialsValidator validator = new AdminCredentialsValidator(env);
        validator.setAdminEmail("secure-admin@example.com");
        validator.setAdminPassword(KNOWN_DEFAULT_PASSWORD);   // still the default

        assertThrows(IllegalStateException.class, validator::validate,
                "Startup must be aborted when the default password is used in prod");
    }

    // -----------------------------------------------------------------------
    // T2 — prod profile + blank email → fail fast
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: prod profile with blank email throws IllegalStateException")
    void prodProfile_blankEmail_throwsIllegalStateException() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminCredentialsValidator validator = new AdminCredentialsValidator(env);
        validator.setAdminEmail("");
        validator.setAdminPassword("S3cure!Pass#2024");

        assertThrows(IllegalStateException.class, validator::validate,
                "Startup must be aborted when the admin email is blank in prod");
    }

    // -----------------------------------------------------------------------
    // T3 — prod profile + strong credentials → passes
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: prod profile with strong credentials passes without exception")
    void prodProfile_strongCredentials_noException() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"prod"});

        AdminCredentialsValidator validator = new AdminCredentialsValidator(env);
        validator.setAdminEmail("ops@example.com");
        validator.setAdminPassword("S3cure!Pass#2024");

        assertDoesNotThrow(validator::validate,
                "Validator must not throw when prod credentials are strong");
    }

    // -----------------------------------------------------------------------
    // T4 — dev profile + known defaults → passes (no fail-fast in dev)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T4: dev profile with default credentials passes without exception")
    void devProfile_defaultCredentials_noException() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"dev"});

        AdminCredentialsValidator validator = new AdminCredentialsValidator(env);
        validator.setAdminEmail(KNOWN_DEFAULT_EMAIL);
        validator.setAdminPassword(KNOWN_DEFAULT_PASSWORD);

        assertDoesNotThrow(validator::validate,
                "Validator must not throw for default credentials when not in prod");
    }
}

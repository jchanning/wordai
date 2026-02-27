package com.fistraltech.security.config;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Startup validator that refuses to let the application start in the {@code prod} Spring
 * profile if admin credentials are blank or still set to the well-known development
 * defaults (defence-in-depth on top of Spring's own placeholder-resolution failure).
 *
 * <p>In non-prod profiles this validator is a no-op, so local development is unaffected.
 *
 * <p>Spec: {@code docs/features/admin-credentials.spec.md}
 */
@Component
public class AdminCredentialsValidator {

    private static final Logger logger = LoggerFactory.getLogger(AdminCredentialsValidator.class);

    /** The well-known default password that must never reach production. */
    static final String KNOWN_DEFAULT_PASSWORD = "ChangeMe123!";

    private final Environment env;

    @Value("${wordai.admin.email}")
    private String adminEmail;

    @Value("${wordai.admin.password}")
    private String adminPassword;

    public AdminCredentialsValidator(Environment env) {
        this.env = env;
    }

    // Setters used by unit tests to inject values without a Spring context.
    void setAdminEmail(String adminEmail)       { this.adminEmail = adminEmail; }
    void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    /**
     * Validates that production-grade credentials are configured before the application
     * finishes starting up.
     *
     * @throws IllegalStateException if the {@code prod} profile is active and the
     *                               admin credentials are blank or equal to the known
     *                               development defaults.
     */
    @PostConstruct
    public void validate() {
        if (!isProdProfile()) {
            logger.debug("AdminCredentialsValidator: non-prod profile detected — skipping check");
            return;
        }

        if (adminEmail == null || adminEmail.isBlank()) {
            throw new IllegalStateException(
                    "SECURITY: wordai.admin.email must not be blank in the prod profile. "
                    + "Set the WORDAI_ADMIN_EMAIL environment variable.");
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "SECURITY: wordai.admin.password must not be blank in the prod profile. "
                    + "Set the WORDAI_ADMIN_PASSWORD environment variable.");
        }

        if (KNOWN_DEFAULT_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "SECURITY: wordai.admin.password is still set to the well-known default '"
                    + KNOWN_DEFAULT_PASSWORD + "'. "
                    + "Set a strong password via the WORDAI_ADMIN_PASSWORD environment variable.");
        }

        logger.info("AdminCredentialsValidator: prod credential check passed.");
    }

    private boolean isProdProfile() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }
}

package com.fistraltech.security.config;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fistraltech.security.model.User;
import com.fistraltech.security.repository.UserRepository;

/**
 * Seeds or upgrades the configured administrative account during application startup.
 *
 * <p>The initializer ensures the configured admin identity exists and has the
 * {@code ROLE_ADMIN} authority, without overwriting an existing user unnecessarily.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${wordai.admin.email}")
    private String adminEmail;

    @Value("${wordai.admin.username}")
    private String adminUsername;

    @Value("${wordai.admin.password}")
    private String adminPassword;

    @Value("${wordai.admin.fullName}")
    private String adminFullName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Ensures the configured admin account exists and carries {@code ROLE_ADMIN}.
     *
     * @param args application startup arguments
     */
    @Override
    public void run(ApplicationArguments args) {
        Optional<User> existingByEmail = userRepository.findByEmail(adminEmail);
        if (existingByEmail.isPresent()) {
            User admin = existingByEmail.get();
            if (!admin.hasRole("ROLE_ADMIN")) {
                admin.addRole("ROLE_ADMIN");
                userRepository.save(admin);
                logger.info("Upgraded existing user '{}' to ROLE_ADMIN", admin.getUsername());
            } else {
                logger.info("Admin user '{}' already exists with ROLE_ADMIN", admin.getUsername());
            }
            return;
        }

        Optional<User> existingByUsername = userRepository.findByUsername(adminUsername);
        if (existingByUsername.isPresent()) {
            User admin = existingByUsername.get();
            if (!admin.hasRole("ROLE_ADMIN")) {
                admin.addRole("ROLE_ADMIN");
                userRepository.save(admin);
                logger.info("Upgraded existing user '{}' to ROLE_ADMIN", admin.getUsername());
            }
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail);
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setFullName(adminFullName);
        admin.setProvider("local");
        admin.setCreatedAt(LocalDateTime.now());
        admin.addRole("ROLE_ADMIN");
        admin.setEnabled(true);

        userRepository.save(admin);
        logger.info("Created default admin user '{}' with ROLE_ADMIN", adminUsername);
    }
}

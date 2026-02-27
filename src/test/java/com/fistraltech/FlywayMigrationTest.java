package com.fistraltech;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * TDD integration tests for the Flyway baseline migration.
 *
 * <p>These tests start a fresh in-memory H2 database, run Flyway, and then assert that
 * Hibernate's {@code ddl-auto=validate} succeeds — proving that the migration scripts
 * produce a schema that exactly matches the JPA entity definitions.
 *
 * <p>Spec: {@code docs/features/flyway-schema-migration.spec.md}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@TestPropertySource(properties = {
        // Fresh in-memory H2 — never touches the dev file-based DB
        "spring.datasource.url=jdbc:h2:mem:flyway_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        // Flyway creates the schema; Hibernate only validates it
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        // H2 console not needed in tests
        "spring.h2.console.enabled=false"
})
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    // -----------------------------------------------------------------------
    // T1 — Spring context loads (Flyway + validate)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T1: Spring context starts successfully with Flyway migration and ddl-auto=validate")
    void contextLoads() {
        // If we reach this line, the Spring context started, Flyway ran, and
        // Hibernate validated the full schema without error.
        assertTrue(true, "Spring context must load without exception");
    }

    // -----------------------------------------------------------------------
    // T2 — V1 migration creates the `users` table
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T2: V1 migration creates the 'users' table")
    void v1Migration_createsUsersTable() throws Exception {
        assertTrue(tableExists("USERS"), "users table must be created by V1 migration");
    }

    // -----------------------------------------------------------------------
    // T3 — V1 migration creates the `user_roles` table
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T3: V1 migration creates the 'user_roles' table")
    void v1Migration_createsUserRolesTable() throws Exception {
        assertTrue(tableExists("USER_ROLES"), "user_roles table must be created by V1 migration");
    }

    // -----------------------------------------------------------------------
    // T4 — V1 migration creates the `player_games` table
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T4: V1 migration creates the 'player_games' table")
    void v1Migration_createsPlayerGamesTable() throws Exception {
        assertTrue(tableExists("PLAYER_GAMES"), "player_games table must be created by V1 migration");
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private boolean tableExists(String tableName) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return rs.next();
            }
        }
    }
}

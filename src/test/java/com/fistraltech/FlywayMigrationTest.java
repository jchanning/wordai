package com.fistraltech;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
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
@DisplayName("FlywayMigration Tests")
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
        assertTrue(columnExists("PLAYER_GAMES", "CLIENT_IP_ADDRESS"),
                "player_games must include client_ip_address for anonymous activity");
    }

    // -----------------------------------------------------------------------
    // T5 — V2 migration creates the `active_game_sessions` table
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("T5: V2 migration creates the 'active_game_sessions' table")
    void v2Migration_createsActiveGameSessionsTable() throws Exception {
        assertTrue(tableExists("ACTIVE_GAME_SESSIONS"),
                "active_game_sessions table must be created by V2 migration");
    }

    @Test
    @DisplayName("T6: active_game_sessions table includes the browser_session_id column")
    void v3Migration_addsBrowserSessionIdColumn() throws Exception {
        assertTrue(columnExists("ACTIVE_GAME_SESSIONS", "BROWSER_SESSION_ID"),
                "active_game_sessions must include browser_session_id for tab isolation");
    }

        @Test
        @DisplayName("T7: legacy schema with active_game_sessions baselines at V2 and applies V3 and V4")
        void legacySchemaWithActiveSessions_baselinesAtV2_andAppliesV3AndV4() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:flyway_legacy_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "");
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE users ("
                + "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                + "email VARCHAR(255) NOT NULL, "
                + "username VARCHAR(255), "
                + "password VARCHAR(255), "
                + "full_name VARCHAR(255), "
                + "provider VARCHAR(255), "
                + "provider_id VARCHAR(255), "
                + "created_at TIMESTAMP(6), "
                + "last_login TIMESTAMP(6), "
                + "enabled BOOLEAN NOT NULL DEFAULT TRUE, "
                + "CONSTRAINT uq_users_email UNIQUE (email), "
                + "CONSTRAINT uq_users_username UNIQUE (username))");
            statement.execute("CREATE TABLE user_roles ("
                + "user_id BIGINT NOT NULL, "
                + "role VARCHAR(255), "
                + "CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id))");
            statement.execute("CREATE TABLE player_games ("
                + "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                + "user_id BIGINT NOT NULL, "
                + "game_id VARCHAR(50) NOT NULL, "
                + "target_word VARCHAR(20) NOT NULL, "
                + "word_length INTEGER, "
                + "dictionary_id VARCHAR(50), "
                + "guess_words VARCHAR(500), "
                + "guess_responses VARCHAR(500), "
                + "result VARCHAR(10) NOT NULL, "
                + "attempts_used INTEGER, "
                + "max_attempts INTEGER, "
                + "completed_at TIMESTAMP(6) NOT NULL)");
            statement.execute("CREATE TABLE active_game_sessions ("
                + "game_id VARCHAR(36) NOT NULL, "
                + "user_id BIGINT NOT NULL, "
                + "dictionary_id VARCHAR(50) NOT NULL, "
                + "target_word VARCHAR(20) NOT NULL, "
                + "strategy VARCHAR(50) NOT NULL DEFAULT 'RANDOM', "
                + "guess_words VARCHAR(500) NOT NULL DEFAULT '', "
                + "guess_responses VARCHAR(500) NOT NULL DEFAULT '', "
                + "status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', "
                + "created_at TIMESTAMP(6) NOT NULL, "
                + "updated_at TIMESTAMP(6) NOT NULL, "
                + "PRIMARY KEY (game_id))");
            statement.execute("CREATE INDEX idx_ags_user_status ON active_game_sessions (user_id, status)");
        }

        Flyway.configure()
            .dataSource(jdbcUrl, "sa", "")
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("2")
            .load()
            .migrate();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            assertTrue(columnExists(conn, "ACTIVE_GAME_SESSIONS", "BROWSER_SESSION_ID"),
                "legacy databases with active_game_sessions must migrate cleanly to V3");
            assertTrue(columnExists(conn, "PLAYER_GAMES", "CLIENT_IP_ADDRESS"),
                "legacy databases must migrate cleanly to support anonymous activity tracking");
            assertTrue(isColumnNullable(conn, "PLAYER_GAMES", "USER_ID"),
                "legacy databases must allow anonymous player_games rows with null user_id");
        }
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

    private boolean columnExists(String tableName, String columnName) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            return columnExists(conn, tableName, columnName);
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private boolean isColumnNullable(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next() && rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }
}

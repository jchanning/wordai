-- ============================================================================
-- V2__add_active_game_sessions.sql  —  Active game session persistence
-- ============================================================================
-- Stores in-progress games for authenticated users so they survive
-- server restarts.  Rows are upserted on each guess and deleted when
-- a game is explicitly removed or when it ends and has been persisted
-- to player_games.
--
-- Only authenticated users have rows here; guest sessions live only
-- in the in-memory Caffeine cache.
-- ============================================================================

CREATE TABLE active_game_sessions (
    game_id         VARCHAR(36)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    dictionary_id   VARCHAR(50)  NOT NULL,
    target_word     VARCHAR(20)  NOT NULL,
    strategy        VARCHAR(50)  NOT NULL DEFAULT 'RANDOM',
    guess_words     VARCHAR(500) NOT NULL DEFAULT '',
    guess_responses VARCHAR(500) NOT NULL DEFAULT '',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP(6) NOT NULL,
    updated_at      TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (game_id)
);

CREATE INDEX idx_ags_user_status ON active_game_sessions (user_id, status);

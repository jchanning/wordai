ALTER TABLE active_game_sessions
ADD COLUMN browser_session_id VARCHAR(100) NOT NULL DEFAULT '';

CREATE INDEX idx_ags_user_dict_browser_status
ON active_game_sessions (user_id, dictionary_id, browser_session_id, status);
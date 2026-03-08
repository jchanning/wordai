ALTER TABLE player_games ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE player_games ADD COLUMN IF NOT EXISTS client_ip_address VARCHAR(45);
CREATE INDEX IF NOT EXISTS idx_player_games_client_ip_address ON player_games (client_ip_address);

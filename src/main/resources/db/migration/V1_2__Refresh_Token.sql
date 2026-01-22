-- ==============================================
-- V1_2__Refresh_Token.sql
-- Add refresh_token table for deployments baselined
-- at version 1 before the JWT refresh token table existed.
-- ==============================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGSERIAL NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,

    CONSTRAINT uk_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_token(expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_token_active ON refresh_token(user_id, revoked) WHERE revoked = FALSE;


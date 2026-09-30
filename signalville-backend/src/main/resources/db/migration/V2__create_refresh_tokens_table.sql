-- Strategie JWT B : access token court (15 min, non persiste) + refresh token
-- persiste en base (7 jours) pour permettre la revocation au logout.
CREATE TABLE refresh_tokens (
    id          UUID            PRIMARY KEY,
    token       VARCHAR(512)    NOT NULL,
    user_id     UUID            NOT NULL,
    issued_at   TIMESTAMP       NOT NULL DEFAULT now(),
    expires_at  TIMESTAMP       NOT NULL,
    revoked_at  TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

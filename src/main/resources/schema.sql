CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,
    user_id     BIGINT          NOT NULL,
    refresh_token VARCHAR(512) NOT NULL UNIQUE,
    tenant_id   VARCHAR(64)     NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_user_id
    ON auth_refresh_tokens (user_id);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_tenant_id
    ON auth_refresh_tokens (tenant_id);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_refresh_token
    ON auth_refresh_tokens (refresh_token);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_expires_at
    ON auth_refresh_tokens (expires_at);

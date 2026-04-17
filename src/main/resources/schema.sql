-- Users 테이블 (auth-service 로컬 읽기 모델)
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    phone       VARCHAR(20),
    role        VARCHAR(20)     NOT NULL DEFAULT 'CONSUMER',
    tenant_id   VARCHAR(64),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    INDEX idx_users_email (email),
    INDEX idx_users_tenant_id (tenant_id)
);

-- Refresh Token 테이블
CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    refresh_token   VARCHAR(512)    NOT NULL UNIQUE,
    tenant_id       VARCHAR(64)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP       NOT NULL,
    INDEX idx_auth_refresh_tokens_user_id (user_id),
    INDEX idx_auth_refresh_tokens_tenant_id (tenant_id),
    INDEX idx_auth_refresh_tokens_refresh_token (refresh_token),
    INDEX idx_auth_refresh_tokens_expires_at (expires_at)
);

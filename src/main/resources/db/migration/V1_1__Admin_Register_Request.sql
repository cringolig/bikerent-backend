-- ==============================================
-- V1_1__Admin_Register_Request.sql
-- Add admin registration request table for deployments
-- that were baselined at version 1 before this table existed.
-- ==============================================

CREATE TABLE IF NOT EXISTS admin_register_request (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by BIGINT,

    CONSTRAINT uk_admin_request_username UNIQUE (username),
    CONSTRAINT uk_admin_request_email UNIQUE (email),
    CONSTRAINT fk_admin_request_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_admin_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_admin_request_status ON admin_register_request(status);
CREATE INDEX IF NOT EXISTS idx_admin_request_created ON admin_register_request(created_at);


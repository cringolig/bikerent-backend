-- ==============================================
-- V2__Performance_Indexes.sql
-- Additional indexes for query optimization
-- ==============================================

-- Composite indexes for common queries
CREATE INDEX IF NOT EXISTS idx_rental_user_status ON rental(user_id, status);
CREATE INDEX IF NOT EXISTS idx_rental_bicycle_status ON rental(bicycle_id, status);
CREATE INDEX IF NOT EXISTS idx_payment_user_date ON payment(user_id, payment_date DESC);
CREATE INDEX IF NOT EXISTS idx_repair_bicycle_status ON repair(bicycle_id, status);

-- Partial indexes for active records
CREATE INDEX IF NOT EXISTS idx_active_rentals ON rental(user_id) WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_available_bicycles ON bicycle(station_id) WHERE status = 'AVAILABLE';
CREATE INDEX IF NOT EXISTS idx_pending_admin_requests ON admin_register_request(created_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_valid_refresh_tokens ON refresh_token(user_id, expires_at) WHERE revoked = FALSE;

-- BRIN indexes for timestamp columns (better for large tables)
CREATE INDEX IF NOT EXISTS idx_rental_started_brin ON rental USING BRIN(rental_started_at);
CREATE INDEX IF NOT EXISTS idx_payment_date_brin ON payment USING BRIN(payment_date);
CREATE INDEX IF NOT EXISTS idx_audit_created_brin ON audit_log USING BRIN(created_at);

-- Index for bicycles needing service
CREATE INDEX IF NOT EXISTS idx_bicycle_needs_service ON bicycle(mileage) WHERE mileage > 50;

-- Hash index for token lookups
CREATE INDEX IF NOT EXISTS idx_refresh_token_hash ON refresh_token USING HASH(token);

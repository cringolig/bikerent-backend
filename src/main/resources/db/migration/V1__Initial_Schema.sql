-- ==============================================
-- V1__Initial_Schema.sql
-- Initial database schema for BikeRent
-- ==============================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ==============================================
-- USERS
-- ==============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    password VARCHAR(255) NOT NULL,
    user_status VARCHAR(20) DEFAULT 'ACTIVE',
    balance BIGINT DEFAULT 0 CHECK (balance >= 0 AND balance <= 999999),
    debt BIGINT DEFAULT 0 CHECK (debt >= 0 AND debt <= 999999),
    registration_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'TECH', 'ADMIN'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(user_status);

-- ==============================================
-- STATIONS
-- ==============================================
CREATE TABLE IF NOT EXISTS station (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude >= -90 AND latitude <= 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude >= -180 AND longitude <= 180),
    available_bicycles BIGINT DEFAULT 0 CHECK (available_bicycles >= 0),
    
    CONSTRAINT uk_station_name UNIQUE (name)
);

CREATE INDEX idx_station_name ON station(name);
CREATE INDEX idx_station_coordinates ON station(latitude, longitude);

-- ==============================================
-- BICYCLES
-- ==============================================
CREATE TABLE IF NOT EXISTS bicycle (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    model VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    station_id BIGINT,
    last_service_date TIMESTAMP,
    mileage BIGINT DEFAULT 0 CHECK (mileage >= 0),
    
    CONSTRAINT fk_bicycle_station FOREIGN KEY (station_id) REFERENCES station(id) ON DELETE SET NULL,
    CONSTRAINT chk_bicycle_type CHECK (type IN ('ROAD', 'MOUNTAIN', 'HYBRID', 'ELECTRIC', 'CITY')),
    CONSTRAINT chk_bicycle_status CHECK (status IN ('AVAILABLE', 'RENTED', 'UNAVAILABLE'))
);

CREATE INDEX idx_bicycle_status ON bicycle(status);
CREATE INDEX idx_bicycle_station ON bicycle(station_id);
CREATE INDEX idx_bicycle_type ON bicycle(type);
CREATE INDEX idx_bicycle_mileage ON bicycle(mileage);

-- ==============================================
-- RENTALS
-- ==============================================
CREATE TABLE IF NOT EXISTS rental (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    user_id BIGINT NOT NULL,
    bicycle_id BIGINT NOT NULL,
    start_station_id BIGINT NOT NULL,
    end_station_id BIGINT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    rental_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rental_ended_at TIMESTAMP,
    cost DECIMAL(10, 2) DEFAULT 0.00 CHECK (cost >= 0),
    
    CONSTRAINT fk_rental_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_bicycle FOREIGN KEY (bicycle_id) REFERENCES bicycle(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_start_station FOREIGN KEY (start_station_id) REFERENCES station(id) ON DELETE CASCADE,
    CONSTRAINT fk_rental_end_station FOREIGN KEY (end_station_id) REFERENCES station(id) ON DELETE SET NULL,
    CONSTRAINT chk_rental_status CHECK (status IN ('ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT chk_rental_dates CHECK (rental_ended_at IS NULL OR rental_ended_at >= rental_started_at)
);

CREATE INDEX idx_rental_user ON rental(user_id);
CREATE INDEX idx_rental_bicycle ON rental(bicycle_id);
CREATE INDEX idx_rental_status ON rental(status);
CREATE INDEX idx_rental_started ON rental(rental_started_at);
CREATE INDEX idx_rental_active_bicycle ON rental(bicycle_id, status) WHERE status = 'ACTIVE';

-- ==============================================
-- PAYMENTS
-- ==============================================
CREATE TABLE IF NOT EXISTS payment (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount BIGINT NOT NULL CHECK (amount >= 1 AND amount <= 999999),
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_user ON payment(user_id);
CREATE INDEX idx_payment_date ON payment(payment_date);

-- ==============================================
-- TECHNICIANS
-- ==============================================
CREATE TABLE IF NOT EXISTS technician (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    
    CONSTRAINT uk_technician_phone UNIQUE (phone)
);

CREATE INDEX idx_technician_name ON technician(name);
CREATE INDEX idx_technician_specialization ON technician(specialization);

-- ==============================================
-- REPAIRS
-- ==============================================
CREATE TABLE IF NOT EXISTS repair (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT DEFAULT 0,
    bicycle_id BIGINT NOT NULL,
    technician_id BIGINT,
    description VARCHAR(500) NOT NULL,
    repair_started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    repair_ended_at TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    
    CONSTRAINT fk_repair_bicycle FOREIGN KEY (bicycle_id) REFERENCES bicycle(id) ON DELETE CASCADE,
    CONSTRAINT fk_repair_technician FOREIGN KEY (technician_id) REFERENCES technician(id) ON DELETE SET NULL,
    CONSTRAINT chk_repair_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_repair_dates CHECK (repair_ended_at IS NULL OR repair_ended_at >= repair_started_at)
);

CREATE INDEX idx_repair_bicycle ON repair(bicycle_id);
CREATE INDEX idx_repair_technician ON repair(technician_id);
CREATE INDEX idx_repair_status ON repair(status);
CREATE INDEX idx_repair_started ON repair(repair_started_at);

-- ==============================================
-- ADMIN REGISTER REQUESTS
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

CREATE INDEX idx_admin_request_status ON admin_register_request(status);
CREATE INDEX idx_admin_request_created ON admin_register_request(created_at);

-- ==============================================
-- REFRESH TOKENS (for new JWT flow)
-- ==============================================
CREATE TABLE IF NOT EXISTS refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    
    CONSTRAINT uk_refresh_token UNIQUE (token),
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
CREATE INDEX idx_refresh_token_expires ON refresh_token(expires_at);
CREATE INDEX idx_refresh_token_active ON refresh_token(user_id, revoked) WHERE revoked = FALSE;

-- ==============================================
-- AUDIT LOG (optional, for tracking changes)
-- ==============================================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    user_id BIGINT,
    username VARCHAR(50),
    old_value JSONB,
    new_value JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_log(user_id);
CREATE INDEX idx_audit_created ON audit_log(created_at);
CREATE INDEX idx_audit_action ON audit_log(action);

-- ==============================================
-- FUNCTIONS
-- ==============================================

-- Function to update station available_bicycles count
CREATE OR REPLACE FUNCTION update_station_bicycle_count()
RETURNS TRIGGER AS $$
BEGIN
    -- Update old station count (if changed)
    IF OLD.station_id IS NOT NULL AND (NEW.station_id IS DISTINCT FROM OLD.station_id OR NEW.status IS DISTINCT FROM OLD.status) THEN
        UPDATE station 
        SET available_bicycles = (
            SELECT COUNT(*) FROM bicycle 
            WHERE station_id = OLD.station_id AND status = 'AVAILABLE'
        )
        WHERE id = OLD.station_id;
    END IF;
    
    -- Update new station count
    IF NEW.station_id IS NOT NULL THEN
        UPDATE station 
        SET available_bicycles = (
            SELECT COUNT(*) FROM bicycle 
            WHERE station_id = NEW.station_id AND status = 'AVAILABLE'
        )
        WHERE id = NEW.station_id;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update station counts
DROP TRIGGER IF EXISTS trg_bicycle_station_count ON bicycle;
CREATE TRIGGER trg_bicycle_station_count
    AFTER INSERT OR UPDATE OF station_id, status ON bicycle
    FOR EACH ROW
    EXECUTE FUNCTION update_station_bicycle_count();

-- Function to clean up expired refresh tokens
CREATE OR REPLACE FUNCTION cleanup_expired_tokens()
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM refresh_token 
    WHERE expires_at < CURRENT_TIMESTAMP OR revoked = TRUE;
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ==============================================
-- COMMENTS
-- ==============================================
COMMENT ON TABLE users IS 'System users including customers, technicians, and administrators';
COMMENT ON TABLE station IS 'Bike rental stations with geolocation';
COMMENT ON TABLE bicycle IS 'Bicycles available for rent';
COMMENT ON TABLE rental IS 'Rental transactions tracking';
COMMENT ON TABLE payment IS 'User balance top-up transactions';
COMMENT ON TABLE technician IS 'Technicians who perform bicycle maintenance';
COMMENT ON TABLE repair IS 'Bicycle repair and maintenance records';
COMMENT ON TABLE refresh_token IS 'JWT refresh tokens for secure authentication';
COMMENT ON TABLE audit_log IS 'Audit trail for tracking system changes';

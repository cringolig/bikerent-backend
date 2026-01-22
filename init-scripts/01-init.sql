-- ==============================================
-- Database initialization script
-- This runs on first container startup only
-- ==============================================

-- Create a dedicated test database for integration tests (if missing)
-- Uses psql meta-command \gexec (supported by docker-entrypoint-initdb.d).
SELECT 'CREATE DATABASE bikerent_test'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'bikerent_test')\gexec

SELECT 'Database initialized successfully' as status;

-- Note: Schema creation is handled by Flyway migrations
-- This script is for any one-time setup tasks

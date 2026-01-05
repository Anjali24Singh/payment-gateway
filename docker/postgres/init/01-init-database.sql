-- PostgreSQL initialization script for Payment Gateway
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional databases for different environments
CREATE DATABASE "payment-gateway-test";
CREATE DATABASE "payment-gateway-staging";

-- Grant permissions to the postgres user (already the owner, but for completeness)
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway" TO postgres;
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway-test" TO postgres;
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway-staging" TO postgres;

-- Connect to the main database and set up extensions
\c "payment-gateway";

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create application user (used by the app)
CREATE USER payment_user WITH PASSWORD 'ajay';
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway" TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway-test" TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE "payment-gateway-staging" TO payment_user;

-- Create a read-only user for reporting (optional)
CREATE USER payment_readonly WITH PASSWORD 'readonly_pass';
GRANT CONNECT ON DATABASE "payment-gateway" TO payment_readonly;
GRANT USAGE ON SCHEMA public TO payment_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO payment_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO payment_readonly;

-- Set up the test database
\c "payment-gateway-test";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Set up the staging database
\c "payment-gateway-staging";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Initialize PostgreSQL database for Agent Application

-- Create database if not exists (this is handled by POSTGRES_DB env var)
-- CREATE DATABASE IF NOT EXISTS agentdb;

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS agent_data;
CREATE SCHEMA IF NOT EXISTS agent_monitoring;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Grant permissions
GRANT ALL PRIVILEGES ON SCHEMA agent_data TO agent;
GRANT ALL PRIVILEGES ON SCHEMA agent_monitoring TO agent;

-- Create indexes for common queries (these will be created by JPA, but we can pre-create some)
-- Note: Actual table creation will be handled by Hibernate DDL

-- Set timezone
SET timezone = 'UTC';
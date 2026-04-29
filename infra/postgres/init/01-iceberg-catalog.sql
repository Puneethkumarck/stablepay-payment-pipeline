-- Iceberg JDBC catalog database
CREATE DATABASE iceberg_catalog;

\c iceberg_catalog;

-- The Iceberg JDBC catalog will create its own tables on first use.
-- This script just ensures the database exists.
GRANT ALL PRIVILEGES ON DATABASE iceberg_catalog TO stablepay;

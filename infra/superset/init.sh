#!/usr/bin/env bash
set -euo pipefail

echo "=== Superset initialization ==="

echo "Upgrading database..."
superset db upgrade

echo "Creating admin user..."
superset fab create-admin \
  --username admin \
  --firstname Admin \
  --lastname User \
  --email admin@stablepay.local \
  --password admin || true

echo "Initializing roles and permissions..."
superset init

echo "Registering Trino Iceberg database..."
superset set-database-uri \
  -d "Trino Iceberg" \
  -u "trino://trino:8080/iceberg" || true

echo "Importing dashboards..."
for dashboard in /app/superset-config/dashboards/*.json; do
  if [ -f "$dashboard" ]; then
    echo "Importing $(basename "$dashboard")..."
    superset import-dashboards -p "$dashboard" || echo "Warning: failed to import $(basename "$dashboard")"
  fi
done

echo "=== Superset initialization complete ==="

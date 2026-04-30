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

echo "Skipping dashboard import."
echo "Dashboard JSON stubs in infra/superset/dashboards/ are placeholders only."
echo "Real dashboards will be exported as versioned ZIPs from the Superset UI"
echo "and imported via 'superset import-dashboards -p <file>.zip' in a later plan."

echo "=== Superset initialization complete ==="

#!/usr/bin/env bash
set -euo pipefail

echo "=== Superset initialization ==="

echo "Upgrading database..."
superset db upgrade

echo "Creating admin user..."
admin_output=$(superset fab create-admin \
  --username admin \
  --firstname Admin \
  --lastname User \
  --email admin@stablepay.local \
  --password admin 2>&1) || {
    if echo "$admin_output" | grep -qiE 'already exists|duplicate'; then
      echo "Admin user already exists — skipping."
    else
      echo "$admin_output" >&2
      echo "ERROR: failed to create admin user" >&2
      exit 1
    fi
  }

echo "Initializing roles and permissions..."
superset init

echo "Registering Trino Iceberg database..."
db_output=$(superset set-database-uri \
  -d "Trino Iceberg" \
  -u "trino://trino:8080/iceberg" 2>&1) || {
    if echo "$db_output" | grep -qiE 'already exists|duplicate'; then
      echo "Trino Iceberg database already registered — skipping."
    else
      echo "$db_output" >&2
      echo "ERROR: failed to register Trino Iceberg database" >&2
      exit 1
    fi
  }

echo "Skipping dashboard import."
echo "Dashboard JSON stubs in infra/superset/dashboards/ are placeholders only."
echo "Real dashboards will be exported as versioned ZIPs from the Superset UI"
echo "and imported via 'superset import-dashboards -p <file>.zip' in a later plan."

echo "=== Superset initialization complete ==="

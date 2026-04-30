#!/usr/bin/env bash
set -euo pipefail

OPENSEARCH_URL="${STBLPAY_OPENSEARCH_URL:-http://localhost:9200}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Waiting for OpenSearch at ${OPENSEARCH_URL}..."
until curl -sf "${OPENSEARCH_URL}/_cluster/health" > /dev/null 2>&1; do
    sleep 2
done
echo "OpenSearch is ready."

for template in transactions flows dlq-events; do
    file="${SCRIPT_DIR}/${template}-index-template.json"
    if [ -f "$file" ]; then
        echo "Creating index template: ${template}..."
        curl -sf -X PUT "${OPENSEARCH_URL}/_index_template/${template}" \
            -H 'Content-Type: application/json' \
            -d @"$file"
        echo ""
    fi
done

for policy in transactions flows dlq-events; do
    file="${SCRIPT_DIR}/ism-${policy}-policy.json"
    if [ -f "$file" ]; then
        echo "Creating ISM policy: ${policy}..."
        curl -sf -X PUT "${OPENSEARCH_URL}/_plugins/_ism/policies/${policy}" \
            -H 'Content-Type: application/json' \
            -d @"$file"
        echo ""
    fi
done

# Create initial rollover-managed write indices with write+read aliases.
# Each {name}-write alias is the rollover write target; {name}-read is for queries.
create_initial_index() {
    local name="$1"
    local initial="${name}-000001"

    if curl -sf "${OPENSEARCH_URL}/${initial}" > /dev/null 2>&1; then
        echo "Initial index ${initial} already exists; skipping."
        return
    fi

    echo "Creating initial index ${initial} with write/read aliases..."
    curl -sf -X PUT "${OPENSEARCH_URL}/${initial}" \
        -H 'Content-Type: application/json' \
        -d "{
            \"aliases\": {
                \"${name}-write\": { \"is_write_index\": true },
                \"${name}-read\": {}
            }
        }"
    echo ""
}

for name in transactions flows dlq-events; do
    create_initial_index "$name"
done

echo "OpenSearch initialization complete."

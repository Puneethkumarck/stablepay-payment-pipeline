set dotenv-load := true
set dotenv-filename := ".env"

# ─── Core lifecycle ────────────────────────────────

# Bring up the core stack
up:
    docker compose -f infra/docker-compose.yml up -d --wait

# Tear down (preserves volumes)
down:
    docker compose -f infra/docker-compose.yml down

# Tear down and destroy all volumes
nuke:
    docker compose -f infra/docker-compose.yml down -v --remove-orphans

# Show service logs
logs *args:
    docker compose -f infra/docker-compose.yml logs {{args}}

# ─── Schema & codegen ─────────────────────────────

# Regenerate Java + Python from Avro schemas
regenerate-schemas:
    ./gradlew :schemas:generateAvroJava
    uv run scripts/regen-py-schemas.py

# ─── Validation ────────────────────────────────────

# Health + topic + SR readiness check
preflight:
    uv run infra/preflight.py

# ─── GitHub Issues ────────────────────────────────

# Create GitHub issues from GSD phase plans (dry-run first)
create-issues phase:
    uv run scripts/create-github-issues.py {{phase}}

# Preview issues without creating them
preview-issues phase:
    uv run scripts/create-github-issues.py {{phase}} --dry-run

# ─── Simulator ────────────────────────────────────

# Run the payment event simulator
simulate *ARGS:
    cd apps/simulator && uv run stablepay-simulate {{ARGS}}

# Run simulator with realistic timing (delay-multiplier=1.0)
simulate-realistic:
    cd apps/simulator && uv run stablepay-simulate --realistic

# Run simulator with periodic burst mode
simulate-burst:
    cd apps/simulator && uv run stablepay-simulate --burst

# ─── Flink Jobs ────────���──────────────────────────

# Build Flink fat JAR
flink-build:
    ./gradlew :apps:flink-jobs:shadowJar

# Submit ingest job to Flink session cluster
flink-submit-ingest:
    docker cp apps/flink-jobs/build/libs/stablepay-flink-jobs.jar stablepay-flink-jobmanager:/opt/flink/usrlib/
    docker exec stablepay-flink-jobmanager flink run -d /opt/flink/usrlib/stablepay-flink-jobs.jar --job-class io.stablepay.flink.IngestJob

# Submit correlator job to Flink session cluster
flink-submit-correlator:
    docker cp apps/flink-jobs/build/libs/stablepay-flink-jobs.jar stablepay-flink-jobmanager:/opt/flink/usrlib/
    docker exec stablepay-flink-jobmanager flink run -d /opt/flink/usrlib/stablepay-flink-jobs.jar --job-class io.stablepay.flink.CorrelatorJob

# Submit aggregation job to Flink session cluster
flink-submit-aggregation:
    docker cp apps/flink-jobs/build/libs/stablepay-flink-jobs.jar stablepay-flink-jobmanager:/opt/flink/usrlib/
    docker exec stablepay-flink-jobmanager flink run -d /opt/flink/usrlib/stablepay-flink-jobs.jar --job-class io.stablepay.flink.AggregationJob

# Submit stuck-withdrawals batch job to Flink
flink-submit-stuck-withdrawals:
    docker cp apps/flink-jobs/build/libs/stablepay-flink-jobs.jar stablepay-flink-jobmanager:/opt/flink/usrlib/
    docker exec stablepay-flink-jobmanager flink run /opt/flink/usrlib/stablepay-flink-jobs.jar --job-class io.stablepay.flink.StuckWithdrawalsJob

# Build and submit all Flink jobs
flink-deploy: flink-build flink-submit-ingest flink-submit-correlator flink-submit-aggregation

# Open Flink Web UI
flink-ui:
    open http://localhost:8082

# ─── OpenSearch ───────────────────────────────────

# Initialize OpenSearch index templates and ISM policies
opensearch-init:
    bash infra/opensearch/init.sh

# ─── Trino & Superset ─────────────────────────────

# Initialize Trino analytics views (runs the SQL inside the trino container so no host CLI is required)
trino-init:
    docker cp infra/trino/analytics-views.sql stablepay-trino:/tmp/analytics-views.sql
    docker exec stablepay-trino trino --server http://localhost:8080 --file /tmp/analytics-views.sql

# Initialize Superset (db upgrade, admin user, dashboards)
superset-init:
    docker exec stablepay-superset bash /app/superset-config/init.sh

# Run a time-travel query (LAK-07 verification)
trino-time-travel version:
    docker exec stablepay-trino trino --server http://localhost:8080 --execute "SELECT count(*) FROM iceberg.facts.fact_transactions FOR VERSION AS OF {{version}}"

# ─── API Service ──────────────────────────────────

# Build the API service jar and Docker image
api-build:
    ./gradlew :apps:api:main:bootJar
    docker compose -f infra/docker-compose.yml build apps-api

# Bring up the API service (waits for healthy)
api-up:
    docker compose -f infra/docker-compose.yml up -d --wait apps-api

# Follow API service logs
api-logs:
    docker compose -f infra/docker-compose.yml logs -f apps-api

# Smoke test: hit actuator health endpoint
api-smoke:
    @echo "Checking API health..."
    curl -fs http://localhost:8080/actuator/health | grep -q '"status":"UP"' && echo "API is healthy" || (echo "API is not healthy" && exit 1)

# ─── Auth Service ─────────────────────────────────

# Build the auth service jar and Docker image
auth-build:
    ./gradlew :apps:auth:main:bootJar
    docker compose -f infra/docker-compose.yml build apps-auth

# Bring up the auth service (waits for healthy)
auth-up:
    docker compose -f infra/docker-compose.yml up -d --wait apps-auth

# Follow auth service logs
auth-logs:
    docker compose -f infra/docker-compose.yml logs -f apps-auth

# ─── DLQ Tools ────────────────────────────────────

# List DLQ entries
dlq-list *ARGS:
    cd apps/dlq-tools && uv run dlq list {{ARGS}}

# Inspect a single DLQ event
dlq-inspect ID:
    cd apps/dlq-tools && uv run dlq inspect {{ID}}

# Replay a single DLQ event
dlq-replay ID *ARGS:
    cd apps/dlq-tools && uv run dlq replay {{ID}} {{ARGS}}

# Replay all events by error class. Pass --dry-run explicitly to preview without producing.
dlq-replay-class CLASS *ARGS:
    cd apps/dlq-tools && uv run dlq replay-class {{CLASS}} {{ARGS}}

# ─── Stubs (expanded in later phases) ─────────────

# Run all tests
test:
    @echo "Test recipes added in later phases"

# Run all CI checks locally
ci-all:
    @echo "CI recipes added in Phase 7"

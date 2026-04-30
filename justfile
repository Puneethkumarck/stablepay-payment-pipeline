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

# ─── Stubs (expanded in later phases) ───���─────────

# Run all tests
test:
    @echo "Test recipes added in later phases"

# Run all CI checks locally
ci-all:
    @echo "CI recipes added in Phase 7"

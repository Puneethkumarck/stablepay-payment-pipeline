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

# ─── Stubs (expanded in later phases) ─────────────

# Run all tests
test:
    @echo "Test recipes added in later phases"

# Run all CI checks locally
ci-all:
    @echo "CI recipes added in Phase 7"

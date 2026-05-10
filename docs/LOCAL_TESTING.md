# Local Testing Guide

How to run the full StablePay Payment Pipeline stack locally with Docker Compose.

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Docker Desktop | 4.30+ | [docker.com](https://docs.docker.com/get-docker/) |
| Java | 25 | `brew install temurin` or SDKMAN |
| Node.js | 22 | `brew install node@22` |
| pnpm | 11.x | `corepack enable && corepack prepare pnpm@latest` |
| uv | latest | `brew install uv` |
| just | latest | `brew install just` |

Ensure Docker has at least **8 GB RAM** allocated (Preferences → Resources).

## Quick Start

```bash
# 1. Build backend JARs
./gradlew :apps:auth:main:bootJar :apps:api:main:bootJar

# 2. Bring up the full stack (postgres, redis, kafka, opensearch, auth, api, web)
make up
# or: just up

# 3. Verify all services are healthy
make status
make api-smoke

# 4. Open the web app
open http://localhost:3000
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Web (Next.js) | 3000 | http://localhost:3000 |
| API (Spring Boot) | 8080 | http://localhost:8080 |
| Auth (JWT issuer) | 9000 | http://localhost:9000 |
| Kafka (plaintext) | 29092 | localhost:29092 |
| Schema Registry | 8081 | http://localhost:8081 |
| OpenSearch | 9200 | http://localhost:9200 |
| PostgreSQL | 5432 | localhost:5432 |
| Redis | 6379 | localhost:6379 |
| MinIO | 9001 | http://localhost:9001 |
| Flink UI | 8082 | http://localhost:8082 |
| Trino | 8083 | http://localhost:8083 |

## Running Tests

### Unit Tests (no Docker required)

```bash
# API unit tests (Java)
make test-api
# or: ./gradlew :apps:api:main:test

# Web unit tests (Vitest)
make test-web
# or: cd apps/web && pnpm exec vitest run
```

### Integration Tests (needs Docker services)

```bash
# Start only infrastructure (no app services)
docker compose -f infra/docker-compose.yml up -d postgres redis kafka schema-registry opensearch

# Run API integration tests (uses Testcontainers — starts its own containers)
./gradlew :apps:api:main:integrationTest
```

### E2E Tests (Playwright — needs full stack)

```bash
# 1. Build and start everything
make build-all
make up

# 2. Wait for services to be healthy
make api-smoke

# 3. Run Playwright E2E
make test-e2e
# or: cd apps/web && pnpm exec playwright test

# 4. Run with headed browser for debugging
cd apps/web && pnpm exec playwright test --headed

# 5. Run a specific spec
cd apps/web && pnpm exec playwright test tests/e2e/transactions.spec.ts
```

### Running E2E Against Local Dev Server

If you prefer using the Next.js dev server instead of the Docker container:

```bash
# Terminal 1: start infra + backend
docker compose -f infra/docker-compose.yml up -d postgres redis kafka schema-registry opensearch apps-auth apps-api

# Terminal 2: start Next.js dev server
cd apps/web && pnpm dev

# Terminal 3: run Playwright (it will use the dev server at localhost:3000)
cd apps/web && pnpm exec playwright test
```

## Demo Accounts

The login page provides pre-filled demo accounts:

| Email | Password | Roles |
|-------|----------|-------|
| alice@stablepay.io | demo1234 | Admin + Customer |
| bob@stablepay.io | demo1234 | Customer only |
| admin@stablepay.io | demo1234 | Admin only |

## Common Workflows

### Regenerate OpenAPI Client

After changing API endpoints:

```bash
# Ensure API is running
make api-smoke

# Regenerate TypeScript client
make web-codegen
```

### Feed Sample Data

```bash
# Run simulator to produce payment events into Kafka
make simulate

# Or with burst mode (periodic spikes)
make simulate-burst
```

### View Logs

```bash
# All services
make logs

# Specific service
make logs S=apps-api
make logs S=kafka
```

### Full Reset

```bash
# Destroy everything including data volumes
make nuke

# Rebuild and restart
make build-all
make up
```

## Troubleshooting

### Kafka fails to start

Kafka 4.0 (KRaft mode) can fail if volumes contain stale cluster metadata.

```bash
# Destroy volumes and restart
make nuke
make up
```

### OpenSearch OOM

OpenSearch defaults to 512m heap. If your machine is constrained:

```bash
# Use CI overrides (256m heap for OpenSearch)
make ci-up
```

### API fails with "connection refused" to Kafka/OpenSearch

Services have healthchecks with `start_period: 30s`. Use `--wait`:

```bash
docker compose -f infra/docker-compose.yml up -d --wait apps-api
```

### Playwright tests time out

Ensure the web app is actually running on port 3000:

```bash
curl -s http://localhost:3000 | head -5
```

If using Docker for the web app, check its logs:

```bash
make logs S=apps-web
```

If running against the dev server, ensure `pnpm dev` is running in another terminal.

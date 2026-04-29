# Setup Guide

## Prerequisites

- Docker Desktop 4.x+ (or Docker Engine 27+) with at least 8GB memory allocated
- Java 25 (via SDKMAN or manual install)
- Gradle 9.x (wrapper included)
- Python 3.13+ with [uv](https://docs.astral.sh/uv/)
- Node.js 22+ with pnpm 10+
- [just](https://just.systems/) 1.49+
- [lefthook](https://github.com/evilmartians/lefthook) 2.x
- [gitleaks](https://github.com/gitleaks/gitleaks) 8.x

## First-Time Setup

1. Clone the repo and enter it
2. `cp .env.example .env`
3. `pnpm install` (installs lefthook + commitlint)
4. `lefthook install`
5. `uv sync` (creates Python venv + installs workspace deps)
6. `just up` (starts Docker Compose stack)
7. `just preflight` (validates everything is healthy)

## Resource Requirements

- **Minimum:** 16GB RAM, 4 CPU cores
- **Recommended:** 32GB RAM, 8 CPU cores
- Docker memory: allocate at least 8GB

## Troubleshooting

### Kafka won't start
Ensure Docker has enough memory. Kafka KRaft needs ~512MB minimum.

### Schema Registry fails to connect
Wait for Kafka to be fully healthy first. SR depends on Kafka via `service_healthy`.

### MinIO bucket creation fails
The `minio-init` service runs once on startup. If it fails, run `just down && just up` to retry.

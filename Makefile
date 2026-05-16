# ─── StablePay Payment Pipeline — Makefile ──────────────────────────────────
# Convenience targets for local development. The canonical tool is `just`
# (see justfile); this Makefile provides make-based equivalents.

COMPOSE := docker compose -f infra/docker-compose.yml
COMPOSE_CI := docker compose -f infra/docker-compose.yml -f infra/docker-compose.ci.yml

.PHONY: help up down nuke logs status \
        build-api build-auth build-web build-all \
        api-up auth-up web-up web-dev \
        preflight api-smoke \
        test-api test-web test-e2e test-all \
        web-codegen opensearch-init \
        simulate simulate-burst

# ─── Help ─────────────────────────────────────────────────────────────────────

help: ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ─── Core Lifecycle ───────────────────────────────────────────────────────────

up: ## Bring up the full infrastructure stack (waits for healthchecks)
	$(COMPOSE) up -d --wait

down: ## Stop all services (preserves volumes)
	$(COMPOSE) down

nuke: ## Stop all services and destroy volumes
	$(COMPOSE) down -v --remove-orphans

logs: ## Tail all service logs (use: make logs S=kafka)
	$(COMPOSE) logs -f $(S)

status: ## Show running services and health status
	$(COMPOSE) ps

# ─── Build ────────────────────────────────────────────────────────────────────

build-api: ## Build the API service JAR + Docker image
	./gradlew :apps:api:main:bootJar
	$(COMPOSE) build apps-api

build-auth: ## Build the Auth service JAR + Docker image
	./gradlew :apps:auth:main:bootJar
	$(COMPOSE) build apps-auth

build-web: ## Build the Web app Docker image
	$(COMPOSE) build apps-web

build-all: build-auth build-api build-web ## Build all application images

# ─── Service Startup ──────────────────────────────────────────────────────────

api-up: ## Start the API service (with dependencies)
	$(COMPOSE) up -d --wait apps-api

auth-up: ## Start the Auth service (with dependencies)
	$(COMPOSE) up -d --wait apps-auth

web-up: ## Start the Web app via Docker (with dependencies)
	$(COMPOSE) up -d --wait apps-web

web-dev: ## Run Next.js dev server locally (no Docker)
	cd apps/web && pnpm dev

# ─── Validation & Smoke Tests ─────────────────────────────────────────────────

preflight: ## Run health + topic + schema registry readiness check
	uv run infra/preflight.py

api-smoke: ## Hit API actuator/health endpoint
	@curl -fs http://localhost:8080/actuator/health | grep -q '"status":"UP"' \
		&& echo "✓ API is healthy" || (echo "✗ API is not healthy" && exit 1)

# ─── Testing ──────────────────────────────────────────────────────────────────

test-api: ## Run API unit + integration tests
	./gradlew :apps:api:main:test :apps:api:main:integrationTest

test-web: ## Run web Vitest unit tests
	cd apps/web && pnpm exec vitest run

test-e2e: ## Run Playwright E2E tests against running stack
	cd apps/web && pnpm exec playwright test

test-all: test-api test-web ## Run all tests (excludes E2E — needs running stack)

# ─── Codegen & Init ───────────────────────────────────────────────────────────

web-codegen: ## Regenerate OpenAPI TypeScript client from running API
	cd apps/web && OPENAPI_INPUT=http://localhost:8080/v3/api-docs pnpm exec openapi-ts

opensearch-init: ## Initialize OpenSearch index templates and ISM policies
	bash infra/opensearch/init.sh

# ─── Simulator ────────────────────────────────────────────────────────────────

simulate: ## Run the payment event simulator
	cd apps/simulator && uv run stablepay-simulate

simulate-burst: ## Run simulator with periodic burst mode
	cd apps/simulator && uv run stablepay-simulate --burst

# ─── CI (local reproduction) ──────────────────────────────────────────────────

ci-up: ## Bring up stack with CI overrides (reduced memory)
	$(COMPOSE_CI) up -d --wait

ci-down: ## Tear down CI stack with volumes
	$(COMPOSE_CI) down -v

# CLAUDE.md — stablepay-payment-pipeline

This file provides workflow guidance to Claude Code when working in this repository. It is loaded into context on every session.

## Project at a glance

`stablepay-payment-pipeline` is a real-time data pipeline for stablecoin and fiat payment events. Five flows (fiat payin/payout, crypto payin/payout, multi-leg payment flow), Avro+Kafka+Flink+Iceberg+OpenSearch+Trino+Superset+Spring Boot+Next.js+LangGraph stack, single-machine Docker Compose deployment, designed as a forkable reference platform.

See `.planning/PROJECT.md` for the full project context, `.planning/REQUIREMENTS.md` for v1 requirements, and `.planning/ROADMAP.md` for phase breakdown.

## GSD workflow

This project uses the GSD (Get Shit Done) workflow. Standard cycle:

1. `/gsd-discuss-phase N` — gather phase context through adaptive questioning
2. `/gsd-plan-phase N` — create detailed phase plan with verification loop
3. `/gsd-execute-phase N` — execute all plans in the phase
4. `/gsd-verify-work` — validate built features against UAT criteria
5. `/gsd-transition` — update PROJECT.md after phase, move to next

`/gsd-progress` shows current state and routes to next action at any time.

## Hard constraints (non-negotiable)

These are loaded from project memory and apply to every Claude Code session:

### 1. No source-platform references

Zero references to the upstream platform whose architecture inspired this project — no name, path, code, comments, commits, schemas, or documentation. Use the renamed domain-native vocabulary throughout. The project is generic and forkable; identifying details must not propagate.

### 2. Java service follows the imported reference standards verbatim

The Java/Spring Boot side (`apps/api/`, `apps/flink-jobs/` where Java) follows the conventions in [`docs/ADR.md`](docs/ADR.md), [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md), [`docs/PROJECT_STRUCTURE.md`](docs/PROJECT_STRUCTURE.md), and [`docs/TESTING_STANDARDS.md`](docs/TESTING_STANDARDS.md) exactly. See [`docs/JAVA_STANDARDS_README.md`](docs/JAVA_STANDARDS_README.md) for placeholder mapping (org=stablepay, domain=payments, service=stablepay-api, error-code prefix=STBLPAY).

Highlights:

- Hexagonal architecture (`application/` / `domain/` / `infrastructure/`) with ArchUnit-enforced layering
- Java 25 LTS, Spring Boot 4.0.x, Gradle 9 Kotlin DSL multi-module (`-api`, `-client`, main)
- Java records with `@Builder(toBuilder = true)` — domain models immutable, return new instances on state change
- Type-safe IDs wrapping UUID (`record TransactionId(UUID value)`)
- `Money(BigDecimal value, CurrencyCode currency)` via `nv-i18n`
- Generic `StateMachine<S, T>` for all status transitions
- Namastack Outbox Starter JDBC (`io.namastack:namastack-outbox-starter-jdbc`) for event publishing — events have `public static final String TOPIC` field
- MapStruct for every layer-boundary mapping (`@Mapper(componentModel = "spring")`)
- Spring Security with `@Secured("ROLE_*")` + `@AuthenticationPrincipal AuthenticatedUser`
- Pessimistic locking for balance ops, optimistic for everything else
- Constructor injection only via `@RequiredArgsConstructor` — never `@Autowired`
- Functional style: streams over loops, Optional pipelines over null checks
- Logging via SLF4J `@Slf4j` — never `System.out`
- Error codes structured as `STBLPAY-XXXX` (4-digit zero-padded)
- Idempotency via `X-Idempotency-Key` header + DB unique constraint

### 3. Testing — golden recursive-comparison rule

Every Java test that verifies an object result MUST construct an expected object and compare with a single `assertThat(...).usingRecursiveComparison()`. Multiple `assertThat` calls on individual fields are forbidden. See [`docs/TESTING_STANDARDS.md`](docs/TESTING_STANDARDS.md) for narrow exceptions (exception assertions, single primitives, collection size+containment, single enum mappings, Optional checks).

Four source sets per Java module: `test/` (unit, JUnit 5 + Mockito BDD + AssertJ), `testFixtures/` (shared fixtures + WireMock stubs), `integration-test/` (Spring + Testcontainers), `business-test/` (full-server E2E).

### 4. Latest stable versions everywhere

Pin latest stable for every dependency. CI runs a weekly `ci-version-bump.yml` workflow that opens auto-PRs with version bumps. Document any pin caused by a known incompatibility in `docs/STACK.md` with a one-line justification.

### 5. Money never as float

Domain layer uses `Money(BigDecimal, CurrencyCode)`. Storage (OpenSearch + Iceberg) uses `long` micros + `keyword` currency. Conversion happens at the infrastructure-layer mapper. Never use `float` or `double` for money in any layer.

### 6. PII masking enforced everywhere

Logback masking pattern + structlog processor + Flink LogEventMasker + OTEL span filter strip `iban`, `email`, `name`, `phone`, `address` fields before emission. CI grep-step on E2E logs fails the build if known-PII test fixtures appear. Never log a customer-identifying field directly — use the masked field accessors.

### 7. Customer scope enforced server-side

Repository adapters apply `customer_id` filter from the JWT principal automatically. Agent guardrail rule 7 refuses cross-customer queries. Integration tests assert cross-customer requests return 404 (not 403, to avoid enumeration). Frontend cannot bypass — server is authoritative.

## Stack snapshot

See `docs/STACK.md` (auto-updated by `ci-version-bump.yml`) for the full pin list. Key versions:

- Java 25 LTS, Spring Boot 4.0.x, Spring Cloud 2025.x, Gradle 9 Kotlin DSL
- Python 3.13+ with `uv`, ruff
- TypeScript 5.7+ strict, Next.js 15 App Router, React 19, Tailwind 4, shadcn/ui
- Apache Kafka 4.0+ (KRaft), Confluent Schema Registry, Apache Flink 2.x, Apache Iceberg 1.8+, Trino 470+, OpenSearch 2.18+, Apache Superset 4.x, PostgreSQL 17, MinIO latest
- LangGraph 2.0+, LangChain 0.3+, langchain-mcp-adapters, OpenAI SDK latest, Langfuse v3 self-hosted

## Repository layout

```
stablepay-payment-pipeline/
├── justfile                              # top-level orchestration
├── build.gradle.kts / settings.gradle.kts # Gradle multi-module root
├── apps/
│   ├── simulator/                        # Python — Faker-driven event producer
│   ├── api/                              # Java/Spring Boot — public API + admin + agent endpoints
│   ├── flink-jobs/                       # Java — Flink jobs (consumer + sinks + correlator + aggs)
│   ├── web/                              # Next.js 15 App Router web app
│   ├── llm-agent/                        # Python LangGraph agent + FastAPI surface
│   ├── agent-tools-mcp/                  # Python MCP server exposing the three agent tools
│   ├── auth/                             # Python or Java — JWT issuer
│   ├── dlq-tools/                        # Python CLI for DLQ replay
│   └── lakehouse-jobs/                   # Python — Iceberg maintenance jobs
├── schemas/                              # Avro source-of-truth (.avsc)
├── infra/
│   ├── docker-compose.yml                # core stack
│   ├── docker-compose.observability.yml  # opt-in observability
│   ├── opensearch/                       # index templates, ILM policies, security config
│   ├── trino/                            # catalog config, query profiles
│   └── postgres/                         # init scripts (catalog, auth, idempotency)
├── ops/
│   ├── prometheus/                       # rules + alerts
│   ├── grafana/dashboards/               # JSON dashboards
│   └── alertmanager/                     # routing config
├── tests/
│   └── e2e/                              # nightly pipeline E2E orchestrator
├── docs/                                 # ARCHITECTURE, EVENT-MODEL, EXTENDING, RUNBOOK, STACK, EVAL-METHODOLOGY, PRIVACY
├── .planning/                            # GSD workflow artifacts (PROJECT, REQUIREMENTS, ROADMAP, STATE, phase plans)
└── .github/workflows/                    # 10 path-filtered CI pipelines
```

## Common commands

```
just up                  # bring up the core stack
just up-observability    # add the observability overlay
just down                # tear down
just simulate            # run the simulator at default rate
just simulate --burst    # run with periodic burst mode
just regenerate-schemas  # codegen from schemas/*.avsc
just preflight           # health check + version drift check
just test                # run all tests across all components
just ci-all              # run CI checks locally before pushing
just dlq-list            # list DLQ entries
just dlq-replay <id>     # replay a single DLQ entry
```

## Documentation links

**Java service standards (already imported):**
- `docs/ADR.md` — 21-section architecture decision record
- `docs/CODING_STANDARDS.md` — hexagonal layout, Lombok policy, naming, functional style
- `docs/PROJECT_STRUCTURE.md` — multi-module Gradle layout + package tree + file-placement decision tree
- `docs/TESTING_STANDARDS.md` — four-source-set pyramid + golden recursive-comparison rule + ArchUnit
- `docs/JAVA_STANDARDS_README.md` — placeholder mapping for this project

**Project documentation (built during phases):**
- `docs/ARCHITECTURE.md` — annotated architecture diagram + data flow (Phase 8)
- `docs/EVENT-MODEL.md` — full state machines + topic listing + event examples (Phase 8)
- `docs/EXTENDING.md` — forking guide with worked example (Phase 8)
- `docs/RUNBOOK.md` — alert response procedures (Phase 6/8)
- `docs/STACK.md` — auto-updated version pin list (Phase 7)
- `docs/EVAL-METHODOLOGY.md` — agent eval rubrics + reproduction steps (Phase 8)
- `docs/PRIVACY.md` — PII inventory + masking + retention (Phase 6)

---

When in doubt, defer to `.planning/PROJECT.md`, the stablebridge-tx-recovery standards docs, and the GSD workflow.

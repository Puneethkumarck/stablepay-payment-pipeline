# REQUIREMENTS — stablepay-payment-pipeline

> v1 requirements with REQ-IDs for traceability. Categories: EVT, ING, STM, DLQ, LAK, SCH, API, WEB, AGT, EVL, OBS, CI, TST, SEC, PLT, DEP. Phase mapping in `## Traceability` section (filled by `ROADMAP.md`).

---

## v1 Requirements

### EVT — Events, Schemas, Registry

- [ ] **EVT-01**: Avro schemas authored under `schemas/` for every Kafka topic (16 topics across the five flows + DLQs + signing/screening/approval), with shared `EventEnvelope` record carrying `event_id`, `event_time`, `ingest_time`, `schema_version`, `flow_id`, `correlation_id`, `trace_id`.
- [ ] **EVT-02**: Confluent Schema Registry running in Compose with all subjects registered under `BACKWARD` compatibility; CI step validates new schema versions against the last registered version.
- [ ] **EVT-03**: Java + Python codegen produced from the Avro source-of-truth on every build; codegen output committed and CI fails if regenerated output diverges.
- [ ] **EVT-04**: Renamed domain-native vocabulary applied throughout (no source-platform references); five flow types modeled — `payment.payin.fiat`, `payment.payin.crypto`, `payment.payout.fiat`, `payment.payout.crypto`, `payment.flow` (multi-leg orchestration).
- [ ] **EVT-05**: High-fidelity state machines preserved verbatim with renamed enum values: full fiat-payout 30+ internal status set including dual-approval / compliance-screening / partner-routing / ledger-suspense / refund / confiscation paths; full crypto TransferStatus + TransferLifecycleState; chain stuck/RBF/replacement events.

### ING — Producer / Simulator

- [ ] **ING-01**: Python simulator (`apps/simulator/`) drives a deterministic state machine per flow type and emits Avro-encoded events to Kafka via the idempotent transactional producer.
- [ ] **ING-02**: Sustains ~200 events/sec aggregate across topics with weighted distribution per flow type; supports a `--burst` mode that injects 2000 evt/s × 30s every ~10 minutes.
- [ ] **ING-03**: Multi-leg `payment_flow` emission — simulator generates a parent `flow_id` UUID, propagates it on every child event (payin → trade → payout legs), and emits the parent `payment.flow.v1` lifecycle as legs progress.
- [ ] **ING-04**: Pluggable `EventSource` interface with three implementations: `FakerSource` (default), `ReplaySource` (replay from Iceberg `raw_*` table dump), `ExternalKafkaSource` (stub + docs only in v1).
- [ ] **ING-05**: Per-event partition key set to the natural entity reference (`hash` for chain events, `payout_reference`/`flow_id`/`trade_reference`/etc. for the rest) to preserve per-entity ordering.

### STM — Stream Processing (Flink)

- [ ] **STM-01**: Apache Flink 2.x jobs (`apps/flink-jobs/`) consume every Kafka topic, validate the Avro envelope, enrich, and dual-write to Iceberg + OpenSearch.
- [ ] **STM-02**: Event-time semantics with `WatermarkStrategy.forBoundedOutOfOrderness(60s) + withIdleness(60s)`; watermark drives all windowed operations.
- [ ] **STM-03**: Exactly-once semantics — Flink 60s checkpoints with RocksDB state on MinIO, 2PC iceberg-flink sink, idempotent OpenSearch writes keyed by `event_id`.
- [ ] **STM-04**: Late events (past the 60s buffer) routed to `dlq.late-events.v1` via Flink side outputs.
- [ ] **STM-05**: Sandwich-flow correlator — keyed Flink job (keyed by `flow_id`) that joins child events to the parent flow stream and emits `payment.flow.v1` lifecycle updates as legs complete or fail (with compensation events on partial failure).
- [ ] **STM-06**: Continuous aggregation jobs producing `agg_volume_hourly`, `agg_success_rate_hourly`, `agg_screening_outcomes_daily`, `agg_stuck_withdrawals`, `agg_dlq_summary_hourly` Iceberg tables via incremental MERGE.

### DLQ — Dead Letter Queue

- [ ] **DLQ-01**: Four DLQ topics with shared envelope: `dlq.schema-invalid.v1`, `dlq.late-events.v1`, `dlq.processing-failed.v1`, `dlq.sink-failed.v1`.
- [ ] **DLQ-02**: Per-class retry policy enforced in Flink: no retry for SCHEMA_INVALID / LATE_EVENT / ILLEGAL_TRANSITION / BUSINESS_RULE; 3× exponential backoff (1s/5s/25s) for SINK_FAILURE / TRANSIENT.
- [ ] **DLQ-03**: All DLQ topics mirrored to Iceberg `dlq_events` table partitioned by `days(failed_at), bucket(4, error_class)` for Trino-queryable analytics and LLM agent grounding.
- [ ] **DLQ-04**: Python CLI replay tool (`apps/dlq-tools/`) supporting list, inspect, single-replay, bulk-replay (by class/source/age), with optional repair-transform hook.
- [ ] **DLQ-05**: 7-day live retention on Kafka DLQ topics; 90-day retention on Iceberg `dlq_events` table.

### LAK — Lakehouse (Iceberg / Trino)

- [ ] **LAK-01**: Iceberg JDBC catalog backed by Postgres (`iceberg_catalog` schema); MinIO storage layout with `raw/`, `facts/`, `dlq/` zones.
- [ ] **LAK-02**: Per-event-type raw tables (~16) with `days(event_time)` partitioning; `chain_transaction` adds `bucket(8, hash)` secondary partitioning.
- [ ] **LAK-03**: Denormalized `fact_transactions`, `fact_flows`, `fact_screening_outcomes` tables populated by Flink continuous joins; partitioned by `days(event_time), bucket(N, customer_id|flow_id)`.
- [ ] **LAK-04**: Hourly `rewrite_data_files` compaction targeting 128MB Parquet (ZSTD-3) in partitions older than 1 hour; daily `expire_snapshots` retaining 10 latest or 7 days; weekly `remove_orphan_files` and `rewrite_manifests`.
- [ ] **LAK-05**: Schema evolution via additive Avro changes propagated through `ALTER TABLE` migration jobs; destructive changes rejected at CI.
- [ ] **LAK-06**: Trino 470+ catalog config (`iceberg` JDBC + `postgres` admin) with 3-layer query strategy (raw views → fact tables → agg tables); Superset connects exclusively via Trino.
- [ ] **LAK-07**: Time-travel queries supported via `FOR VERSION AS OF`; last 7 days of snapshots retained.

### SCH — Search (OpenSearch)

- [ ] **SCH-01**: Three logical indices — `transactions`, `flows`, `dlq_events` — with `dynamic: strict` mappings and explicit field types.
- [ ] **SCH-02**: `transactions` index has ~35 fields including envelope, discriminators (event_type, flow_type, direction, is_crypto, is_user_facing), identifiers (transaction_reference, hash, customer_id, account_id, wallet_id), money (`amount_micros` as `long`, currency as `keyword`, source/target amounts, fx_rate, fee), status (internal_status, customer_status, screening_outcome), crypto (chain, asset, addresses with dual `text+keyword`, confirmations, gas_fee_micros, block_number, block_timestamp), counterparties (provider, route, beneficiary, sender), free text (description, notes).
- [ ] **SCH-03**: Money never stored as `float` — all amounts represented as `long` micros (USD/EUR 6-decimal, BTC satoshi).
- [ ] **SCH-04**: ILM with monthly + 50GB rollover, hot 30d → warm 60d (force-merged) → delete 1y; aliases (`transactions-write`, `transactions-read`) front the physical indices.
- [ ] **SCH-05**: `is_user_facing` flag separates internal fee/sweep events from customer-visible events; default API queries filter `is_user_facing=true`.
- [ ] **SCH-06**: Bulk indexing sink: 1000 docs / 5MB / 5s flush, idempotent on `event_id`, failed sub-requests routed to `dlq.sink-failed.v1`.
- [ ] **SCH-07**: Field-level customer-scope filter enforced via OpenSearch security plugin.

### API — Spring Boot

- [ ] **API-01**: Java 25 + Spring Boot 4.0.x service in `apps/api/` following the stablebridge-tx-recovery reference standards verbatim — hexagonal architecture, Gradle Kotlin DSL multi-module (`-api`, `-client`, main), four test source sets, ArchUnit-enforced layering.
- [ ] **API-02**: Sixteen REST endpoints across customer / admin / agent / actuator surfaces, each with `@Secured("ROLE_*")` role-based access.
- [ ] **API-03**: Domain models as Java records with `@Builder(toBuilder = true)`, type-safe IDs (`TransactionId`, `FlowId`, `CustomerId`), `Money(BigDecimal, CurrencyCode)` via nv-i18n.
- [ ] **API-04**: Generic `StateMachine<S, T>` framework for any domain status transitions; customer-facing status derived (never stored).
- [ ] **API-05**: Repository ports defined in `domain/`, adapters in `infrastructure/opensearch/`, `infrastructure/trino/`, `infrastructure/db/`; MapStruct mappers at every layer boundary.
- [ ] **API-06**: Namastack transactional outbox wired for the `dlq.replay.command.v1` event published from `POST /admin/dlq/{id}/replay`.
- [ ] **API-07**: JWT bearer auth issued by `apps/auth/` (RS256, JWK set served at `/.well-known/jwks.json`); short-lived access (15 min) + refresh (7d).
- [ ] **API-08**: Customer scoping enforced server-side in repository adapters via `customer_id` JWT claim; admin role bypasses.
- [ ] **API-09**: `X-Idempotency-Key` header + DB unique constraint on every create/replay endpoint.
- [ ] **API-10**: Bucket4j + Redis rate limiting (customer 100/min, admin 500/min, agent 1000/min) with `Retry-After` on 429.
- [ ] **API-11**: Constrained text-to-SQL endpoint at `POST /api/v1/agent/sql` validates SQL via Trino parser-walker against `fact_*` / `agg_*` allowlist before forwarding to Trino with 30s timeout.
- [ ] **API-12**: OpenAPI 3.1 spec exposed at `/v3/api-docs`; Next.js TypeScript client auto-generated via `@hey-api/openapi-ts` in CI.
- [ ] **API-13**: Structured error responses using `ApiError` record with `STBLPAY-XXXX` error codes; global `@RestControllerAdvice` handler.
- [ ] **API-14**: Micrometer metrics on every endpoint with p50/p90/p95/p99 distributions; Sentry error reporting; OTLP trace export.

### WEB — Next.js

- [ ] **WEB-01**: Next.js 15 App Router + React 19 + TypeScript 5.7 strict + Tailwind 4 + shadcn/ui application in `apps/web/`.
- [ ] **WEB-02**: Nine pages — `/login`, `/`, `/transactions`, `/transactions/[ref]` with state-machine timeline, `/flows/[id]` with multi-leg view, `/customers/[id]`, `/admin/dlq`, `/admin/dlq/[id]` with replay, `/admin/stuck`, `/admin/agent`.
- [ ] **WEB-03**: Auth via Auth.js v5 with custom JWT provider; HTTP-only secure cookie; middleware-enforced route protection by role.
- [ ] **WEB-04**: Hybrid data fetching — RSC for initial page render, TanStack Query for client-side mutations + invalidation + polling.
- [ ] **WEB-05**: Real-time live activity tile via SSE from `GET /api/v1/streams/transactions`; transaction/flow detail pages 3s polling while non-terminal.
- [ ] **WEB-06**: Agent chat UI via Vercel AI SDK with streaming responses and tool-call indication.
- [ ] **WEB-07**: Status badges color-coded by customer_status; vertical multi-leg flow stepper; horizontal Gantt-band state-machine timeline; dark mode default with theme toggle.
- [ ] **WEB-08**: Vitest unit tests + Playwright E2E covering login → search → DLQ replay → agent chat happy paths.
- [ ] **WEB-09**: Biome 2 format/lint + ESLint 9 flat config; OpenAPI client regenerated in CI on every API change.

### AGT — LLM Agent

- [ ] **AGT-01**: Python LangGraph 2.0 agent in `apps/llm-agent/` with FastAPI HTTP surface and SSE streaming; Postgres-backed checkpointer.
- [ ] **AGT-02**: StateGraph with planner → query-rewriter → tool-executor → relevance grader → answer drafter → hallucination grader → quality grader → server-side validator nodes; max 2 retries per grader before refusal; `interrupt()` for admin mutations requiring approval.
- [ ] **AGT-03**: Three constrained tools — `query_analytics`, `search_events`, `fetch_timeline` — exposed as a single MCP server (`apps/agent-tools-mcp/`) and consumed by the agent via `MultiServerMCPClient`.
- [ ] **AGT-04**: Defense-in-depth hallucination prevention — Pydantic schemas (client) → Java SQL allowlist + OpenSearch DSL whitelist (server) → in-graph LLM-as-judge graders → eight-rule final response validator.
- [ ] **AGT-05**: Eight final-validator rules: citation required, no invented references, no invented tables/columns, numeric grounding within 0.01%, no future claims, currency formatting compliance, customer-scope privacy boundary, refuse-on-missing-data.
- [ ] **AGT-06**: GPT-4o for planner / drafter / quality grader; GPT-4o-mini for classifier / relevance grader / hallucination grader (~70% cost savings on grader path).
- [ ] **AGT-07**: Pluggable `LlmProvider` abstraction — OpenAI (default), Anthropic (built, used for eval cross-validation), Ollama (stub + docs).
- [ ] **AGT-08**: Per-conversation 50K token budget; per-customer 10 conversations/hour limit; global daily token budget configurable via env var.
- [ ] **AGT-09**: Schema summary auto-generated from Avro definitions + Iceberg DDL + OpenSearch mappings on agent service startup, injected into the planner system prompt.
- [ ] **AGT-10**: Short-term memory via LangGraph messages state (last 20 turns); long-term per-customer memory in `agent_long_term_memory` Postgres table with 30-day TTL; cross-session insights from weekly batch summaries.

### EVL — Agent Evaluation

- [ ] **EVL-01**: 80-question golden eval set in YAML (`apps/llm-agent/eval/goldens/`) — 25 analytics, 20 incident-triage, 20 walkthroughs, 10 refusal cases, 5 cross-customer leak attempts.
- [ ] **EVL-02**: Three-layer eval harness — Layer 1 pytest unit (per-tool wrappers + grader prompts, 100% pass), Layer 2 trajectory match via `langchain-ai/agentevals` (≥ 90% with strict / unordered / subset modes), Layer 3 LLM-as-judge regression suite (faithfulness ≥ 4.2/5, relevance ≥ 4.0/5, citation 100%, format 100%).
- [ ] **EVL-03**: CI gate on every PR touching agent code — all three layers + p95 latency ≤ 8s + cost per question ≤ $0.05 avg / $0.20 max; budget cap of $20/PR.
- [ ] **EVL-04**: Eval results posted as PR comment with diffs vs `main` baseline.
- [ ] **EVL-05**: Production trace sampling — Langfuse weekly samples N=100 random traces and re-applies LLM-judge rubrics; failures grouped and reported.
- [ ] **EVL-06**: LLM-judge rubrics validated against human labels on a 50-sample subset (≥ 80% agreement) before being trusted in CI.

### OBS — Observability

- [ ] **OBS-01**: Prometheus + Grafana + Loki + Tempo + Langfuse self-hosted stack in `docker-compose.observability.yml` (opt-in to keep core Compose laptop-friendly).
- [ ] **OBS-02**: Five metric categories — pipeline throughput/latency, infra health, business KPIs, agent quality, DLQ health — with appropriate exporters per source (Micrometer, Flink native, kafka-exporter, prometheus-elasticsearch-exporter, postgres_exporter, redis_exporter, MinIO native, cAdvisor, custom Langfuse-driven Prometheus exporter for agent metrics).
- [ ] **OBS-03**: Fifteen alert rules defined in `ops/prometheus/alerts/*.yml` — 8 page-class (Kafka lag, Flink job failure, schema-invalid DLQ, OS cluster red, Trino down, Postgres pool exhausted, API p95 > 2s, agent refusal > 15%) + 7 warn-class.
- [ ] **OBS-04**: Five Grafana dashboards committed as JSON in `ops/grafana/dashboards/` — Pipeline Overview, Lakehouse Health, Agent Quality, DLQ Health, plus a System Overview meta-dashboard for portfolio storytelling.
- [ ] **OBS-05**: Structured JSON logging across every service with `trace_id`, `customer_id`, `event_id` MDC fields; Loki retention 14d dev / 90d prod.
- [ ] **OBS-06**: OTEL trace context propagated via W3C TraceContext headers across HTTP and via Avro `EventEnvelope.trace_id` across Kafka; 100% sampling in dev, 10% head-based + always-error/slow in prod.
- [ ] **OBS-07**: Langfuse v3 (Clickhouse-backed) captures every agent turn with full LLM/tool call tree, retries, grader scores; OTEL spans linked to Langfuse traces by `trace_id`.

### CI — Continuous Integration

- [ ] **CI-01**: Ten path-filtered GitHub Actions workflows — `ci-java.yml`, `ci-python-simulator.yml`, `ci-flink-jobs.yml`, `ci-llm-agent.yml`, `ci-agent-tools-mcp.yml`, `ci-web.yml`, `ci-schemas.yml`, `ci-infra.yml`, `ci-e2e.yml` (nightly), `ci-version-bump.yml` (weekly).
- [ ] **CI-02**: Per-PR pipeline shape: lint → compile → unit → integration → coverage → image build → push to GHCR; target wall-clock ≤ 12 min.
- [ ] **CI-03**: Schema-compatibility CI gate — ephemeral Confluent SR validates BACKWARD compatibility on each subject; codegen sync verified; Avro-to-Iceberg mapping drift rejected.
- [ ] **CI-04**: Agent eval CI gate (per Q15d / EVL-03) — three eval layers + latency + cost gates with PR comment reporting.
- [ ] **CI-05**: Trunk-based branching with SemVer release tags; release workflow builds + pushes `:vX.Y.Z` tagged images, registers Avro schemas to SR with version label, generates GitHub release notes.
- [ ] **CI-06**: Branch protection on `main` requiring all path-relevant workflows green, schema CI green if schemas changed, agent eval green if agent changed, no direct push.
- [ ] **CI-07**: Aggressive caching — Gradle build cache, pnpm cache, uv cache, Docker BuildKit, Testcontainers image warmup.
- [ ] **CI-08**: Local CI mirror via `just ci-all` running the same checks before push.

### TST — Testing Strategy

- [ ] **TST-01**: Java side follows stablebridge §16 four-source-set strategy — unit (JUnit 5 + Mockito BDD + AssertJ, golden recursive-comparison rule), test fixtures, integration (Testcontainers Postgres/OpenSearch/Trino/Kafka + WireMock), business tests (full server, real containers, end-to-end through HTTP).
- [ ] **TST-02**: Python services use pytest + hypothesis (property-based for state-machine transitions) + Testcontainers-Python for integration tests.
- [ ] **TST-03**: Flink jobs use `MiniClusterWithClientResource` for unit tests + Testcontainers Kafka/MinIO for integration tests.
- [ ] **TST-04**: Web app uses Vitest unit + Playwright E2E covering the full happy path; Mock Service Worker for API stubbing.
- [ ] **TST-05**: ArchUnit (Java) + import-linter (Python) enforce hexagonal layering; tests fail PRs that violate the architecture.
- [ ] **TST-06**: k6 load tests pre-release asserting API p95 < 500ms at 100 RPS sustained.
- [ ] **TST-07**: Toxiproxy chaos tests pre-release covering kill-OS / kill-Trino / partition-Kafka scenarios; verify recovery, no data loss, DLQ catches errors.
- [ ] **TST-08**: Nightly E2E orchestrator (`tests/e2e/`) spins full Compose, drives 100 transactions across all 5 flows, asserts presence in OS + Iceberg + API + agent answers, injects schema-invalid events to verify DLQ handling, triggers DLQ replay to verify reprocessing, all within ~10 min.
- [ ] **TST-09**: Coverage gates — Java 90% line / 80% branch via JaCoCo; Python 85% line; Web 80% logic + 100% auth/middleware. Reported in dashboards; not a hard merge gate.

### SEC — Security, Privacy, Retention

- [ ] **SEC-01**: PII inventory documented in `docs/PRIVACY.md`; classifies every field across customer, beneficiary, sender, crypto-address, free-text categories.
- [ ] **SEC-02**: Encryption at rest — MinIO SSE-S3 for Iceberg parquet, OpenSearch index-level encryption, Postgres `pgcrypto` for `customers` and `auth_users` tables.
- [ ] **SEC-03**: TLS where supported on inter-service traffic in Compose (Kafka SSL listeners, OpenSearch HTTPS, Postgres SSL); plain HTTP on the local Docker network is acceptable for dev.
- [ ] **SEC-04**: Logback masking pattern + structlog processor + Flink LogEventMasker + OTEL span filter strip PII fields (iban, email, name, phone, address) before emission.
- [ ] **SEC-05**: CI step greps E2E-run logs for known-PII test fixtures and fails the build if matched.
- [ ] **SEC-06**: `POST /api/v1/admin/customers/{id}/redact-pii` endpoint masks PII fields in OpenSearch + Iceberg in place (not hard-deleted — financial records retained 7 years per regulatory baseline) and hard-deletes the row in Postgres `customers`.
- [ ] **SEC-07**: Tiered retention — raw event tables 365d hot / archived 7y; fact tables 365d / 7y; OpenSearch 30d hot + 60d warm + delete 1y; DLQ Iceberg 90d; logs 14d; traces 7d; auth audit log 7y. Compose-dev retention shortened to keep MinIO bucket small.
- [ ] **SEC-08**: Customer-scope enforcement at three layers — server-side filter in repository adapters, agent guardrail rule 7 (cross-customer privacy boundary), integration test asserting cross-customer requests return 404 (not 403, to avoid enumeration).
- [ ] **SEC-09**: Secrets scan via `gitleaks` in CI; `infra/.env.example` documented; production secret-manager integration deferred to `infra/SECRETS.md` future-state.

### PLT — Platform / Extension Points

- [ ] **PLT-01**: Pluggable `EventSource` (Q20a) — Faker default, replay built, external-Kafka stub.
- [ ] **PLT-02**: Pluggable `LlmProvider` (Q20b) — OpenAI default, Anthropic built, Ollama stub.
- [ ] **PLT-03**: Pluggable schema domain — `just regenerate-schemas` regenerates Java POJOs, Python dataclasses, OpenSearch mappings (templated), Iceberg DDL (templated), Flink job stubs from `schemas/*.avsc`.
- [ ] **PLT-04**: Plugin discovery via env var (`SOURCE_TYPE`, `LLM_PROVIDER`) and Python entry-point registration; third-party packages can register additional implementations without code changes.
- [ ] **PLT-05**: Six documentation files committed — `docs/EXTENDING.md` (forking guide with insurance-claims worked example, ~2-day target), `docs/ARCHITECTURE.md` (annotated diagram), `docs/EVENT-MODEL.md` (full state-machine + topic listing), `docs/RUNBOOK.md` (alert response), `docs/STACK.md` (auto-updated version pin list), `docs/EVAL-METHODOLOGY.md` (rubrics + reproduction steps).

### DEP — Deployment

- [ ] **DEP-01**: Single-machine Docker Compose v2 deployment — `docker-compose.yml` core (Kafka, Schema Registry, Flink, MinIO, Postgres, OpenSearch, Trino, Superset, API, Web, Auth, MCP server, Agent, Redis, OTEL Collector), `docker-compose.observability.yml` opt-in (Prometheus, Grafana, Loki, Tempo, Langfuse, Clickhouse, alertmanager, exporters).
- [ ] **DEP-02**: `just up` / `just up-observability` / `just down` / `just logs` / `just preflight` / `just test` / `just ci-all` orchestration commands documented in the README.
- [ ] **DEP-03**: Healthcheck-driven dependency ordering in Compose — services declare `depends_on` with `condition: service_healthy`; total cold-start to all-healthy ≤ 5 minutes on a 16GB laptop.
- [ ] **DEP-04**: Container images published to GHCR with `:vX.Y.Z` SemVer tags + `:latest` floating tag.

---

## v2 Requirements (deferred)

- **DEP-V2-01**: Cloud deployment automation (AWS / EKS / Helm / Terraform) — documented as future-state in `infra/CLOUD.md`.
- **API-V2-01**: OAuth2 / Keycloak SSO replacing local JWT issuer.
- **STM-V2-01**: External Kafka cluster tap (`ExternalKafkaSource` full implementation).
- **LAK-V2-01**: dbt or SQLMesh batch transforms layered on top of `agg_*` tables for analyst-style modeling.
- **AGT-V2-01**: Stream-mid-validation (validate response chunks as they're generated rather than after full draft).
- **WEB-V2-01**: GraphQL surface for the Next.js client where REST round-trips become noticeable.
- **OBS-V2-01**: Multi-region trace correlation, distributed Tempo, sentry-self-hosted.

## Out of Scope (excluded from any version)

- Mobile / native apps — web-only
- Real cryptocurrency operations (real chain RPCs, signing keys, vault integrations)
- Real banking-partner contracts
- Multi-region / disaster recovery design
- Schema Registry replacement with Apicurio (license cleanliness not load-bearing here)

---

## Traceability

REQ-ID → Phase mapping is filled by `ROADMAP.md`. Each requirement maps to exactly one phase.

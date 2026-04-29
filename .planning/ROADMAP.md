# ROADMAP — stablepay-payment-pipeline

> Eight phases mapping every v1 REQ-ID to a deliverable. Standard granularity (5-8 phases, ~3-5 plans each). All v1 requirements covered.

## Coverage check

- **62 v1 requirements** across 16 categories
- **8 phases**, each with 3–5 plans
- **100% requirement coverage** verified in `## Phase Details` below

---

## Phase Summary

| # | Phase | Goal | REQ-IDs | Success criteria |
|---|---|---|---|---|
| 1 | Foundation & Schemas | Repo skeleton, build orchestration, Avro contract definitions, Schema Registry, baseline Compose | EVT-01..05, DEP-02 | 4 |
| 2 | Producer & Streaming Core | Faker simulator with state machines, Flink jobs ingesting to Iceberg raw tables + OpenSearch transactions index with exactly-once + DLQ | ING-01..05, STM-01..05, DLQ-01..02, LAK-01..02, SCH-01..03, SCH-06 | 5 |
| 3 | Lakehouse & Analytics | Fact + agg tables, Iceberg maintenance, Trino + Superset, DLQ mirror + replay CLI, ILM on OpenSearch | STM-06, DLQ-03..05, LAK-03..07, SCH-04..05, SCH-07 | 5 |
| 4 | API & Web | Spring Boot API following stablebridge standards, Next.js shell with all 9 pages, auth, OpenAPI codegen | API-01..14, WEB-01..05, WEB-07..09 | 5 |
| 5 | LLM Agent & MCP | MCP tools server, LangGraph agent with grader nodes, eval harness with 80 goldens, agent chat in web | AGT-01..10, EVL-01..06, WEB-06 | 5 |
| 6 | Observability & Hardening | Prom/Grafana/Loki/Tempo/Langfuse stack, 15 alerts, 5 dashboards, PII masking, retention policies | OBS-01..07, SEC-01..09 | 4 |
| 7 | CI/CD & Test Discipline | All ten GitHub Actions workflows, schema + agent eval gates, ArchUnit + chaos + load + nightly E2E | CI-01..08, TST-01..09, DEP-01, DEP-03..04 | 4 |
| 8 | Platform Polish & Docs | Plugin implementations, schema regeneration, six published docs, README, portfolio polish | PLT-01..05 | 3 |

---

## Phase Details

### Phase 1 — Foundation & Schemas

**Goal:** Establish the polyglot monorepo skeleton with build orchestration (`just`, Gradle 9 Kotlin DSL, uv, pnpm), define every Avro schema for the five flows + DLQs + signing/screening/approval topics with renamed domain-native vocabulary, run Confluent Schema Registry in Compose with BACKWARD compatibility, and wire Java + Python codegen so downstream phases consume schemas as a typed contract.

**Requirements:** EVT-01, EVT-02, EVT-03, EVT-04, EVT-05, DEP-02

**Plans:**
- 1.1 — Monorepo skeleton + build orchestration (`justfile`, root `build.gradle.kts`, `settings.gradle.kts` for `apps/api/` + `apps/flink-jobs/`, root `package.json` for pnpm workspaces, `pyproject.toml` per Python app, `.gitignore`, `.editorconfig`, `.gitleaks.toml`)
- 1.2 — Avro schemas authoring — shared `EventEnvelope`, all five-flow event records, signing/screening/approval auxiliary records, DLQ envelope, with codegen targets generating Java POJOs (Gradle Avro plugin) and Python dataclasses (`dataclasses-avroschema`)
- 1.3 — Compose baseline (`docker-compose.yml`) with Kafka 4.0 (KRaft), Confluent Schema Registry, Postgres 17 (catalog + auth), MinIO, all healthchecked; `just up` / `just down` / `just preflight` working end-to-end

**Success criteria:**
1. `just up` brings up Kafka + SR + Postgres + MinIO with all healthchecks passing within 90 seconds on a 16GB laptop
2. `just regenerate-schemas` produces deterministic Java POJOs and Python dataclasses checked in to repo
3. CI sketch (`ci-schemas.yml`) registers every Avro subject in an ephemeral SR with BACKWARD compatibility and exits green
4. `EventEnvelope` record carries event_id, event_time, ingest_time, schema_version, flow_id, correlation_id, trace_id and is referenced by every event-bearing topic schema

---

### Phase 2 — Producer & Streaming Core

**Goal:** Build the Python state-machine simulator that emits all five payment flows at ~200 evt/s with periodic 2000 evt/s bursts, plus the Flink job set that consumes every Kafka topic, validates Avro, and dual-writes to Iceberg raw tables + an initial OpenSearch `transactions` index — with exactly-once semantics, event-time watermarks, and the four DLQ topics wired up.

**Requirements:** ING-01, ING-02, ING-03, ING-04, ING-05, STM-01, STM-02, STM-03, STM-04, STM-05, DLQ-01, DLQ-02, LAK-01, LAK-02, SCH-01, SCH-02, SCH-03, SCH-06

**Plans:**
- 2.1 — Python simulator (`apps/simulator/`) — pluggable `EventSource` protocol with `FakerSource` implementation, state machines per flow type (fiat payout 30+ statuses, crypto lifecycle, fiat payin, crypto payin, multi-leg payment_flow with `flow_id` propagation), idempotent transactional Kafka producer, sustained 200 evt/s + `--burst` mode
- 2.2 — Flink jobs core (`apps/flink-jobs/`) — Avro deserialization with envelope validation, `WatermarkStrategy.forBoundedOutOfOrderness(60s) + withIdleness(60s)`, RocksDB state on MinIO, 60s checkpoints, restart strategy
- 2.3 — Iceberg + OpenSearch dual-write sinks — Iceberg raw tables (`payment_payout_fiat`, `payment_payout_crypto`, `payment_payin_fiat`, `payment_payin_crypto`, `payment_flow`, `chain_transaction`, etc.) via 2PC sink; OpenSearch `transactions` index with strict mapping, money as `long` micros, idempotent writes keyed by `event_id`
- 2.4 — Sandwich correlator — keyed Flink job (key = `flow_id`) joining child events to parent flow stream, emitting `payment.flow.v1` lifecycle updates with leg state, partial-failure compensation events
- 2.5 — DLQ topology — four DLQ topics with shared envelope, Flink side outputs for SCHEMA_INVALID / LATE_EVENT / ILLEGAL_TRANSITION / SINK_FAILURE, per-class retry policy (no retry vs 3× exponential backoff), counters exposed as Flink metrics

**Success criteria:**
1. Simulator runs `just simulate --rate 200` and Kafka shows ~200 msg/s sustained across topics for 10 minutes with zero producer-side errors
2. Flink jobs consume every topic and write to Iceberg raw tables + OpenSearch with end-to-end producer→OS p95 latency under 2 seconds at sustained load
3. Killing the Flink job mid-stream and restarting produces zero duplicate Iceberg rows and zero duplicate OpenSearch documents (verified by replay test)
4. Schema-invalid events injected by the simulator land in `dlq.schema-invalid.v1` (not in raw Iceberg or OpenSearch)
5. Sandwich correlator produces a `payment.flow.v1` lifecycle stream where parent FLOW_COMPLETED is emitted only after all three legs have COMPLETED in Iceberg (asserted via integration test)

---

### Phase 3 — Lakehouse & Analytics

**Goal:** Build the analytics layer — denormalized fact tables, continuously-aggregated `agg_*` tables driven by Flink upserts, Iceberg maintenance jobs, Trino catalog with Superset dashboards, mirror DLQ events to Iceberg, ship the DLQ replay CLI, and add OpenSearch ILM + multi-index support.

**Requirements:** STM-06, DLQ-03, DLQ-04, DLQ-05, LAK-03, LAK-04, LAK-05, LAK-06, LAK-07, SCH-04, SCH-05, SCH-07

**Plans:**
- 3.1 — Fact tables (`fact_transactions`, `fact_flows`, `fact_screening_outcomes`) populated by Flink continuous joins; Iceberg v2 row-level updates for in-place status mutations
- 3.2 — Aggregation tables (`agg_volume_hourly`, `agg_success_rate_hourly`, `agg_screening_outcomes_daily`, `agg_stuck_withdrawals`, `agg_dlq_summary_hourly`) via Flink continuous aggregations with 1-minute MERGE cadence
- 3.3 — Iceberg maintenance jobs orchestrated by `apps/lakehouse-jobs/` (Python + APScheduler) — hourly compaction targeting 128MB Parquet, daily snapshot expiration, weekly orphan-file removal + manifest rewrite, daily Postgres catalog `pg_dump` backup
- 3.4 — Trino 470+ deployment in Compose (1 coordinator + 2 workers) with `iceberg` JDBC catalog + `postgres` admin catalog; Superset 4.x with datasets defined for fact + agg tables; 4 initial Superset dashboards (Volume, Success Rate, Compliance, DLQ); time-travel queries verified
- 3.5 — DLQ Iceberg mirror (`dlq_events` table) + Python replay CLI (`apps/dlq-tools/` with `dlq-list / dlq-inspect / dlq-replay / dlq-replay --class`) + 7-day Kafka retention + 90-day Iceberg retention + add `flows` and `dlq_events` OpenSearch indices with ILM monthly + 50GB rollover

**Success criteria:**
1. Trino query against `fact_transactions` returns p95 < 500ms for "find all transactions for customer X in last 7 days" at 1M+ row volumes
2. `agg_volume_hourly` updates within 90s of Kafka events for that hour (verified via end-to-end timing test)
3. Compaction job reduces small-file count by ≥ 80% on a synthetic 10K-file partition
4. DLQ replay CLI successfully bulk-replays 100 events from `dlq.processing-failed.v1` back into the original topic, all of which land in Iceberg + OpenSearch
5. `transactions` ILM rollover tested in compressed dev timeline (hot 1d / warm 3d / delete 7d) with successful index transition

---

### Phase 4 — API & Web

**Goal:** Build the Spring Boot 4.x API service following the stablebridge-tx-recovery reference standards verbatim, with all 16 endpoints across customer / admin / agent / actuator surfaces; build the Next.js 15 web app with all 9 pages, JWT auth, OpenAPI codegen, real-time updates via SSE + polling, and Playwright E2E coverage of the customer + admin happy paths.

**Requirements:** API-01..14, WEB-01..05, WEB-07, WEB-08, WEB-09

**Plans:**
- 4.1 — Auth service (`apps/auth/`) — RS256 JWT issuer with JWK set, Postgres-backed users seeded via Flyway migration, login + refresh endpoints, customer + admin + agent role grants
- 4.2 — Spring Boot API core (`apps/api/`) — three-module Gradle Kotlin DSL setup (`-api`, `-client`, main), hexagonal layout with `application/`/`domain/`/`infrastructure/`, generic `StateMachine<S, T>` framework, Money + type-safe IDs, MapStruct mappers, Namastack outbox starter, repository ports + OpenSearch + Trino + Postgres adapters, Spring Security with `@Secured`, Bucket4j rate limits, customer + admin endpoints (transactions, flows, customer summary, dlq, stuck, aggregates)
- 4.3 — Spring Boot agent endpoints — `/api/v1/agent/sql` with Trino parser-based SQL allowlist validator (`io.trino:trino-parser`), `/api/v1/agent/search` with OpenSearch DSL whitelist, `/api/v1/agent/timeline/{ref}` returning the canonical Markdown + structured timeline; OpenAPI 3.1 spec exposed at `/v3/api-docs`; ArchUnit tests + four source sets per stablebridge §16
- 4.4 — Next.js web shell (`apps/web/`) — Next.js 15 App Router, Tailwind 4, shadcn/ui, NextAuth v5 with custom JWT provider, middleware-enforced role-based routing, OpenAPI client codegen via `@hey-api/openapi-ts`, dark-mode-default theming
- 4.5 — Customer + admin pages — `/login`, `/`, `/transactions`, `/transactions/[ref]` with state-machine timeline component, `/flows/[id]` with multi-leg stepper, `/customers/[id]/summary`, `/admin/dlq`, `/admin/dlq/[id]` with replay button, `/admin/stuck`; SSE live-activity tile + 3s polling on non-terminal pages; Vitest unit tests + Playwright E2E covering login → search → DLQ replay happy paths

**Success criteria:**
1. ArchUnit test suite passes 100% — no field injection, no `System.out`, hexagonal layering enforced
2. Business test suite (full server + real OpenSearch + Trino + Postgres Testcontainers) passes 100% with `assertThat(...).usingRecursiveComparison()` golden rule applied
3. OpenAPI spec generates a TypeScript client with zero hand-edits in `apps/web/src/api-client/`
4. Playwright E2E walks login → search transaction → drill into flow → admin DLQ replay → re-verify replayed event in transaction list, all green
5. `POST /api/v1/admin/dlq/{id}/replay` with `X-Idempotency-Key` is idempotent — replaying the same key twice produces 200 (not 409) and emits exactly one outbox event

---

### Phase 5 — LLM Agent & MCP

**Goal:** Build the LangGraph 2.0 self-correcting agentic-RAG agent with three constrained tools exposed via MCP, defense-in-depth hallucination prevention (eight server-side validator rules + three in-graph LLM-as-judge graders), the 80-question golden eval harness with three-layer scoring, and end-to-end agent chat in the Next.js web UI with SSE streaming through the Spring Boot API.

**Requirements:** AGT-01..10, EVL-01..06, WEB-06

**Plans:**
- 5.1 — MCP tools server (`apps/agent-tools-mcp/`) — `fastmcp` Python server hosting `query_analytics`, `search_events`, `fetch_timeline` tools, each proxying to the corresponding Spring Boot `/api/v1/agent/*` endpoint with the `ROLE_AGENT` JWT; Pydantic schema enforcement on inputs
- 5.2 — LangGraph agent (`apps/llm-agent/`) — FastAPI HTTP surface with SSE streaming, Postgres-backed checkpointer, StateGraph wiring all eight nodes (planner / query-rewriter / tool-executor / relevance-grader / answer-drafter / hallucination-grader / quality-grader / final-validator), pluggable `LlmProvider` (OpenAI default), GPT-4o + GPT-4o-mini split, schema summary auto-generated at startup, short-term + long-term memory
- 5.3 — Eight-rule final-response validator (Java side, in `apps/api/`) — citations, no-invented-references, no-invented-tables, numeric grounding, no-future-claims, currency formatting, customer-scope privacy, refuse-on-missing-data; per-rule violation telemetry exported as Prometheus metrics
- 5.4 — Eval harness (`apps/llm-agent/eval/`) — 80 goldens across 5 categories in YAML, three-layer harness (pytest unit + `langchain-ai/agentevals` trajectory + LLM-as-judge regression), CI runner with PR-comment reporting, $20/PR budget cap, LLM-judge rubric validation against 50-sample human-labeled subset
- 5.5 — Agent chat UI integration — Next.js `/admin/agent` page using Vercel AI SDK with SSE streaming through Spring Boot proxy, tool-call indicators, conversation history with customer-scope, per-conversation 50K token budget enforcement

**Success criteria:**
1. Agent answers "what's stuck right now" with the correct tool sequence (query_analytics on `agg_stuck_withdrawals`) and produces a response that cites every numeric claim
2. Cross-customer leak attempt ("show me transactions for customer Y" when scoped to X) triggers refusal via guardrail rule 7 — verified by golden eval entry
3. Three-layer eval CI gate passes on a baseline run: trajectory match ≥ 90%, faithfulness ≥ 4.2, relevance ≥ 4.0, citations 100%, format 100%
4. End-to-end agent chat in the web UI streams a response chunk-by-chunk with tool calls visible to the user, total time-to-first-byte < 3 seconds for analytics questions
5. Langfuse trace for every agent turn shows full LLM call tree + tool calls + grader scores + retry count + token cost; sampled 100 traces all pass the eight-rule validator post-hoc

---

### Phase 6 — Observability & Hardening

**Goal:** Stand up the Prometheus + Grafana + Loki + Tempo + Langfuse self-hosted observability stack as an opt-in Compose overlay, define 15 alert rules + 5 Grafana dashboards, instrument every service with structured JSON logging and OTEL trace propagation including the Avro envelope `trace_id`, and lock down PII handling with masking, the redact-PII admin endpoint, and tiered retention policies.

**Requirements:** OBS-01..07, SEC-01..09

**Plans:**
- 6.1 — Observability stack — `docker-compose.observability.yml` with Prometheus, Grafana, alertmanager, Loki, Tempo, OpenTelemetry Collector, Langfuse v3 + Clickhouse, all healthchecked; `just up-observability` orchestration
- 6.2 — Metrics & alerts — exporters wired across every component (Micrometer for Java, Flink native, kafka-exporter, prometheus-elasticsearch-exporter, postgres_exporter, redis_exporter, MinIO native, cAdvisor, custom Langfuse-driven Prometheus exporter for agent metrics); 15 alert rules in `ops/prometheus/alerts/*.yml` (8 page + 7 warn); 5 Grafana dashboards in `ops/grafana/dashboards/*.json` (Pipeline Overview, Lakehouse Health, Agent Quality, DLQ Health, System Overview)
- 6.3 — Logs & traces — structured JSON logging across Spring Boot (Logback + logstash-encoder), Python (structlog with JSON renderer), Flink (log4j2 JSON layout); Loki ingestion with `service` / `level` / `trace_id` labels; OTEL Collector receiving spans + metrics + logs; Tempo storing traces with W3C TraceContext propagation across HTTP + Kafka headers + Avro `EventEnvelope.trace_id`; 100% sampling in dev, 10% head-based + always-error in prod
- 6.4 — Privacy & retention hardening — PII inventory in `docs/PRIVACY.md`, Logback masking pattern + structlog processor + Flink LogEventMasker + OTEL span filter stripping iban/email/name/phone/address fields; CI step grepping E2E logs for known-PII fixtures; `POST /api/v1/admin/customers/{id}/redact-pii` endpoint masking OS + Iceberg in place + hard-delete in Postgres; tiered retention (raw 365d hot / 7y archive, OS 30d hot + 60d warm + delete 1y, DLQ 90d, logs 14d, traces 7d) configured per environment

**Success criteria:**
1. `just up-observability` adds the observability stack to a running Compose with all dashboards loading in Grafana within 30 seconds
2. A simulated Kafka consumer-lag spike (artificially throttle the Flink job) triggers the page-class alert in alertmanager and writes to `ops/alerts.log` in dev
3. End-to-end trace for a single transaction shows ≥ 8 spans across producer → Kafka → Flink → Iceberg + OpenSearch sink → API → web; all spans share the same `trace_id`
4. PII-scan CI step on the E2E log output finds zero hits for known-PII test fixtures (iban / email / name / phone)
5. `POST /admin/customers/{id}/redact-pii` redacts all PII fields in OpenSearch + Iceberg for the targeted customer in < 60 seconds for ≤ 100K records and produces an audit log entry

---

### Phase 7 — CI/CD & Test Discipline

**Goal:** Ship all ten path-filtered GitHub Actions workflows including the schema-compatibility CI gate and agent eval CI gate, codify ArchUnit + import-linter + chaos + load testing, build the nightly E2E orchestrator that drives 100 simulated transactions through the full pipeline, and complete the deployment story (multi-stage Dockerfiles, GHCR image publishing with SemVer tags, healthcheck-driven Compose dependency ordering).

**Requirements:** CI-01..08, TST-01..09, DEP-01, DEP-03, DEP-04

**Plans:**
- 7.1 — Path-filtered GitHub Actions workflows — `ci-java.yml`, `ci-python-simulator.yml`, `ci-flink-jobs.yml`, `ci-llm-agent.yml`, `ci-agent-tools-mcp.yml`, `ci-web.yml`, `ci-schemas.yml`, `ci-infra.yml` — each with lint → compile → unit → integration → coverage → image build pipeline; aggressive caching (Gradle build cache, pnpm cache, uv cache, BuildKit layers); branch protection rules on `main`
- 7.2 — Schema CI gate + agent eval CI gate + version-bump — `ci-schemas.yml` runs ephemeral SR + BACKWARD compat + codegen sync + Iceberg-mapping drift; `ci-llm-agent.yml` runs three-layer eval with PR-comment reporting and $20 budget cap; `ci-version-bump.yml` weekly cron + auto-PR for dependency upgrades
- 7.3 — Test discipline codification — ArchUnit rules in `apps/api/` extending `DefaultArchitectureTest` per stablebridge §16; import-linter rules for Python services; hypothesis property-based tests asserting state-machine invariants; Toxiproxy chaos tests in Compose covering kill-OS / kill-Trino / partition-Kafka; k6 load tests pre-release with API p95 < 500ms at 100 RPS gate
- 7.4 — Nightly E2E + production-grade Compose — `ci-e2e.yml` cron at 02:00 UTC spinning full Compose, driving 100 transactions across all 5 flows, asserting OS + Iceberg + API + agent answer correctness, injecting schema-invalid events for DLQ verification, triggering DLQ replay; multi-stage Dockerfiles for every service with `output: 'standalone'` for Next.js; healthcheck-driven `depends_on: condition: service_healthy` everywhere; `:vX.Y.Z` SemVer tag publish to GHCR on release tag

**Success criteria:**
1. Pull request touching only `apps/web/` runs only `ci-web.yml` (path filtering working) and completes in under 10 minutes
2. Pull request introducing a backward-incompatible Avro change is blocked at merge by `ci-schemas.yml` with a clear error message
3. Pull request bumping the agent prompt that regresses LLM-judge faithfulness below 4.2 is blocked at merge by `ci-llm-agent.yml` with PR-comment showing the score deltas
4. Nightly E2E orchestrator completes a 100-transaction full-pipeline run in under 12 minutes and asserts all 100 transactions in OS + Iceberg + API + correct agent answer for one canned question per flow type
5. Cold `just up` from a fresh `git clone` reaches "all healthchecks green" in under 5 minutes on a 16GB laptop

---

### Phase 8 — Platform Polish & Documentation

**Goal:** Ship the production implementations of the pluggable EventSource and LlmProvider abstractions, build the schema-domain regeneration script with a worked forking example, and publish the full six-document set so a stranger can understand, run, and extend the project.

**Requirements:** PLT-01..05

**Plans:**
- 8.1 — Pluggable production implementations — `ReplaySource` reading from Iceberg `raw_*` table dump or local file; `AnthropicProvider` for cross-validation use in eval; `OllamaProvider` stub with documented setup; entry-point registration in `pyproject.toml` so third-party packages can register additional sources/providers
- 8.2 — Schema-domain regeneration script + worked example — `just regenerate-schemas` regenerates Java POJOs, Python dataclasses, OpenSearch mappings (templated), Iceberg DDL (templated), Flink job stubs from `schemas/*.avsc`; `examples/insurance-claims/` worked example showing forking effort (~2 days for an experienced engineer)
- 8.3 — Documentation set — `docs/EXTENDING.md` (forking guide), `docs/ARCHITECTURE.md` (annotated architecture diagram with all components + data flow), `docs/EVENT-MODEL.md` (full state-machine + topic listing + event examples), `docs/RUNBOOK.md` (alert response procedures for each of the 15 alerts), `docs/STACK.md` (auto-updated version pin list), `docs/EVAL-METHODOLOGY.md` (rubrics + reproduction steps), top-level `README.md` with screenshots + 5-minute quickstart

**Success criteria:**
1. A stranger following `README.md` can `git clone && just up` and reach the running Next.js dashboard in under 10 minutes on a fresh laptop
2. `examples/insurance-claims/` includes a complete fork showing schema swap + simulator state-machine swap + everything else (Kafka, Flink, Iceberg, Trino, OpenSearch, API, agent) working unchanged
3. All six docs published, cross-linked from `README.md`, with at least one architecture diagram in `docs/ARCHITECTURE.md`

---

## Traceability table (REQ-ID → Phase)

| REQ-ID | Phase | REQ-ID | Phase | REQ-ID | Phase |
|---|---|---|---|---|---|
| EVT-01..05 | 1 | API-01..14 | 4 | OBS-01..07 | 6 |
| ING-01..05 | 2 | WEB-01..05, 07..09 | 4 | SEC-01..09 | 6 |
| STM-01..05 | 2 | WEB-06 | 5 | CI-01..08 | 7 |
| STM-06 | 3 | AGT-01..10 | 5 | TST-01..09 | 7 |
| DLQ-01..02 | 2 | EVL-01..06 | 5 | DEP-01, 03..04 | 7 |
| DLQ-03..05 | 3 | LAK-01..02 | 2 | DEP-02 | 1 |
| LAK-03..07 | 3 | SCH-01..03, 06 | 2 | PLT-01..05 | 8 |
| SCH-04..05, 07 | 3 |  |  |  |  |

**Coverage**: 62/62 v1 requirements mapped to a phase. ✓

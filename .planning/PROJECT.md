# stablepay-payment-pipeline

## What This Is

A real-time data pipeline for stablecoin and fiat payment events: producer → Kafka → Flink → Iceberg lakehouse + OpenSearch search index, served by a Spring Boot API, a Next.js dashboard, and a self-correcting LangGraph agent. Models five canonical payment flows — fiat payin, fiat payout, crypto payin, crypto payout, and the multi-leg payment flow that orchestrates them (the on-ramp / off-ramp / crypto-to-crypto sandwich pattern). Built as a forkable reference platform that demonstrates production-grade streaming, lakehouse, agentic-AI, and observability practices on a single laptop.

## Core Value

A single-`git clone`, single-`just up` reference platform that demonstrates every production-grade pattern a modern stablecoin/fiat payment data system requires — high-fidelity event modeling, exactly-once stream processing, lakehouse + search dual-write, agentic SQL/RAG with hallucination guardrails, and full observability — with credible end-to-end testing and clear extension points.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] **REQ-EVENTS-01**: High-fidelity event schemas for fiat payout, fiat payin, crypto payout, crypto payin, and multi-leg payment flow, with renamed domain-native vocabulary and full state-machine transitions including 4-eyes / dual-approval, compliance screening with hold/RFI/release/reject/seize branches, partner routing, and refund/reversal/confiscation flows.
- [ ] **REQ-EVENTS-02**: Avro schemas authored in `schemas/`, registered with Confluent Schema Registry under BACKWARD compatibility, with Java + Python codegen kept in sync via CI.
- [ ] **REQ-EVENTS-03**: Shared `EventEnvelope` Avro header across every topic carrying `event_id`, `event_time`, `ingest_time`, `schema_version`, `flow_id`, `correlation_id`, and `trace_id`.
- [ ] **REQ-PRODUCER-01**: Pluggable event source (Faker simulator, fixture replay, external-Kafka tap) emitting ~200 events/sec sustained with periodic 2000 events/sec bursts; idempotent + transactional Kafka producer.
- [ ] **REQ-PRODUCER-02**: Simulator drives a deterministic state machine per flow; emits parent `payment.flow` events with `flow_id` propagated through every child event for downstream correlation.
- [ ] **REQ-STREAM-01**: Apache Flink 2.x jobs consume every Kafka topic, validate Avro, enrich, and dual-write to Iceberg (lakehouse) and OpenSearch (search) with exactly-once semantics via 2PC sink + 60s checkpoints + idempotent `_id`-keyed OpenSearch writes.
- [ ] **REQ-STREAM-02**: Event-time semantics with 60s bounded-out-of-order watermarks + 60s idleness detection; late events routed to a dedicated DLQ topic.
- [ ] **REQ-STREAM-03**: Sandwich-flow correlator — keyed Flink job joining child events to parent `flow_id` and emitting `payment.flow.v1` lifecycle updates as legs progress.
- [ ] **REQ-DLQ-01**: Four error-class DLQ topics (`schema-invalid`, `late-events`, `processing-failed`, `sink-failed`) with shared envelope including source topic/partition/offset and original payload bytes.
- [ ] **REQ-DLQ-02**: Per-class retry policy (no retry for schema/transition/late; 3× exponential backoff for transient/sink) and a Python CLI replay tool capable of bulk + single replay against original or override topic.
- [ ] **REQ-DLQ-03**: All DLQ topics mirrored to Iceberg `dlq_events` table for Trino-queryable analytics and LLM agent grounding.
- [ ] **REQ-LAKEHOUSE-01**: Iceberg tables on MinIO with JDBC catalog (Postgres-backed); ~16 raw event tables + denormalized `fact_transactions` / `fact_flows` / `fact_screening_outcomes` tables + 5+ continuously-aggregated `agg_*` tables driven by Flink upserts.
- [ ] **REQ-LAKEHOUSE-02**: Hidden partitioning (`days(event_time)` plus bucket-by-customer/flow/hash where applicable); hourly compaction targeting 128MB Parquet files with ZSTD compression; daily snapshot expiration; 7-day time-travel window.
- [ ] **REQ-LAKEHOUSE-03**: Schema evolution via additive Avro field changes propagated through `ALTER TABLE` migration jobs; destructive changes rejected by CI.
- [ ] **REQ-SEARCH-01**: OpenSearch indices (`transactions`, `flows`, `dlq_events`) with strict 35-field mapping, money stored as `long` micro-units (never `float`), dual `text + keyword` mapping for searchable IDs, and ILM-driven monthly rollover with hot/warm/delete tiers.
- [ ] **REQ-API-01**: Spring Boot 4.x API (`apps/api/`) following the stablebridge-tx-recovery reference standards verbatim — hexagonal architecture, Java 25, Gradle Kotlin DSL multi-module (`-api`, `-client`, main), Namastack outbox, MapStruct, generic StateMachine, ArchUnit-enforced layering, four test source sets.
- [ ] **REQ-API-02**: ~16 endpoints across customer / admin / agent / actuator surfaces with `@Secured` role-based access, JWT bearer auth, server-side customer scoping, idempotency-key support, Bucket4j rate limits, OpenAPI codegen for the web client.
- [ ] **REQ-WEB-01**: Next.js 15 App Router + React 19 + TypeScript 5.7 + Tailwind 4 + shadcn/ui + TanStack Query + Auth.js v5 + Vercel AI SDK web app at `apps/web/` with 9-page surface (login, transactions, transaction detail with state-machine timeline, flow detail with multi-leg view, customer summary, admin DLQ inspector + replay, stuck-withdrawal dashboard, agent chat).
- [ ] **REQ-WEB-02**: Real-time updates via SSE for live activity tile and short-poll for non-terminal transactions/flows.
- [ ] **REQ-AGENT-01**: LangGraph 2.0 self-correcting agentic-RAG agent (`apps/llm-agent/`) with planner → query-rewriter → tool-executor → relevance grader → answer drafter → hallucination grader → quality grader → server-side validator graph; OpenAI GPT-4o for reasoning + GPT-4o-mini for graders; Postgres-backed checkpointer.
- [ ] **REQ-AGENT-02**: Three constrained tools (`query_analytics`, `search_events`, `fetch_timeline`) exposed as a single MCP server (`apps/agent-tools-mcp/`) consumed by the LangGraph agent via `MultiServerMCPClient` and reusable by any MCP-compatible client.
- [ ] **REQ-AGENT-03**: Defense-in-depth hallucination prevention — Pydantic input validation, server-side SQL allowlist (Trino parser-validated against `fact_*`/`agg_*` only), in-graph LLM-as-judge graders, eight-rule final response validator (citations, no-invented-refs, no-invented-tables, numeric grounding, no-future-claims, currency formatting, privacy-boundary, refuse-on-missing-data).
- [ ] **REQ-AGENT-EVAL-01**: 80-question golden eval set spanning analytics / incident-triage / walkthrough / refusal / cross-customer-leak categories.
- [ ] **REQ-AGENT-EVAL-02**: Three-layer eval — pytest unit (100%), `agentevals` trajectory match (≥ 90%), LLM-as-judge regression (faithfulness ≥ 4.2/5, relevance ≥ 4.0/5, citations 100%, format 100%) — gated in CI on every PR touching agent code.
- [ ] **REQ-AGENT-EVAL-03**: Production trace sampling — Langfuse weekly samples N=100 traces and re-applies LLM-judge rubrics for drift detection.
- [ ] **REQ-OBS-01**: Prometheus + Grafana + Loki + Tempo + Langfuse self-hosted observability stack with 5 metric categories (pipeline / infra / business / agent / DLQ), 15 alert rules (8 page / 7 warn), and 4+1 Grafana dashboards.
- [ ] **REQ-OBS-02**: OTEL trace context propagation across Producer → Kafka header → Flink → Sink → API → UI → MCP → agent → LLM, with `trace_id` carried in the Avro envelope.
- [ ] **REQ-OBS-03**: Structured JSON logging across every service with PII-masking processors enforced and a CI step that fails the build if known PII fixtures appear in E2E logs.
- [ ] **REQ-CI-01**: Ten path-filtered GitHub Actions workflows; schema-compatibility CI gate (BACKWARD); agent-eval CI gate; nightly full-pipeline E2E that spins up Compose, drives 100 transactions, and verifies OpenSearch, Iceberg, API, and agent answers.
- [ ] **REQ-CI-02**: Trunk-based branching, SemVer release tags, weekly auto-PR for dependency upgrades.
- [ ] **REQ-TESTING-01**: Per-component unit / integration / E2E layers; property-based state-machine tests; ArchUnit + import-linter architecture tests; Toxiproxy-driven chaos tests; k6 load tests pre-release.
- [ ] **REQ-PRIVACY-01**: PII inventory documented; field-level masking in logs/traces/Sentry; `redact-PII` admin endpoint that updates OpenSearch + Iceberg in place (no hard-deletes — financial records retained 7 years for audit).
- [ ] **REQ-PRIVACY-02**: Customer-scope enforcement at three layers (server-side filter in repository adapters, agent guardrail rule 7, integration test asserting cross-customer 404 not 403).
- [ ] **REQ-PLATFORM-01**: Pluggable `EventSource` (Faker / replay / external-kafka), pluggable `LlmProvider` (OpenAI / Anthropic / Ollama), pluggable schema domain via `schemas/*.avsc` regeneration script.
- [ ] **REQ-PLATFORM-02**: `docs/EXTENDING.md`, `docs/ARCHITECTURE.md`, `docs/EVENT-MODEL.md`, `docs/RUNBOOK.md`, `docs/STACK.md`, `docs/EVAL-METHODOLOGY.md` published with worked forking example (~2-day fork target for an experienced engineer).
- [ ] **REQ-DEPLOY-01**: Single-machine Docker Compose v2 deployment with split files (`docker-compose.yml` core, `docker-compose.observability.yml` optional) and `just up` / `just up-observability` orchestration.

### Out of Scope

- **Cloud deployment automation (AWS / EKS / Helm / Terraform)** — documented as future-state in `infra/CLOUD.md` but not built in v1; Compose-only target keeps reviewer setup friction near zero.
- **Tap into a real upstream Kafka cluster as the production source** — the `ExternalKafkaSource` adapter exists as a stub with documentation; no live integration tested.
- **OAuth2 / Keycloak SSO** — local JWT issuer covers the auth story; Keycloak adds RAM cost without learning value at v1.
- **Mobile UI / native apps** — web only.
- **GraphQL API surface** — REST-only via OpenAPI; GraphQL adds complexity without unique value here.
- **dbt / SQLMesh batch transforms** — Flink-driven incremental aggs handle v1's analytical needs; dbt deferred.
- **Automated cost analytics on cloud spend** — not deployed in cloud.
- **Real cryptocurrency operations** — no actual chain RPCs, signing keys, or vault integrations; everything is simulated to demonstrate the pipeline pattern.
- **Stripe-Bridge-style on/off-ramp partner integrations** — partners are simulated providers; no real banking-partner contracts.
- **Multi-region / disaster recovery design** — single-region Compose; DR documented as future-state only.
- **Schema Registry license / Apicurio swap** — Confluent SR (Confluent Community License) is fine for self-hosted; we don't need an Apache-2.0-only registry at v1.

## Context

**Domain reference:** The event model and state machines are inspired by an internal reference architecture for a stablecoin/fiat payment platform (annotated docs at `docs/service-architecture/08-payments.md` and `12-layer1.md`, present locally on the developer's machine but not redistributed). All names — events, topics, statuses, services, accounts — are renamed to domain-native payments-industry vocabulary so the project is generic and source-platform-independent.

**Reference data-pipeline stack:** Architectural baseline drawn from the open-source `abeltavares/real-time-data-pipeline` repository (Python+Faker → Kafka → Flink → Iceberg/MinIO → Trino → Superset). We extend it with OpenSearch, Spring Boot API, Next.js UI, LangGraph agent, Langfuse observability, schema registry, DLQ topology, and full CI/eval/test discipline.

**Java service standards:** The Spring Boot API and any Java code (Flink jobs included where applicable) follow the conventions in `stablebridge-tx-recovery/docs/{ADR.md, CODING_STANDARDS.md, PROJECT_STRUCTURE.md, TESTING_STANDARDS.md}` verbatim — hexagonal layering, immutable record-based domain models, generic `StateMachine<S, T>`, Namastack transactional outbox, MapStruct, JUnit 5 + Mockito BDD + AssertJ with the golden recursive-comparison rule, ArchUnit enforcement, four test source sets.

**Audience:** Forward-looking reference platform — primarily for the developer's portfolio and ongoing reuse, but designed so anyone can fork it and adapt to a different event domain in ~2 days.

**Constraint reality:** Single 16GB-laptop development target. Throughput tier (~200 evt/s sustained, 2000 evt/s burst) chosen so partition counts, Flink parallelism, and Iceberg partitioning decisions are non-trivial without OOM-ing the laptop. Observability stack split into an opt-in compose file so the always-on minimum is laptop-friendly.

## Constraints

- **Language**: Java 25 LTS for backend service + Flink jobs; Python 3.13+ for simulator + LLM agent + lakehouse-jobs + DLQ tools; TypeScript 5.7+ strict mode for web. — *Why*: matches reference standards, latest stable, polyglot is appropriate per component.
- **Build**: Gradle 9.x Kotlin DSL multi-module for Java; uv for Python; pnpm 10 for TypeScript; `just` at the repo root. — *Why*: Q2.1 lock; native tooling per language minimizes orchestration surface area.
- **Spring Boot**: 4.0.x with Spring Cloud 2025.x. — *Why*: stablebridge ADR baseline, latest stable.
- **Wire format**: Avro + Confluent Schema Registry, BACKWARD compatibility, Avro-as-source-of-truth for codegen. — *Why*: production-grade Kafka pattern, Iceberg-friendly, evolvable.
- **State of money**: `BigDecimal + CurrencyCode` (nv-i18n) in domain layer; `long` micros + `keyword` currency in storage. Conversion at infrastructure-layer mapper. — *Why*: type safety in code, efficiency in storage, no float-money mistakes.
- **Throughput target**: 200 evt/s sustained, periodic 2000 evt/s × 30s bursts. — *Why*: laptop-tractable, non-trivial enough to make architecture decisions meaningful.
- **Exactly-once**: idempotent Kafka producer with transactions + Flink 2PC iceberg sink + idempotent OpenSearch writes (`_id` = event_id) + Flink 60s checkpoints with RocksDB state on MinIO.
- **No source-platform names**: zero references to the source company anywhere in code, docs, comments, or commits. Renamed taxonomy throughout.
- **Latest stable versions**: pin latest stable for every dependency; CI weekly auto-PR for upgrades.
- **Compose-only deploy in v1**: no cloud, no Kubernetes; future-state cloud deployment is documented but unbuilt.
- **PII**: BigDecimal-style explicit care: field inventory, masking in logs/traces, `redact-PII` admin endpoint, customer-scope enforcement at three layers.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| High-fidelity (option A) event schemas with renamed vocabulary | Realistic state machines drive credible LLM RAG, OpenSearch search, dashboard storytelling; rename keeps source-platform anonymous | — Pending |
| Single polyglot monorepo with `just` | One clone, atomic cross-cutting PRs, no polyrepo coordination overhead at solo-dev scale | — Pending |
| Avro + Confluent SR + Avro-canonical codegen | Production-grade Kafka pattern with strong evolution guarantees; minimizes drift | — Pending |
| 200 evt/s sustained + 2000 evt/s burst | Tractable on laptop, partition-count-meaningful, demonstrates backpressure under load | — Pending |
| Five payment flows with multi-leg `payment_flow` parent + `flow_id` propagation | Models the differentiating stablecoin sandwich pattern explicitly, enables deterministic streaming joins | — Pending |
| Java service locked to stablebridge-tx-recovery standards verbatim | Reuses a fully-considered ADR; cross-project consistency for the developer | — Pending |
| LangGraph 2.0 + MCP-wrapped tools + 3 in-graph graders + server-side validator | Modern agentic-RAG pattern with defense-in-depth hallucination prevention; tools reusable beyond LangChain via MCP | — Pending |
| Langfuse self-hosted over LangSmith | OSS, OTEL-native, fits "production-grade reference platform" framing better than a SaaS dependency | — Pending |
| OpenSearch + Iceberg dual-write rather than OpenSearch-only or Trino-only | OpenSearch for sub-50ms transaction lookups, Iceberg + Trino for analytics; mirrors reference architecture's actual division | — Pending |
| Money in `BigDecimal + CurrencyCode` domain / `long` micros storage | Type safety in business logic + efficient lakehouse + no float arithmetic on money | — Pending |
| 80-question golden eval set + three-layer eval + CI gate | Real failure-mode coverage; trajectory + LLM-judge + production sampling matches 2026 best practice | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-04-29 after initialization*

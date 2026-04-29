# Java service standards — index

This directory holds the four canonical Java/Spring Boot reference documents that govern `apps/api/` and any Java code in `apps/flink-jobs/`. They have been adapted to use this project's domain (`com.stablepay.payments`), aggregates (Transaction, Flow, DlqMessage, IdempotencyKey, Customer), Testcontainers (Postgres / OpenSearch / Trino / Kafka), and external integrations (OpenSearch + Trino + Kafka, no Feign-to-peer-microservices in v1).

## Documents

| Doc | Scope |
|---|---|
| [ADR.md](ADR.md) | 20-section architecture decision record covering build, hexagonal architecture, domain modeling, CQRS, state machine, outbox, locking, API design, error handling, observability, testing strategy, and conventions |
| [CODING_STANDARDS.md](CODING_STANDARDS.md) | Hexagonal layout rules, Lombok policy, naming, functional style, DI, null handling, money representation, idempotency, error handling |
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | Multi-module Gradle layout, package tree, source set layout, file-placement decision tree |
| [TESTING_STANDARDS.md](TESTING_STANDARDS.md) | Four-source-set test pyramid, golden recursive-comparison rule, fixture conventions, ArchUnit rules, coverage gates |

## Locked project values

| Concept | Value |
|---|---|
| Java package root | `com.stablepay.payments` |
| Service module | `apps/api/` containing `stablepay-api-api`, `stablepay-api-client`, `stablepay-api` (main) |
| Error code prefix | `STBLPAY-XXXX` (4-digit zero-padded) |
| Java version | 25 LTS |
| Spring Boot | 4.0.x |
| Database | PostgreSQL 17 + Flyway |
| Outbox | Namastack Outbox Starter JDBC |
| Mapping | MapStruct (`componentModel = "spring"`) |
| Money in domain | `BigDecimal + CurrencyCode` (nv-i18n) |
| Money in storage | `long` micros + currency `keyword` (converted at infrastructure-layer mapper) |
| ID type pattern | `record TransactionId(UUID value)`, `FlowId`, `CustomerId`, `DlqId` |

## Scope clarifications

- **State machines (ADR §5)**: the Spring Boot API in v1 is read-mostly. The `StateMachine<S, T>` framework is documented as the pattern when write-side aggregates are added in a future phase. The customer-status mapping table in §5 is what the API computes from `internalStatus` returned by Trino/OpenSearch.
- **Pessimistic locking (ADR §7)**: not applicable in v1 (no balance-affecting writes). Documented as the correct pattern when balance writes are added.
- **Feign clients (ADR §12)**: the API does not call peer microservices via Feign in v1. Downstream systems are OpenSearch (`spring-data-opensearch`), Trino (JDBC), Kafka (Namastack outbox).
- **Outbox (ADR §6, CODING §5.2)**: the API publishes one event type — `dlq.replay.command.v1` — from `POST /admin/dlq/{id}/replay`. The pattern is wired up to scale.
- **Batch processing**: deliberately not in scope for v1. The simulator drives event volume, not batch payouts.

## What governs in case of conflict

If a decision in this directory conflicts with one locked in `.planning/PROJECT.md`, `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, or `CLAUDE.md`, **the project artifacts win for project-specific decisions** (e.g., five-flow taxonomy, OpenSearch dual-write, LangGraph agent design, sandwich correlator). These four docs win for generic Java/Spring Boot conventions (hexagonal layout, Lombok rules, recursive-comparison test rule, ArchUnit enforcement, etc.).

## Maintenance

These docs were adapted from a sibling reference project. When that project updates its standards, the relevant changes can be ported over with care: keep the project-specific values (`com.stablepay.payments` package, `STBLPAY-XXXX` error codes, our domain entity names, our Testcontainer choices, our external integration patterns) and update only the generic patterns. Do not propagate project-specific adaptations back upstream.

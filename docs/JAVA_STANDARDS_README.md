# Java service standards — index

This directory holds the four canonical Java/Spring Boot reference documents that govern `apps/api/` and any Java code in `apps/flink-jobs/`. They were imported from a sibling reference project; project-specific variable values are listed below.

## Documents

| Doc | Scope |
|---|---|
| [ADR.md](ADR.md) | 21-section architecture decision record covering build, hexagonal architecture, domain modeling, CQRS, state machine, outbox, locking, API design, error handling, observability, testing strategy, and conventions |
| [CODING_STANDARDS.md](CODING_STANDARDS.md) | Hexagonal layout rules, Lombok policy, naming, functional style, DI, null handling, money representation, idempotency, error handling |
| [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) | Multi-module Gradle layout, package tree, source set layout, file-placement decision tree |
| [TESTING_STANDARDS.md](TESTING_STANDARDS.md) | Four-source-set test pyramid, golden recursive-comparison rule, fixture conventions, ArchUnit rules, coverage gates |

## Placeholder values for this project

When the imported docs use template placeholders, the values for `stablepay-payment-pipeline` are:

| Placeholder | Value | Notes |
|---|---|---|
| `{org}` / `<org>` | `stablepay` | Top-level Java package and Maven group root |
| `{domain}` | `payments` | Sub-domain segment |
| `banking.{domain}` | `payments` | Drop the `banking.` segment — package root is `com.stablepay.payments` (we don't have `banking` as an intermediate segment) |
| `{service-name}` | `stablepay-api` | Single Java service in v1; module names: `stablepay-api-api`, `stablepay-api-client`, `stablepay-api` (main) |
| Service prefix in error codes | `STBLPAY` | Error codes formatted `STBLPAY-XXXX` (4-digit zero-padded) |
| Aggregate examples in docs (Payout, Wallet) | Map to project entities | When the docs use `Payout` examples, map to whichever aggregate is being implemented (transaction queries, flow drill-down, DLQ replay, etc.) |

## Concrete project-specific resolutions

Wherever the imported docs reference patterns that the project has already locked specifically:

- **State machine examples in ADR §5** describing `MERCHANT_4_EYES_APPROVAL_REQUESTED → ...` are illustrative of *the state machine pattern*, not the literal status names this project uses. Our renamed taxonomy lives in `.planning/PROJECT.md` and the Avro schemas under `schemas/` (not in this directory).
- **External service adapters in ADR §12** (`BankingApiAdapter`, `WalletServiceAdapter`, etc.) are illustrative of the Feign-adapter pattern; this project's external adapters connect to OpenSearch, Trino, and Postgres rather than peer microservices in v1.
- **Outbox usage** in CODING_STANDARDS §5.2 is in scope — the Spring Boot API uses Namastack outbox for the `dlq.replay.command.v1` event published from `POST /admin/dlq/{id}/replay`.
- **Pessimistic locking** in ADR §7 applies if/when the API gains write paths that touch balances; v1 is read-mostly so this primarily applies to the DLQ replay write.

## What governs in case of conflict

If a decision in this directory conflicts with one locked in `.planning/PROJECT.md`, `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, or `CLAUDE.md`, **the project artifacts win for project-specific decisions** (e.g., five-flow taxonomy, OpenSearch dual-write, LangGraph agent design). The imported docs win for generic Java/Spring Boot conventions (hexagonal layout, Lombok rules, recursive-comparison test rule, etc.).

## Updates

When the upstream reference project updates its standards, run a 3-way diff (this directory ↔ upstream ↔ this project's adaptations) and decide whether to pull. Adaptations to keep the project-specific values consistent are documented above; do not propagate them back upstream.

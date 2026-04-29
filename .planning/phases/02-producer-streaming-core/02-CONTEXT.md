# Phase 2: Producer & Streaming Core - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 delivers the Python state-machine simulator (`apps/simulator/`) that emits all five payment flows at ~200 evt/s sustained with periodic 2000 evt/s bursts, plus the Flink job set (`apps/flink-jobs/`) that consumes every Kafka topic, validates Avro envelopes, and dual-writes to Iceberg raw tables + an initial OpenSearch `transactions` index — with exactly-once semantics, event-time watermarks, and the four DLQ topics wired up. Also delivers the sandwich correlator as a separate Flink job that joins child events by `flow_id` and emits `payment.flow.v1` lifecycle updates.

**Requirements covered:** ING-01..05, STM-01..05, DLQ-01..02, LAK-01..02, SCH-01..03, SCH-06

**In scope:** Python simulator with state machines for all 5 flows, pluggable `EventSource` protocol with `FakerSource` implementation, idempotent transactional Kafka producer, Flink session cluster in Compose (JobManager + TaskManager), ingest Flink job (consume → validate → dual-write), sandwich correlator Flink job, Iceberg raw tables (one per topic), OpenSearch `transactions` index with strict ~35-field mapping, DLQ side-output routing for 4 error classes, Compose services for Flink + OpenSearch.

**Out of scope (deferred):** Fact/agg tables (Phase 3), DLQ Iceberg mirror + replay CLI (Phase 3), OpenSearch ILM rollover + `flows`/`dlq_events` indices (Phase 3), Trino/Superset (Phase 3), Spring Boot API (Phase 4), Iceberg maintenance jobs (Phase 3).

</domain>

<decisions>
## Implementation Decisions

### Simulator state-machine design

- **D-01:** Transition model — Hybrid: scenario-driven at entity creation (selects broad path: happy/failure/edge), probabilistic branching within each scenario for sub-variations. Scenario templates guarantee coherent end-to-end stories; per-step probabilities add realism. Scenario weights and per-step probabilities configurable via YAML config file.
- **D-02:** Timing — Compressed with configurable multiplier. State machines define realistic base delays (screening 5-30s, partner ack 10-60s). Default `--delay-multiplier 0.01` for throughput targets (200 evt/s), `--delay-multiplier 1.0` for realistic demo mode. `just simulate` hits throughput; `just simulate --realistic` produces believable dashboard timing.
- **D-03:** Multi-leg flow coordination — Simulator emits only child events (payin, trade, payout) with shared `flow_id` in the envelope. Simulator also emits one initial `payment.flow.v1` with `INITIATED` status as the "intent" event. All subsequent flow lifecycle updates are produced by the Flink sandwich correlator (Plan 2.4), not the simulator. This validates the correlator with real traffic from day one.
- **D-04:** Flow type distribution — Weighted random: fiat payout (30%), crypto payout (20%), fiat payin (20%), crypto payin (15%), multi-leg payment flow (15%). Weights configurable in the same YAML config.

### Flink job topology & deployment

- **D-05:** Job split — Two Flink jobs: (1) Ingest job — consumes all payment/chain/aux topics, validates Avro, dual-writes to Iceberg + OpenSearch, DLQ routing via side outputs. (2) Sandwich correlator — keyed by `flow_id`, joins child events to parent flow, emits `payment.flow.v1` lifecycle updates. Different keying, state shape, and failure domains justify the split.
- **D-06:** Deployment mode — Flink session cluster in Compose (JobManager + TaskManager). Provides the Flink Web UI for debugging watermarks, checkpoints, backpressure — useful for portfolio demos. Two jobs submitted to the single cluster.
- **D-07:** Parallelism — Ingest job: parallelism=2 (handles 6-partition payment topics). Correlator: parallelism=1 (3-partition `payment.flow.v1`, lower volume). TaskManager configured with 3 total slots.
- **D-08:** Flink resource budget — JobManager 512MB + TaskManager 1.5GB ≈ 2GB total. Fits within the 16GB laptop constraint alongside existing Compose services (~3GB from Phase 1) + OpenSearch (~1GB).
- **D-09:** Checkpointing — 60s checkpoint interval, RocksDB state backend with MinIO (`flink-checkpoints` bucket) as checkpoint storage, 3 retained checkpoints, fixed-delay restart strategy (3 attempts, 30s delay).

### Iceberg table layout & OpenSearch mapping

- **D-10:** Iceberg raw tables — One table per topic (9 tables): `raw_payment_payout_fiat`, `raw_payment_payout_crypto`, `raw_payment_payin_fiat`, `raw_payment_payin_crypto`, `raw_payment_flow`, `raw_chain_transaction`, `raw_signing_request`, `raw_screening_result`, `raw_approval_decision`. Matches Avro contract 1:1 with no nullable-column bloat.
- **D-11:** Partitioning — All raw tables: `days(event_time)` hidden partition. `raw_chain_transaction` adds `bucket(8, hash)` secondary partition per LAK-02. No secondary partition on others at 200 evt/s volume.
- **D-12:** OpenSearch `transactions` index — Single index (no ILM rollover at Phase 2; that's Phase 3 SCH-04). Strict mapping with all ~35 fields from SCH-02. All payment event types (payin/payout x fiat/crypto) go into this one index with `event_type`, `flow_type`, `direction`, `is_crypto`, `is_user_facing` discriminator fields. Money as `long` micros + `keyword` currency per D-05 from Phase 1.
- **D-13:** Dual-write architecture — Two independent sinks branching from the same validated stream (not sequential). Iceberg sink: Flink `IcebergSink` with 2PC (exactly-once via checkpoint). OpenSearch sink: custom `AsyncSinkFunction` with bulk API (1000 docs / 5MB / 5s flush per SCH-06), idempotent on `event_id` as `_id`. Each sink has its own error handling routing to `dlq.sink-failed.v1`. OS being slow/down does not block Iceberg writes.

### DLQ routing & error classification

- **D-14:** Classification point — Errors classified at the point they occur in the Flink pipeline, routed via side outputs:
  - `SCHEMA_INVALID` — Avro deserialization failure (first pipeline step). No retry.
  - `LATE_EVENT` — `envelope.event_time` before current watermark minus 60s buffer (after deser, before sink). No retry.
  - `ILLEGAL_TRANSITION` — Status not a valid successor for entity's last-seen status in keyed state. No retry.
  - `SINK_FAILURE` — Iceberg or OpenSearch sink error. Transient (timeout, connection refused, 429): 3x exponential backoff (1s → 5s → 25s). Permanent (mapping conflict): immediate, no retry.
- **D-15:** Transition validation strictness — Lenient with logging at Phase 2. Valid transition graph defined as a static lookup table per flow type. Violations routed to DLQ as side effect but the event ALSO proceeds through the main pipeline. Config flag `STBLPAY_FLINK_STRICT_TRANSITIONS=false` (default) controls this. Tighten to strict rejection in a later pass after simulator tuning.
- **D-16:** DLQ envelope — Uses existing DLQ Avro schemas from Phase 1 (already generated). Carries: source topic, partition, offset, original payload bytes, error class enum, error message, `failed_at` timestamp, retry count. Counters exposed as Flink metrics per DLQ-02.

### Edge cases & additional clarifications

- **D-17:** `ReplaySource` scope at Phase 2 — Only the `EventSource` protocol (abstract interface) and `FakerSource` (default) are built. `ReplaySource` is a stub (interface + `NotImplementedError`) with a docstring describing future behavior (replay from Iceberg raw table dump). `ExternalKafkaSource` is also stub-only per ING-04. Full `ReplaySource` implementation deferred to Phase 3 or later when Iceberg raw tables exist to replay from.
- **D-18:** Burst mode mechanics — Burst bypasses the delay multiplier entirely (instant emit at max throughput for 30s). Burst spikes ALL flow types proportionally (same weighted distribution as steady state per D-04). Burst interval configurable: default every ~10 minutes, flag `--burst-interval 600` (seconds). Burst targets 2000 evt/s aggregate across all topics.
- **D-19:** OpenSearch deployment in Compose — Single-node cluster (`discovery.type=single-node`, `OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m`). Security plugin disabled for dev (`DISABLE_SECURITY_PLUGIN=true`). Memory budget: 1GB total. Port 9200 exposed. Phase 6 may add security plugin + TLS per SEC-03.

### Claude's Discretion
- Specific Flink connector versions (iceberg-flink, opensearch-flink-connector or custom) — researcher determines latest stable compatible set
- Internal simulator package structure (`apps/simulator/src/stablepay_simulator/`) — models/, sources/, producers/, config/ breakdown
- Flink job main class naming and Java package structure within `apps/flink-jobs/`
- OpenSearch index template JSON structure and field mapping details (follow SCH-02 requirements)
- Iceberg table DDL specifics (Flink SQL CREATE TABLE statements)
- Flink `AsyncSinkFunction` vs `ElasticsearchSink` for OpenSearch — pick the better-maintained option
- Compose service names and port assignments for Flink JobManager, TaskManager, OpenSearch

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project planning (always)
- `.planning/PROJECT.md` — project context, constraints, key decisions
- `.planning/REQUIREMENTS.md` — v1 requirement IDs (ING-01..05, STM-01..05, DLQ-01..02, LAK-01..02, SCH-01..03, SCH-06 are Phase 2)
- `.planning/ROADMAP.md` — Phase 2 goal, plans 2.1-2.5, success criteria
- `.planning/STATE.md` — current project state
- `CLAUDE.md` — hard constraints (no source-platform refs, money-never-as-float, latest-stable-versions)

### Phase 1 context (decisions that bind Phase 2)
- `.planning/phases/01-foundation-schemas/01-CONTEXT.md` — Avro schema design decisions (D-01..D-78), topic naming, SR strategy, codegen layout, Compose topology

### Avro schemas (Phase 2 consumes)
- `schemas/src/main/avro/` — all 17 `.avsc` files; Phase 2 simulator and Flink jobs consume these as the contract
- `schemas/src/main/avro/common/event_envelope.avsc` — `EventEnvelope` record structure (7 fields)
- `schemas/src/main/avro/payout-fiat/payment_payout_fiat.avsc` — fiat payout with 31 `FiatPayoutInternalStatus` enum values (reference for state machine)
- `schemas/src/main/avro/dlq/` — 4 DLQ envelope schemas

### Infrastructure (Phase 2 extends)
- `infra/docker-compose.yml` — existing Compose with Kafka, SR, Postgres, MinIO (Phase 2 adds Flink + OpenSearch services)
- `infra/kafka/topics.yaml` — 13 topics manifest (simulator produces to these, Flink consumes them)

### Java service standards (Flink jobs follow where applicable)
- `docs/CODING_STANDARDS.md` — Java naming, functional style, SLF4J logging
- `docs/JAVA_STANDARDS_README.md` — placeholder mapping (org=stablepay)

### Generated code (Phase 2 depends on)
- `packages/schemas-py/src/_generated/` — Python dataclasses for simulator
- `schemas/build.gradle.kts` — Java Avro POJOs for Flink jobs (`:schemas` module)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `packages/schemas-py` — Generated Python dataclasses for all 17 Avro schemas. Simulator imports these directly for event construction.
- `:schemas` Gradle module — Generated Java Avro POJOs. Flink jobs depend on this via `implementation(project(":schemas"))` (already declared in `apps/flink-jobs/build.gradle.kts`).
- `infra/kafka/topics.yaml` — Topic manifest with partition counts. Simulator reads topic names; Flink subscribes to them.
- `infra/docker-compose.yml` — Running Kafka 4.0 + SR 7.9 + Postgres 17 + MinIO. Phase 2 adds Flink + OpenSearch as new services.

### Established Patterns
- Avro `EventEnvelope` composition (D-02 from Phase 1) — every event has `record.envelope.event_id`, `.event_time`, `.flow_id`, etc. Flink deserializer extracts envelope first for validation.
- Money as `long amount_micros` + `string currency_code` (D-05 from Phase 1) — Flink passes through to both Iceberg and OpenSearch without conversion.
- Topic naming `{domain}.{action}.{rail}.v1` (D-08 from Phase 1) — consistent consumer group naming follows this pattern.
- Single bridge network `stablepay_default` (D-27) — Flink + OpenSearch join the same network.
- Named volumes for persistence (D-28) — add `opensearch_data`, `flink_data` volumes.

### Integration Points
- Simulator → Kafka: transactional producer writing to all 13 topics
- Kafka → Flink ingest job: consumes 9 payment/chain/aux topics
- Flink → Iceberg: 2PC sink to 9 raw tables in MinIO `warehouse` bucket
- Flink → OpenSearch: async bulk sink to `transactions` index
- Flink → DLQ topics: side-output to 4 `dlq.*` topics
- Flink correlator → Kafka: produces `payment.flow.v1` lifecycle events
- Flink → MinIO: RocksDB checkpoint storage in `flink-checkpoints` bucket

</code_context>

<specifics>
## Specific Ideas

- **Simulator scenarios as YAML** — scenario definitions (happy path, screening rejection, partial refund, etc.) in `apps/simulator/config/scenarios.yaml`. Each scenario is a named sequence of states with probabilistic branches. Tunable without code changes.
- **Delay multiplier CLI flag** — `just simulate` = fast mode (0.01x), `just simulate --realistic` = real timing (1.0x). Good for portfolio Grafana demos vs throughput testing.
- **Flink Web UI exposed** — session cluster exposes JobManager dashboard on a Compose port. Useful for debugging watermarks, checkpoint timing, backpressure visualization.
- **Lenient transition validation** — `STBLPAY_FLINK_STRICT_TRANSITIONS=false` default. Events with invalid transitions are DLQ'd AND still processed, allowing the pipeline to run while the simulator's edge cases are tuned.
- **Initial `payment.flow.v1 INITIATED` from simulator** — only the intent event comes from the simulator; all subsequent flow status updates come from the Flink correlator.

</specifics>

<deferred>
## Deferred Ideas

- **Strict transition validation** — flip `STBLPAY_FLINK_STRICT_TRANSITIONS=true` after simulator stabilization (Phase 3 or later)
- **OpenSearch ILM rollover** — Phase 3 SCH-04 adds hot/warm/delete tiers with monthly + 50GB rollover
- **`flows` and `dlq_events` OpenSearch indices** — Phase 3 adds these
- **DLQ Iceberg mirror + replay CLI** — Phase 3 DLQ-03..05
- **Iceberg fact tables and continuous aggregations** — Phase 3 LAK-03..07
- **Trino + Superset** — Phase 3
- **Flink continuous aggregation jobs** — Phase 3 STM-06
- **Per-component MinIO IAM** — Phase 6 hardening

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-Producer & Streaming Core*
*Context gathered: 2026-04-29*

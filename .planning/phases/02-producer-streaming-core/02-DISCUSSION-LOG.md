# Phase 2: Producer & Streaming Core - Discussion Log

**Date:** 2026-04-29
**Mode:** Default (interactive)
**Areas discussed:** 4/4

---

## Area 1: Simulator State-Machine Design

**Questions explored:**
1. How should state transitions branch? (Probabilistic vs scenario-driven vs hybrid)
2. How should timing work? (Realistic delays vs compressed)
3. How should multi-leg `payment_flow` parent coordinate? (Simulator-emitted vs correlator-produced)

**Decisions:**
- Hybrid model: scenario-driven at creation, probabilistic within paths
- Compressed timing with configurable `--delay-multiplier` (0.01 default, 1.0 for realistic)
- Simulator emits child events + one initial INITIATED flow event; correlator produces all subsequent flow updates

**User response:** Accepted Claude's recommendation without modification.

---

## Area 2: Flink Job Topology & Deployment

**Questions explored:**
1. How many Flink jobs? (Monolithic vs per-concern vs two-job split)
2. Deployment mode in Compose? (Session cluster vs application mode)
3. Parallelism settings?

**Decisions:**
- Two jobs: ingest+DLQ (coupled by side-output relationship) and correlator (different keying/state)
- Session cluster for Flink Web UI debugging/demo value
- Parallelism 2 for ingest, 1 for correlator, 3 TaskManager slots total
- Resource budget: JM 512MB + TM 1.5GB ≈ 2GB

**User response:** Accepted Claude's recommendation without modification.

---

## Area 3: Iceberg Table Layout & OpenSearch Mapping

**Questions explored:**
1. How many Iceberg raw tables? (Per-topic vs consolidated)
2. Partitioning strategy?
3. OpenSearch `transactions` index scope at Phase 2?
4. Dual-write sink architecture?

**Decisions:**
- One table per topic (9 tables), matching Avro 1:1
- `days(event_time)` partition on all; `bucket(8, hash)` secondary on `raw_chain_transaction`
- Single `transactions` index, strict ~35-field mapping, no ILM yet
- Two independent sinks (not sequential) — Iceberg 2PC + OpenSearch async bulk

**User response:** Accepted Claude's recommendation without modification.

---

## Area 4: DLQ Routing & Error Classification

**Questions explored:**
1. Where does classification happen in the pipeline?
2. Should ILLEGAL_TRANSITION validation be strict or lenient?

**Decisions:**
- Layered classification at point of occurrence via side outputs
- Four error classes mapped to four DLQ topics with per-class retry policy
- Lenient transition validation at Phase 2 (DLQ + still process), config flag to tighten later

**User response:** Accepted Claude's recommendation without modification.

---

## Deferred Ideas

None captured — all discussion stayed within phase scope.

---

*Generated: 2026-04-29*

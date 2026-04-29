# STATE — stablepay-payment-pipeline

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-04-29 after initialization)

**Core value:** A single-`git clone`, single-`just up` reference platform that demonstrates every production-grade pattern a modern stablecoin/fiat payment data system requires — high-fidelity event modeling, exactly-once stream processing, lakehouse + search dual-write, agentic SQL/RAG with hallucination guardrails, and full observability — with credible end-to-end testing and clear extension points.

**Current focus:** Phase 1 — Foundation & Schemas

---

## Phase status

| # | Phase | Status |
|---|---|---|
| 1 | Foundation & Schemas | 📋 Pending plan |
| 2 | Producer & Streaming Core | ⬜ Not started |
| 3 | Lakehouse & Analytics | ⬜ Not started |
| 4 | API & Web | ⬜ Not started |
| 5 | LLM Agent & MCP | ⬜ Not started |
| 6 | Observability & Hardening | ⬜ Not started |
| 7 | CI/CD & Test Discipline | ⬜ Not started |
| 8 | Platform Polish & Documentation | ⬜ Not started |

## Workflow config

- **Mode**: YOLO (auto-approve, just execute)
- **Granularity**: Standard (5–8 phases, 3–5 plans each)
- **Parallelization**: Enabled
- **Model profile**: Balanced (Sonnet for most agents)
- **Workflow agents**: Research ✓ | Plan check ✓ | Verifier ✓ | Nyquist validation ✓
- **Auto-advance**: Disabled (user reviews each phase transition explicitly)

## Memory references

- `feedback_no_bvnk_references.md` — never name the source company; renamed taxonomy throughout
- `reference_java_standards.md` — Java service follows stablebridge-tx-recovery `docs/{ADR,CODING_STANDARDS,PROJECT_STRUCTURE,TESTING_STANDARDS}.md` verbatim
- `project_java_stack_locked.md` — Java side: Java 25 + Spring Boot 4.0.x + Gradle Kotlin DSL multi-module
- `feedback_latest_versions.md` — pin latest stable for every dependency

## Reference materials

- Domain reference (event model + state machines): `~/Documents/AI/bank/organized-codebase/docs/service-architecture/{08-payments.md, 12-layer1.md}` — read locally only, never redistributed, names not propagated.
- Reference data-pipeline stack: `github.com/abeltavares/real-time-data-pipeline` (Python+Faker → Kafka → Flink → Iceberg/MinIO → Trino → Superset).
- Java service standards: `~/Documents/AI/github/stablebridge-tx-recovery/docs/`.

## Next action

Run `/gsd-discuss-phase 1` to gather phase-1 context before planning, or `/gsd-plan-phase 1` to plan directly.

---
*Last updated: 2026-04-29 after initialization*

# stablepay-payment-pipeline

> **WIP — not yet feature-complete.** See [`.planning/ROADMAP.md`](.planning/ROADMAP.md) for phase status.

Real-time data pipeline for stablecoin and fiat payment events.

```
Producer ──► Kafka ──► Flink ──► Iceberg + OpenSearch
                                      │
                              Trino ◄──┘──► API ──► Web
                                            │
                                        LLM Agent
```

## Quick Start

```bash
git clone <repo-url> && cd stablepay-payment-pipeline
cp .env.example .env
just up
just preflight
```

## Just Recipes

| Recipe | Description |
|--------|-------------|
| `just up` | Bring up core stack (Kafka, SR, Postgres, MinIO) |
| `just down` | Tear down (preserves volumes) |
| `just nuke` | Tear down + destroy volumes |
| `just logs` | Show service logs |
| `just preflight` | Health + topic + SR readiness check |
| `just regenerate-schemas` | Regen Java + Python from Avro |
| `just test` | Run all tests (stub) |
| `just ci-all` | Run CI checks locally (stub) |

## Documentation

- [`docs/SETUP.md`](docs/SETUP.md) — Detailed setup instructions
- [`docs/ADR.md`](docs/ADR.md) — Architecture decision record
- [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) — Java coding standards
- [`docs/TESTING_STANDARDS.md`](docs/TESTING_STANDARDS.md) — Testing conventions

## License

MIT

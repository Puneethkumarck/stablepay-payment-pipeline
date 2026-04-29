# Contributing

## Setup

1. Install [lefthook](https://github.com/evilmartians/lefthook): `pnpm install && lefthook install`
2. After schema changes: `just regenerate-schemas`
3. Pre-commit hooks run automatically (gitleaks, ruff-format, commitlint)

## Commit Messages

We use [Conventional Commits](https://www.conventionalcommits.org/). Format:

```
type(scope): description
```

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`, `ci`, `build`, `perf`

Scopes: `schemas`, `api`, `web`, `simulator`, `llm-agent`, `agent-tools-mcp`, `dlq-tools`, `lakehouse-jobs`, `flink-jobs`, `infra`, `deploy`, `ci`, `docs`, `tooling`, `auth`

## Standards

- Java: see [`docs/ADR.md`](docs/ADR.md), [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)
- Testing: see [`docs/TESTING_STANDARDS.md`](docs/TESTING_STANDARDS.md)

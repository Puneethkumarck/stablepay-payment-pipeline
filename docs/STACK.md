# Stack pin list

This document tracks every dependency in the stablepay-payment-pipeline project. Renovate Bot (`renovate.json`) opens grouped PRs weekly when newer stable releases are available.

CLAUDE.md hard constraint #4 requires latest-stable pins everywhere. Any pin below latest stable must be justified here in one line.

## Phase 4 Stack Pins (2026-05-01)

| Dep | Version | Reason for non-latest pin |
|-----|---------|---------------------------|
| `next-auth` | `5.0.0-beta.31` | Auth.js v5 GA still pending; v4 has known App Router friction. Beta-as-stable accepted. |
| `eslint` | `9.39.4` | ESLint 10.3.0 is latest but `eslint-config-next@16.2.6` triggers `scopeManager.addGlobals` crash. Bump when upstream fixes. |
| `org.mapstruct:mapstruct` | `1.6.3` | 1.7.0.Beta1 is the only newer release; beta excluded per project policy. Bump on 1.7.0 GA. |

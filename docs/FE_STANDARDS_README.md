# Frontend standards — index

This directory holds the three canonical frontend reference documents that govern `apps/web/`. They codify the Next.js 16 + TypeScript 6 + Tailwind 4 + shadcn/ui conventions for this project, paralleling the Java standards (`CODING_STANDARDS.md`, `TESTING_STANDARDS.md`, `PROJECT_STRUCTURE.md`) that govern `apps/api/`.

## Documents

| Doc | Scope |
|---|---|
| [FE_CODING_STANDARDS.md](FE_CODING_STANDARDS.md) | App Router architecture, TypeScript rules, component patterns (RSC vs client), Tailwind + shadcn styling, data fetching (React Query + MSW), state management (Zustand), auth (Auth.js v5), error handling, naming, imports, linting, security |
| [FE_TESTING_STANDARDS.md](FE_TESTING_STANDARDS.md) | Three-layer test pyramid (Vitest unit + MSW integration + Playwright E2E), component/hook/utility testing patterns, fixture factories, coverage gates, anti-patterns |
| [FE_PROJECT_STRUCTURE.md](FE_PROJECT_STRUCTURE.md) | Directory layout, route structure (App Router), component/hook/lib/server/test/types directories, file placement decision tree, configuration file inventory, dependency management |

## Locked project values

| Concept | Value |
|---|---|
| Framework | Next.js 16 App Router with `src/` directory |
| Language | TypeScript 6.0.3 strict mode (`noUncheckedIndexedAccess`, `noImplicitOverride`) |
| Styling | Tailwind 4 (CSS-first config via `@theme` in `globals.css`) |
| Component primitives | shadcn (CLI v4.7.0, base-nova style, Radix + Base UI) |
| Icon library | `lucide-react@1.14.0` |
| Data fetching | `@tanstack/react-query@5.x` (client-side), RSC `async/await` (server-side) |
| State management | Zustand (client UI state), React Query (server state), URL params (filter/sort) |
| Authentication | Auth.js v5 (`next-auth@5.0.0-beta.31`) |
| Formatter | Biome 2.4.14 (single quotes, semicolons, 100-char width) |
| Linter | ESLint 9.39.4 flat config (`eslint-config-next/core-web-vitals`) |
| Unit tests | Vitest 4.1.5 + Testing Library + jsdom |
| API mocking | MSW 2.14.5 (network-level mocking — never mock React Query) |
| E2E tests | Playwright 1.59.1 |
| API client codegen | `@hey-api/openapi-ts@0.97.1` → `src/lib/api-client/_generated/` |
| Build output | `output: 'standalone'` for multi-stage Docker build |
| Path alias | `~/*` → `./src/*` |
| Package manager | pnpm 11.0.8 (exact pins, no `^`/`~`) |

## Visual design contract

The visual and interaction specification lives in `.planning/phases/04-api-web/04-UI-SPEC.md`. It defines:
- Design tokens (colors, typography, spacing, radii, shadows, motion)
- Page inventory (9 pages with route mapping)
- Component inventory (translated from the HTML prototype)
- Status badge color mapping (60+ statuses)
- Accessibility requirements

The three standards docs in this directory govern **how** to implement; `04-UI-SPEC.md` governs **what** to implement.

## Scope clarifications

- **shadcn components (`src/components/ui/`)**: managed by the shadcn CLI. Never hand-edit. Override by wrapping in `src/components/`.
- **Generated API client (`src/lib/api-client/_generated/`)**: managed by `@hey-api/openapi-ts`. Regenerate with `pnpm openapi:gen`. Never hand-edit.
- **ESLint pinned to 9.39.4** (not 10.x): `eslint-config-next` has a `scopeManager.addGlobals` incompatibility with ESLint 10. Bump when upstream fixes land. Documented in `docs/STACK.md`.
- **Auth.js v5 beta**: `next-auth@5.0.0-beta.31` is pinned as beta-as-stable. v5 is the App Router-native version; v4 has known friction with Next.js 16. Documented in `docs/STACK.md`.

## What governs in case of conflict

If a decision in this directory conflicts with one in `.planning/PROJECT.md`, `.planning/REQUIREMENTS.md`, `.planning/ROADMAP.md`, `CLAUDE.md`, or `04-UI-SPEC.md`:
- **Project artifacts win** for project-specific decisions (page inventory, visual tokens, status taxonomy, API contract).
- **These docs win** for generic frontend conventions (component patterns, testing approach, file placement, naming, linting).

## Maintenance

When dependencies are bumped by the weekly `ci-version-bump.yml` workflow or manually, update the version numbers in these docs if they are referenced explicitly (e.g., shadcn CLI version in `FE_PROJECT_STRUCTURE.md`). The `package.json` exact pins are the source of truth — these docs provide context, not authoritative version numbers.

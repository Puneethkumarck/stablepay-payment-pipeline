# Project Structure — apps/web

> Physical layout of the Next.js 16 web application in `apps/web/`. For coding standards see [FE_CODING_STANDARDS.md](FE_CODING_STANDARDS.md), for testing rules see [FE_TESTING_STANDARDS.md](FE_TESTING_STANDARDS.md), for visual/interaction contract see `.planning/phases/04-api-web/04-UI-SPEC.md`.

---

## 1. Top-Level Layout

```
apps/web/
├── biome.json                     # Biome 2 formatter + linter config
├── components.json                # shadcn CLI config (aliases, style, registry)
├── eslint.config.mjs              # ESLint 9 flat config (next/core-web-vitals)
├── next.config.mjs                # Next.js config (CSP, headers, rewrites, standalone)
├── package.json                   # Pinned manifest (exact versions, no ^/~)
├── postcss.config.mjs             # Tailwind 4 PostCSS plugin
├── tsconfig.json                  # TypeScript 6 strict mode
├── vite.config.ts                 # Vitest 4 config (test environment, plugins)
├── playwright.config.ts           # Playwright E2E config
├── public/                        # Static assets (logos, favicons)
│   ├── logo-mark.svg
│   └── logo-wordmark.svg
└── src/                           # Application source
    ├── app/                       # Next.js App Router routes
    ├── components/                # React components
    ├── hooks/                     # Custom React hooks
    ├── lib/                       # Utilities and shared logic
    ├── server/                    # Server-only code (auth, actions)
    ├── test/                      # Test utilities (setup, fixtures, MSW)
    └── types/                     # Shared TypeScript types
```

---

## 2. Route Structure (App Router)

```
src/app/
├── globals.css                    # Tailwind imports + shadcn theme tokens (CSS variables)
├── layout.tsx                     # Root layout: fonts, global providers, <html>/<body>
├── not-found.tsx                  # Global 404 page
├── login/
│   └── page.tsx                   # Public login page
├── (authed)/                      # Protected route group (middleware-guarded)
│   ├── layout.tsx                 # Authed layout: sidebar, live-feed panel, providers
│   ├── page.tsx                   # Dashboard (/)
│   ├── transactions/
│   │   ├── page.tsx               # Transaction list (/transactions)
│   │   └── [ref]/
│   │       └── page.tsx           # Transaction detail (/transactions/[ref])
│   ├── flows/
│   │   └── [id]/
│   │       └── page.tsx           # Flow detail (/flows/[id])
│   ├── customers/
│   │   └── [id]/
│   │       └── summary/
│   │           └── page.tsx       # Customer summary (/customers/[id]/summary)
│   └── admin/
│       ├── dlq/
│       │   ├── page.tsx           # DLQ list (/admin/dlq)
│       │   └── [id]/
│       │       └── page.tsx       # DLQ detail (/admin/dlq/[id])
│       └── stuck/
│           └── page.tsx           # Stuck payments (/admin/stuck)
└── api/                           # Next.js API routes (if needed — prefer rewrites)
```

**Route group conventions:**
- `(authed)` wraps all authenticated routes. `src/middleware.ts` redirects unauthenticated users to `/login`.
- Each route segment can have: `page.tsx` (route UI), `layout.tsx` (shared layout), `loading.tsx` (Suspense fallback), `error.tsx` (error boundary), `not-found.tsx` (404), `forbidden.tsx` (403), `unauthorized.tsx` (401).
- `forbidden.tsx` renders when `forbidden()` is called from `next/navigation` — use for authorization failures. `unauthorized.tsx` renders when `unauthorized()` is called — use for unauthenticated access.
- Route pages use `export default function` (Next.js convention).

---

## 3. Component Directory

```
src/components/
├── ui/                            # shadcn primitives (CLI-managed — DO NOT hand-edit)
│   ├── alert-dialog.tsx
│   ├── avatar.tsx
│   ├── badge.tsx
│   ├── button.tsx
│   ├── card.tsx
│   ├── dialog.tsx
│   ├── drawer.tsx
│   ├── dropdown-menu.tsx
│   ├── input.tsx
│   ├── label.tsx
│   ├── select.tsx
│   ├── separator.tsx
│   ├── sheet.tsx
│   ├── skeleton.tsx
│   ├── sonner.tsx
│   ├── switch.tsx
│   └── table.tsx
├── layout/                        # Layout components
│   ├── sidebar.tsx                # Main navigation sidebar
│   ├── page-header.tsx            # Page title + eyebrow + actions
│   └── user-dropdown.tsx          # User menu in sidebar footer
├── status-badge.tsx               # Status pill with semantic color + optional pulse
├── amount.tsx                     # Money display (currency-aware formatting)
├── id-chip.tsx                    # Truncated ID with copy-to-clipboard
├── data-table.tsx                 # Table with sorting, pagination, skeleton rows
├── search-bar.tsx                 # Search input with icon
├── session-expired-banner.tsx     # Sticky banner for expired sessions
├── not-found-card.tsx             # 404 card (non-enumerating copy)
└── live-feed/                     # Live activity panel (SSE-powered)
    ├── live-feed-panel.tsx
    └── live-feed-entry.tsx
```

**Rules:**
- `ui/` is shadcn-managed. Add primitives with `pnpm dlx shadcn@4.7.0 add <name>`.
- App-specific components live directly in `components/` or in named subdirectories (`layout/`, `live-feed/`).
- Co-locate tests: `status-badge.tsx` has `status-badge.test.tsx` next to it.
- One component per file. No multi-component files.

---

## 4. Hooks Directory

```
src/hooks/
├── use-sidebar.ts                 # Sidebar collapsed state (localStorage-backed)
├── use-live-feed.ts               # SSE connection for live transaction events
├── use-copy-to-clipboard.ts       # Copy text to clipboard with success feedback
├── use-debounce.ts                # Debounced value for search inputs
└── use-media-query.ts             # Responsive breakpoint detection
```

**Rules:**
- One hook per file. File name matches hook name in kebab-case: `useSidebar` → `use-sidebar.ts`.
- Hooks are always `'use client'` (they use React state/effects).
- Co-locate tests: `use-sidebar.ts` has `use-sidebar.test.ts` next to it.

---

## 5. Lib Directory

```
src/lib/
├── utils.ts                       # cn() class utility (clsx + tailwind-merge)
├── format-money.ts                # Money formatting (currency-aware, micros → display)
├── format-date.ts                 # Date/time formatting (relative + absolute)
├── format-id.ts                   # ID truncation (8 chars + "…")
├── constants.ts                   # App-wide constants (polling intervals, page sizes)
├── stores/                        # Zustand stores
│   ├── sidebar-store.ts
│   └── live-feed-store.ts
└── api-client/                    # API client layer
    ├── _generated/                # OpenAPI codegen output (DO NOT hand-edit)
    ├── client.ts                  # Configured client instance (base URL, auth headers)
    └── queries.ts                 # React Query wrapper functions
```

**Rules:**
- `_generated/` is auto-generated by `@hey-api/openapi-ts`. Run `pnpm openapi:gen` to regenerate.
- `queries.ts` exports functions that return `useQuery` / `useMutation` options objects, not the hooks themselves. This allows RSC and client components to share query logic.
- Zustand stores in `stores/` — one file per store, named `{concern}-store.ts`.

---

## 6. Server Directory

```
src/server/
├── auth.ts                        # Auth.js v5 configuration (providers, callbacks)
└── actions/                       # Server Actions (if needed)
    └── replay-dlq.ts              # DLQ replay server action
```

**Rules:**
- Everything in `src/server/` is server-only. ESLint `no-restricted-imports` blocks client components from importing these files.
- Auth config exports `auth()`, `signIn()`, `signOut()` from Auth.js v5.
- Server Actions use `'use server'` directive and are imported by client components for mutations.

---

## 7. Test Directory

```
src/test/
├── setup.ts                       # Vitest global setup (jest-dom matchers, MSW lifecycle)
├── render.tsx                     # Custom render with providers (QueryClient, theme)
├── msw-server.ts                  # MSW server for Vitest (node)
├── msw-handlers.ts                # Default happy-path API handlers
└── fixtures/                      # Test data factories
    ├── transaction.ts
    ├── flow.ts
    ├── customer.ts
    └── dlq-entry.ts
```

---

## 8. Types Directory

```
src/types/
├── api.ts                         # Re-exports from generated client (convenience aliases)
└── navigation.ts                  # Typed route params (augments Next.js typed routes)
```

**Rules:**
- Only put types here if shared across 3+ files. Otherwise co-locate with the component/hook.
- Prefer importing directly from the OpenAPI-generated client over re-defining types.

---

## 9. File Placement Decision Tree

```
Need a new file?
    │
    ├── Is it a route page/layout/loading/error/not-found/forbidden/unauthorized?
    │   └── src/app/<route-segment>/page.tsx (etc.)
    │
    ├── Is it a shadcn primitive?
    │   └── pnpm dlx shadcn@4.7.0 add <name> → src/components/ui/
    │
    ├── Is it a reusable UI component?
    │   └── src/components/<name>.tsx
    │   └── (with layout concern? → src/components/layout/<name>.tsx)
    │
    ├── Is it a custom React hook?
    │   └── src/hooks/use-<name>.ts
    │
    ├── Is it a pure utility function?
    │   └── src/lib/<name>.ts
    │
    ├── Is it a Zustand store?
    │   └── src/lib/stores/<name>-store.ts
    │
    ├── Is it server-only code (auth, server actions)?
    │   └── src/server/<name>.ts
    │
    ├── Is it a shared TypeScript type?
    │   └── src/types/<name>.ts (only if used in 3+ files)
    │
    ├── Is it a test fixture/factory?
    │   └── src/test/fixtures/<entity>.ts
    │
    ├── Is it a test utility (render helper, MSW handler)?
    │   └── src/test/<name>.ts
    │
    └── Is it a test for an existing file?
        └── Co-locate: <source-file>.test.ts(x) next to the source
```

---

## 10. Key Configuration Files

| File | Purpose | Hand-edit? |
|------|---------|-----------|
| `package.json` | Dependency manifest with exact pins | Yes — but prefer `pnpm add <pkg>` then remove `^` |
| `tsconfig.json` | TypeScript strict config + path aliases | Rarely — Next.js may auto-update on build |
| `next.config.mjs` | CSP headers, security headers, API rewrites, standalone output | Yes |
| `biome.json` | Formatter (quotes, semicolons, width) + lint rules | Rarely |
| `eslint.config.mjs` | Next.js-aware lint rules (flat config) | Rarely |
| `postcss.config.mjs` | Tailwind 4 PostCSS plugin | No |
| `components.json` | shadcn CLI config (aliases, style preset) | No — managed by `shadcn init` |
| `vite.config.ts` | Vitest config (environment, plugins, setup files) | When adding test plugins |
| `playwright.config.ts` | E2E config (base URL, browsers, timeouts) | When changing E2E setup |
| `src/app/globals.css` | Tailwind imports + full CSS variable theme | Yes — theme token changes |

---

## 11. Dependency Management

**Rules:**
- All versions are exact pins (no `^` or `~`). The weekly `ci-version-bump.yml` is the upgrade path.
- After `pnpm dlx shadcn@4.7.0 add <component>`, check `package.json` for any `^` prefixes shadcn may have added and remove them.
- `pnpm-lock.yaml` is committed. Never `.gitignore` it.
- The `packageManager` field in `package.json` locks the pnpm version (`pnpm@11.0.8`). Corepack enforces this.

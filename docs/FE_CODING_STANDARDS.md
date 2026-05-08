# Frontend Coding Standards — apps/web

Instructions for coding agents developing the Next.js 16 web application in `apps/web/`.

## Table of Contents

- [1. Architecture: Next.js App Router](#1-architecture-nextjs-app-router)
- [2. TypeScript](#2-typescript)
- [3. Component Patterns](#3-component-patterns)
  - [3.1 Server Components (Default)](#31-server-components-default)
  - [3.2 Client Components](#32-client-components)
  - [3.3 Component File Structure](#33-component-file-structure)
  - [3.4 React 19 Patterns](#34-react-19-patterns)
- [4. Styling](#4-styling)
  - [4.1 Tailwind 4](#41-tailwind-4)
  - [4.2 shadcn/ui Components](#42-shadcnui-components)
  - [4.3 The cn() Utility](#43-the-cn-utility)
  - [4.4 CVA for Variants](#44-cva-for-variants)
- [5. Data Fetching](#5-data-fetching)
  - [5.1 Server-Side Fetching](#51-server-side-fetching)
  - [5.2 Client-Side Fetching](#52-client-side-fetching)
  - [5.3 API Client](#53-api-client)
- [6. State Management](#6-state-management)
- [7. Authentication](#7-authentication)
- [8. Error Handling](#8-error-handling)
- [9. Forms and Validation](#9-forms-and-validation)
- [10. Naming and File Conventions](#10-naming-and-file-conventions)
- [11. Imports](#11-imports)
- [12. Linting and Formatting](#12-linting-and-formatting)
- [13. Security](#13-security)
- [14. Quick Reference Checklist](#14-quick-reference-checklist)

---

## 1. Architecture: Next.js App Router

The web app uses **Next.js 16 App Router** with the `src/` directory layout:

```
apps/web/src/
├── app/                    # Route segments (App Router)
│   ├── (authed)/           # Protected route group (session-guarded)
│   ├── login/              # Public routes
│   ├── globals.css         # Tailwind + shadcn theme tokens
│   ├── layout.tsx          # Root layout (fonts, providers)
│   └── not-found.tsx       # Global 404
├── components/
│   ├── ui/                 # shadcn primitives (managed by shadcn CLI — do not hand-edit)
│   └── *.tsx               # App-specific components
├── hooks/                  # Custom React hooks
├── lib/
│   ├── utils.ts            # cn() and shared utilities
│   └── api-client/         # OpenAPI-generated client (auto-generated — do not hand-edit)
│       └── _generated/     # Output of @hey-api/openapi-ts
├── server/                 # Server-only code (auth config, server actions)
└── types/                  # Shared TypeScript types
```

**Rules:**
- `src/server/` MUST NOT be imported from client components. ESLint `no-restricted-imports` enforces this.
- `src/components/ui/` is managed by the shadcn CLI. Do not hand-edit files in this directory. Override behavior by wrapping components in `src/components/`.
- `src/lib/api-client/_generated/` is auto-generated from the OpenAPI spec. Do not hand-edit.
- Route segments follow Next.js conventions: `page.tsx`, `layout.tsx`, `loading.tsx`, `error.tsx`, `not-found.tsx`, `forbidden.tsx`, `unauthorized.tsx`.
- Prefer `next.config.ts` (TypeScript) over `next.config.mjs` for new projects. The current project uses `.mjs` — migrate when convenient.

---

## 2. TypeScript

TypeScript 6 strict mode with all safety flags enabled.

**Rules:**
- `strict: true` plus `noUncheckedIndexedAccess` and `noImplicitOverride` — never disable.
- Use `type` imports for type-only imports: `import type { Foo } from '...'`.
- Never use `any`. Use `unknown` and narrow, or define a proper type.
- Use `satisfies` for type checking without widening: `const config = { ... } satisfies Config`.
- Prefer `interface` for object shapes that may be extended. Use `type` for unions, intersections, and mapped types.
- Use the `~/` path alias for all intra-project imports. Never use relative paths that traverse more than one parent (`../../`).

---

## 3. Component Patterns

### 3.1 Server Components (Default)

All components are React Server Components by default in the App Router. Use RSC for:
- Initial data fetching (no client-side loading spinners on first paint)
- Pages that display read-only data
- Layouts and metadata

```tsx
// app/(authed)/transactions/page.tsx — Server Component (default)
export default async function TransactionsPage() {
  const transactions = await fetchTransactions();
  return <TransactionList data={transactions} />;
}
```

### 3.2 Client Components

Add `'use client'` only when the component needs:
- Event handlers (`onClick`, `onChange`, etc.)
- React hooks (`useState`, `useEffect`, `useQuery`, etc.)
- Browser APIs (`localStorage`, `IntersectionObserver`, etc.)

```tsx
'use client';

import { useState } from 'react';

export function SearchBar() {
  const [query, setQuery] = useState('');
  // ...
}
```

**Rules:**
- Push `'use client'` boundaries as low as possible. Wrap the interactive part, not the whole page.
- Never put `'use client'` on a layout or a page component if only a child needs interactivity.
- Prefer composition: RSC parent with a client child, not a client parent wrapping RSC children.

### 3.4 React 19 Patterns

React 19 introduces several APIs that replace older patterns:

**`ref` as a prop (no `forwardRef`):** React 19 passes `ref` as a regular prop. Never use `forwardRef` — it is deprecated.

```tsx
// correct — ref as a prop
function Input({ ref, className, ...props }: ComponentProps<'input'>) {
  return <input ref={ref} className={cn('...', className)} {...props} />;
}

// wrong — forwardRef is deprecated in React 19
const Input = forwardRef<HTMLInputElement, InputProps>((props, ref) => { ... });
```

**`use()` hook for unwrapping promises and context:** Use `use()` to read a promise passed from a Server Component to a Client Component, or to read context conditionally.

```tsx
'use client';

import { use } from 'react';

function TransactionList({ dataPromise }: { dataPromise: Promise<Transaction[]> }) {
  const data = use(dataPromise);
  return (/* ... */);
}
```

**`useOptimistic` for optimistic UI updates:** Use `useOptimistic` for instant feedback on mutations before the server confirms.

```tsx
'use client';

import { useOptimistic } from 'react';

function DlqList({ entries }: { entries: DlqEntry[] }) {
  const [optimisticEntries, addOptimistic] = useOptimistic(
    entries,
    (state, replayedId: string) =>
      state.map((e) => (e.id === replayedId ? { ...e, status: 'REPLAYING' } : e)),
  );
  // ...
}
```

### 3.3 Component File Structure

```tsx
// 1. Directive (if client component)
'use client';

// 2. Imports (type imports first, then libraries, then local)
import type { ComponentProps } from 'react';
import { useState } from 'react';
import { cn } from '~/lib/utils';

// 3. Types (co-located, not in a separate file unless shared)
interface StatusBadgeProps {
  status: string;
  pulse?: boolean;
}

// 4. Component (named function, not arrow function for top-level components)
function StatusBadge({ status, pulse = false }: StatusBadgeProps) {
  return (/* ... */);
}

// 5. Named export (not default export — except page.tsx, layout.tsx, etc.)
export { StatusBadge };
```

**Rules:**
- Use named function declarations for components, not `const Foo = () => {}`.
- Use named exports for reusable components. Only use `export default` for Next.js route files (`page.tsx`, `layout.tsx`, `loading.tsx`, `error.tsx`, `not-found.tsx`).
- Co-locate types with the component file. Extract to `src/types/` only if shared across 3+ files.
- Props interfaces are named `{ComponentName}Props`.
- Destructure props in the function signature.

---

## 4. Styling

### 4.1 Tailwind 4

Tailwind 4 uses CSS-first configuration via `@theme` in `globals.css`. No `tailwind.config.ts` file.

**Rules:**
- Use Tailwind utility classes for all styling. No inline `style` props unless required for dynamic values that Tailwind cannot express (e.g., `style={{ '--progress': `${percent}%` }}`).
- No CSS modules. No styled-components. No emotion.
- Use the design tokens from `globals.css` via CSS variables (`var(--surface-1)`) or Tailwind theme classes (`bg-primary`, `text-muted-foreground`).
- Responsive: mobile-first (`sm:`, `md:`, `lg:`). The dashboard is desktop-primary but must not break on tablet.

### 4.2 shadcn/ui Components

shadcn primitives live in `src/components/ui/` and are managed by the shadcn CLI.

**Rules:**
- Do NOT hand-edit files in `src/components/ui/`. If you need to customize, create a wrapper in `src/components/` that composes the primitive.
- Install new primitives via: `pnpm dlx shadcn@4.7.0 add <component> --cwd apps/web`.
- Compose primitives to build app-specific components. Example: `<DataTable>` wraps shadcn `<Table>` with sorting, pagination, and skeleton rows.
- Use the `data-slot` attribute pattern that shadcn components use for styling hooks.

### 4.3 The cn() Utility

All conditional class composition goes through `cn()` from `~/lib/utils`:

```tsx
import { cn } from '~/lib/utils';

<div className={cn('base-class', isActive && 'active-class', className)} />
```

Never concatenate class strings manually. Never use template literals for classes.

### 4.4 CVA for Variants

Use `class-variance-authority` for components with multiple variants:

```tsx
import { cva, type VariantProps } from 'class-variance-authority';

const badgeVariants = cva('inline-flex items-center rounded-full', {
  variants: {
    variant: {
      success: 'bg-success/10 text-success border-success/22',
      danger: 'bg-danger/10 text-danger border-danger/24',
    },
    size: {
      sm: 'px-2 py-0.5 text-xs',
      md: 'px-2.5 py-1 text-sm',
    },
  },
  defaultVariants: { variant: 'success', size: 'md' },
});
```

---

## 5. Data Fetching

### 5.1 Server-Side Fetching

For initial page loads, fetch data in Server Components using `async/await`:

```tsx
export default async function TransactionsPage() {
  const data = await api.getTransactions();
  return <TransactionList initialData={data} />;
}
```

**`use cache` directive:** Next.js 16 supports the `'use cache'` directive for caching function or component output at the server level. Use it for expensive computations or slow API calls that can tolerate staleness:

```tsx
async function getDashboardStats() {
  'use cache';
  return await api.getDashboardStats();
}
```

Use `cacheLife()` and `cacheTag()` from `next/cache` to control TTL and on-demand revalidation. Only apply `'use cache'` to data that is safe to serve stale — never for user-specific or real-time data.

### 5.2 Client-Side Fetching

For polling, mutations, and client-driven queries, use `@tanstack/react-query`:

```tsx
'use client';

import { useQuery } from '@tanstack/react-query';

function TransactionList({ initialData }: { initialData: Transaction[] }) {
  const { data } = useQuery({
    queryKey: ['transactions'],
    queryFn: () => api.getTransactions(),
    initialData,
    refetchInterval: 3000,
  });
  return (/* ... */);
}
```

**Rules:**
- Query keys are string tuples: `['transactions']`, `['transaction', ref]`, `['flows', id]`.
- Mutations use `useMutation` with `onSuccess` invalidation.
- Never use `useEffect` + `fetch` for data fetching. Always use React Query.
- Polling intervals: 3s for transaction lists (non-terminal), 10s for dashboard stats.

### 5.3 API Client

The OpenAPI-generated client in `src/lib/api-client/_generated/` is the single source of truth for API types and methods. Regenerate with `pnpm openapi:gen`.

**Rules:**
- Import API types from the generated client, never define duplicate types manually.
- Wrap generated client calls in a thin service layer if you need to add auth headers, error mapping, or caching logic.

---

## 6. State Management

| What | Where |
|------|-------|
| Server state (API data) | React Query (`@tanstack/react-query`) |
| Client UI state (sidebar collapsed, theme) | Zustand store or `localStorage` |
| Form state | React hook form or controlled components |
| URL state (filters, pagination cursors) | `useSearchParams()` from `next/navigation` |
| Auth session | Auth.js v5 (`next-auth`) |

**Rules:**
- Never duplicate server state in Zustand. React Query is the cache.
- Zustand stores are in `src/lib/stores/`. One store per concern (e.g., `sidebar-store.ts`, `live-feed-store.ts`).
- Prefer URL search params for filter/sort state so links are shareable.

---

## 7. Authentication

Auth.js v5 (`next-auth@5.0.0-beta.31`) handles authentication.

**Rules:**
- Auth config lives in `src/server/auth.ts`. Never import this from client components.
- Middleware at `src/middleware.ts` protects the `(authed)` route group.
- Use `auth()` in Server Components, `useSession()` in Client Components.
- Customer scope is enforced server-side (CLAUDE.md hard constraint #7). The frontend trusts the JWT `customer_id` and never sends cross-customer requests.
- Session-expired state is handled by `<SessionExpiredBanner>` — never silently swallow 401s.

---

## 8. Error Handling

**Rules:**
- Every route group has an `error.tsx` boundary that catches render errors and shows a recoverable UI.
- Every route group has a `not-found.tsx` for 404 states.
- Use `forbidden.tsx` for 403 responses — triggered by calling `forbidden()` from `next/navigation` in Server Components or middleware. Renders a "not authorized" UI without leaking resource existence.
- Use `unauthorized.tsx` for 401 responses — triggered by calling `unauthorized()` from `next/navigation`. Renders a login prompt or session-expired UI.
- API errors from React Query surface via the `error` state and render an inline error card — never a toast for load failures. Toasts are for transient feedback (successful mutations, rate-limit warnings).
- `sonner` is the toast library. Use semantic variants: `toast.success()`, `toast.error()`, `toast.warning()`.
- Never swallow errors silently. Log to console in development; in production, errors flow to the error boundary.

---

## 9. Forms and Validation

**Rules:**
- Client-side validation is for UX only. The API is authoritative.
- Use controlled components for simple forms (1-3 fields). Use a form library for complex forms.
- Disable submit buttons during mutation (`isPending` from `useMutation`).
- Show inline field errors below inputs, not as toasts.

**Server Action forms with `useActionState`:** For mutations backed by Server Actions, use `useActionState` (React 19) instead of `useMutation`. It handles pending state and return values automatically:

```tsx
'use client';

import { useActionState } from 'react';
import { replayDlqEntry } from '~/server/actions/replay-dlq';

function ReplayButton({ entryId }: { entryId: string }) {
  const [state, formAction, isPending] = useActionState(replayDlqEntry, null);
  return (
    <form action={formAction}>
      <input type="hidden" name="id" value={entryId} />
      <button type="submit" disabled={isPending}>
        {isPending ? 'Replaying…' : 'Replay'}
      </button>
    </form>
  );
}
```

Prefer `useActionState` for Server Action forms and `useMutation` for client-side API calls via React Query. Do not mix both for the same mutation.

---

## 10. Naming and File Conventions

| Thing | Convention | Example |
|-------|-----------|---------|
| Component files | `kebab-case.tsx` | `status-badge.tsx` |
| Component names | `PascalCase` | `StatusBadge` |
| Hook files | `kebab-case.ts` prefixed with `use-` | `use-sidebar.ts` |
| Hook names | `camelCase` prefixed with `use` | `useSidebar` |
| Utility files | `kebab-case.ts` | `format-money.ts` |
| Type files | `kebab-case.ts` | `transaction-types.ts` |
| Route files | Next.js convention | `page.tsx`, `layout.tsx`, `loading.tsx` |
| Constants | `SCREAMING_SNAKE_CASE` | `POLLING_INTERVAL_MS` |
| CSS variables | `--kebab-case` | `--surface-1` |
| Query keys | string tuple | `['transactions', ref]` |

---

## 11. Imports

Import order (enforced by Biome):

1. `'use client'` directive (if present)
2. Type imports (`import type { ... }`)
3. React / Next.js
4. Third-party libraries
5. Local `~/` imports (components, hooks, lib, types)

**Rules:**
- Always use the `~/` path alias for local imports.
- Use `import type` for type-only imports.
- No barrel files (`index.ts` re-exports). Import directly from the source file.
- No circular imports. If two files need each other's types, extract the shared type to `src/types/`.

---

## 12. Linting and Formatting

Two tools run in the `lint` script, each covering different concerns:

| Tool | Version | Scope |
|------|---------|-------|
| Biome | 2.4.14 | Formatting (single quotes, semicolons, 100-char width) + fast lint rules |
| ESLint | 9.39.4 | Next.js-aware rules (`eslint-config-next/core-web-vitals`) |

**Rules:**
- Run `pnpm lint` before committing. Both must pass.
- Biome formatting is authoritative. Single quotes, semicolons always, 2-space indent, 100-char line width.
- Auto-fix with `pnpm lint:fix`.
- `src/lib/api-client/_generated/` and `.next/` are excluded from both linters.

---

## 13. Security

**Rules:**
- CSP headers are configured in `next.config.mjs`. Do not add `unsafe-eval` or `unsafe-inline` to the CSP.
- **Nonce-based CSP (recommended):** Use middleware to generate a per-request nonce and inject it into `script-src` and `style-src` directives. Next.js 16 supports reading the nonce via `headers()` in Server Components. This is stronger than hash-based or `unsafe-inline` CSP. Migrate to nonce-based CSP when implementing middleware.
- Never render user-supplied HTML with `dangerouslySetInnerHTML`.
- Never store tokens in `localStorage`. Auth.js v5 uses HTTP-only cookies.
- API calls go through the Next.js rewrite (`/api/*` → backend). Never expose the internal API URL to the browser.
- PII masking (CLAUDE.md hard constraint #6): the frontend does not log customer-identifying data to the browser console. Use masked field accessors where available.

---

## 14. Quick Reference Checklist

Before submitting any frontend code:

- [ ] TypeScript strict mode passes (`pnpm typecheck`)
- [ ] Biome check passes (`pnpm exec biome check .`)
- [ ] ESLint passes (`pnpm exec eslint .`)
- [ ] Next.js build succeeds (`pnpm build`)
- [ ] `'use client'` boundary pushed as low as possible
- [ ] No hand-edits to `src/components/ui/` or `src/lib/api-client/_generated/`
- [ ] Data fetched via RSC or React Query — no raw `useEffect` + `fetch`
- [ ] Server imports (`~/server/**`) not imported from client components
- [ ] No `any` types
- [ ] No inline styles (except dynamic CSS variables)
- [ ] `cn()` used for all conditional class composition
- [ ] Error and loading boundaries present for new routes

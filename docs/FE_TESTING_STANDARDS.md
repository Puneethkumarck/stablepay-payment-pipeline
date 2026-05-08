# Frontend Testing Standards — apps/web

> Mandatory testing rules for the Next.js 16 web application in `apps/web/`.
> Coding agents must follow these rules exactly. Do not deviate unless explicitly instructed.

## Table of Contents

- [1. Test Strategy Overview](#1-test-strategy-overview)
- [2. Test Tooling](#2-test-tooling)
- [3. Test File Placement](#3-test-file-placement)
- [4. Test Structure: Arrange / Act / Assert](#4-test-structure-arrange--act--assert)
- [5. Component Testing](#5-component-testing)
  - [5.1 Rendering](#51-rendering)
  - [5.2 User Interactions](#52-user-interactions)
  - [5.3 Assertions](#53-assertions)
- [6. Hook Testing](#6-hook-testing)
- [7. API / Data Fetching Tests](#7-api--data-fetching-tests)
- [8. MSW for API Mocking](#8-msw-for-api-mocking)
- [9. Integration Tests (Playwright)](#9-integration-tests-playwright)
- [10. Test Fixtures and Factories](#10-test-fixtures-and-factories)
- [11. What to Test and What Not to Test](#11-what-to-test-and-what-not-to-test)
- [12. Coverage and Quality Gates](#12-coverage-and-quality-gates)
- [13. Anti-Patterns](#13-anti-patterns)
- [Quick Reference: Test Cheat Sheet](#quick-reference-test-cheat-sheet)

---

## 1. Test Strategy Overview

Three test layers:

| Layer | Tool | Scope | Location |
|-------|------|-------|----------|
| Unit | Vitest + Testing Library | Components, hooks, utilities | `*.test.ts(x)` co-located |
| Integration | Vitest + MSW | Data flow with mocked API | `*.test.ts(x)` co-located |
| E2E | Playwright | Full browser, real or mocked backend | `tests/e2e/` |

**Pyramid rule:** many unit tests, fewer integration tests, minimal E2E tests for critical paths.

---

## 2. Test Tooling

| Tool | Version | Purpose |
|------|---------|---------|
| `vitest` | 4.1.5 | Test runner + assertion library |
| `@testing-library/react` | 16.3.2 | Component rendering + queries |
| `@testing-library/dom` | 10.4.1 | DOM queries |
| `@testing-library/jest-dom` | 6.9.1 | Custom DOM matchers (`toBeInTheDocument`, `toHaveTextContent`, etc.) |
| `jsdom` | 29.1.1 | DOM environment for Vitest |
| `msw` | 2.14.5 | API mocking (request interception) |
| `@playwright/test` | 1.59.1 | End-to-end browser testing |

Vitest config lives in `vite.config.ts` (Vitest 4 default — not a separate `vitest.config.ts`).

---

## 3. Test File Placement

Tests are **co-located** with the code they test:

```
src/
├── components/
│   ├── status-badge.tsx
│   └── status-badge.test.tsx       # Unit test for StatusBadge
├── hooks/
│   ├── use-sidebar.ts
│   └── use-sidebar.test.ts         # Unit test for useSidebar
├── lib/
│   ├── format-money.ts
│   └── format-money.test.ts        # Unit test for formatMoney
└── app/
    └── (authed)/
        └── transactions/
            └── page.test.tsx        # Integration test for the page
```

**Rules:**
- Test file names mirror the source file: `foo.tsx` → `foo.test.tsx`.
- No `__tests__/` directories. Co-locate tests next to the source.
- Shared test utilities go in `src/test/` (test setup, render helpers, MSW handlers).
- Playwright E2E tests go in the repo-level `tests/e2e/` directory.

---

## 4. Test Structure: Arrange / Act / Assert

Every test follows the AAA pattern with comment markers:

```tsx
it('renders transaction amount in USD format', () => {
  // arrange
  const transaction = createTransaction({ amount: 150000, currency: 'USD' });

  // act
  render(<Amount value={transaction.amount} currency={transaction.currency} />);

  // assert
  expect(screen.getByText('$1,500.00')).toBeInTheDocument();
});
```

**Rules:**
- Use `it()` for test cases, `describe()` for grouping.
- Test names read as sentences: `it('shows error state when API returns 500')`.
- One logical assertion per test. Multiple `expect()` calls are fine if they verify the same behavior.
- Use `// arrange`, `// act`, `// assert` comment markers in every test.

---

## 5. Component Testing

### 5.1 Rendering

Use `render()` from Testing Library. Wrap with necessary providers (QueryClient, theme, etc.) via a custom render helper:

```tsx
// src/test/render.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, type RenderOptions } from '@testing-library/react';
import type { ReactElement } from 'react';

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });
}

function renderWithProviders(ui: ReactElement, options?: RenderOptions) {
  const queryClient = createTestQueryClient();
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>,
    options,
  );
}

export { renderWithProviders as render };
```

### 5.2 User Interactions

Use `@testing-library/user-event` for realistic interactions:

```tsx
import userEvent from '@testing-library/user-event';

it('filters transactions when search query is entered', async () => {
  // arrange
  const user = userEvent.setup();
  render(<SearchBar onSearch={onSearch} />);

  // act
  await user.type(screen.getByRole('searchbox'), 'TXN-001');

  // assert
  expect(onSearch).toHaveBeenCalledWith('TXN-001');
});
```

**Rules:**
- Prefer `userEvent` over `fireEvent` for user interactions.
- Query elements by accessible roles first: `getByRole`, `getByLabelText`, `getByText`. Use `getByTestId` only as a last resort.
- Never query by CSS class or DOM structure.

### 5.3 Assertions

Use Testing Library DOM matchers:

```tsx
expect(screen.getByRole('button', { name: 'Replay' })).toBeInTheDocument();
expect(screen.getByRole('button', { name: 'Replay' })).toBeDisabled();
expect(screen.getByText('Transaction completed')).toHaveClass('text-success');
expect(screen.queryByText('Error')).not.toBeInTheDocument();
```

**Rules:**
- Use `getBy*` when the element must exist (throws if missing).
- Use `queryBy*` when asserting absence.
- Use `findBy*` for async elements (returns a promise, waits for appearance).
- Prefer semantic matchers: `toBeDisabled()` over `toHaveAttribute('disabled')`.

---

## 6. Hook Testing

Test hooks with `renderHook` from Testing Library:

```tsx
import { renderHook, act } from '@testing-library/react';
import { useSidebar } from './use-sidebar';

it('toggles collapsed state', () => {
  // arrange
  const { result } = renderHook(() => useSidebar());

  // act
  act(() => {
    result.current.toggle();
  });

  // assert
  expect(result.current.collapsed).toBe(true);
});
```

---

## 7. API / Data Fetching Tests

Test React Query hooks and data-fetching components with MSW:

```tsx
import { server } from '~/test/msw-server';
import { http, HttpResponse } from 'msw';

it('displays transactions from the API', async () => {
  // arrange
  server.use(
    http.get('/api/v1/transactions', () =>
      HttpResponse.json({ data: [mockTransaction] }),
    ),
  );

  // act
  render(<TransactionList />);

  // assert
  expect(await screen.findByText('TXN-001')).toBeInTheDocument();
});
```

**Rules:**
- Default MSW handlers return happy-path responses. Override per test for error cases.
- Test loading states by delaying MSW responses.
- Test error states by returning error responses.
- Never mock React Query internals (`useQuery`, `useMutation`). Mock the network layer with MSW.

---

## 8. MSW for API Mocking

MSW setup:

```
src/test/
├── msw-server.ts           # Vitest MSW server setup
├── msw-handlers.ts         # Default happy-path handlers
└── msw-browser.ts          # Browser MSW worker (for Playwright dev mode, optional)
```

```tsx
// src/test/msw-server.ts
import { setupServer } from 'msw/node';
import { handlers } from './msw-handlers';

export const server = setupServer(...handlers);
```

Wire up in `vite.config.ts` setup file:

```tsx
// src/test/setup.ts
import '@testing-library/jest-dom/vitest';
import { server } from './msw-server';

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

**Rules:**
- `onUnhandledRequest: 'error'` — fail the test if an unexpected API call is made.
- Reset handlers after each test to prevent state leakage.
- Handler responses use the same types as the OpenAPI-generated client.

---

## 9. Integration Tests (Playwright)

Playwright tests cover critical user flows end-to-end:

| Flow | What it verifies |
|------|-----------------|
| Login → Dashboard | Auth.js session created, dashboard renders stats |
| Transaction list → Detail | Navigation, data loading, polling |
| DLQ replay | Mutation flow, success toast, list refresh |
| Session expired | Banner shown, redirect to login |

```tsx
// tests/e2e/transactions.spec.ts
import { test, expect } from '@playwright/test';

test('navigates from transaction list to detail', async ({ page }) => {
  await page.goto('/transactions');
  await page.getByRole('row', { name: /TXN-001/ }).click();
  await expect(page.getByRole('heading', { name: 'Transaction Detail' })).toBeVisible();
});
```

**Rules:**
- Use accessible selectors: `getByRole`, `getByLabel`, `getByText`.
- Tests run against a dev server with MSW browser worker or a real backend — configured in `playwright.config.ts`.
- Each test is independent. No shared state between tests.
- Run with `pnpm test:e2e`.

---

## 10. Test Fixtures and Factories

Create fixture factories in `src/test/fixtures/`:

```tsx
// src/test/fixtures/transaction.ts
import type { Transaction } from '~/lib/api-client/_generated';

const defaults: Transaction = {
  ref: 'TXN-001',
  status: 'COMPLETED',
  amount: 100000,
  currency: 'USD',
  customerId: 'CUST-001',
  createdAt: '2026-01-01T00:00:00Z',
};

export function createTransaction(overrides?: Partial<Transaction>): Transaction {
  return { ...defaults, ...overrides };
}
```

**Rules:**
- One factory per domain entity.
- Factories return valid default objects. Tests override only the fields relevant to the scenario.
- Fixture files are named `{entity}.ts` in `src/test/fixtures/`.
- Never hard-code test data inline. Always use factories.

---

## 11. What to Test and What Not to Test

**DO test:**
- Components: renders correct output for given props, handles user interactions, shows correct loading/error/empty states.
- Hooks: state transitions, side effects, return values.
- Utilities: pure functions (formatters, validators, transformers).
- Data flows: component renders data from API (via MSW), mutations trigger correct side effects.
- Error boundaries: error states render recovery UI.
- Accessibility: interactive elements have proper roles, labels, and keyboard support.

**DO NOT test:**
- shadcn primitives (`src/components/ui/`) — they are tested upstream.
- Auto-generated API client code (`src/lib/api-client/_generated/`).
- Static layout (CSS class names, pixel-level positioning) — that is visual regression territory.
- Next.js framework behavior (routing, SSR, middleware) — trust the framework.
- Implementation details (internal state shape, component lifecycle order).

---

## 12. Coverage and Quality Gates

| Metric | Target |
|--------|--------|
| Line coverage | ≥ 80% for `src/components/` and `src/hooks/` |
| Branch coverage | ≥ 70% for business logic utilities |
| Critical paths | 100% — login, transaction list, DLQ replay covered by E2E |

**Rules:**
- Coverage is measured by Vitest's built-in coverage (`v8` provider).
- Coverage does not count `src/components/ui/` (shadcn) or `src/lib/api-client/_generated/`.
- New components must ship with tests. PRs adding untested components will be blocked.
- Coverage thresholds are enforced in CI.

---

## 13. Anti-Patterns

| Anti-Pattern | Why it's wrong | Do this instead |
|-------------|---------------|-----------------|
| Snapshot tests for components | Brittle, fail on irrelevant changes, discourage review | Assert specific behavior and content |
| Mocking React Query hooks | Couples tests to implementation, breaks on refactor | Mock the network with MSW |
| `getByTestId` as first choice | Doesn't test accessibility, misses labeling bugs | Use `getByRole`, `getByLabelText`, `getByText` first |
| Testing CSS classes directly | Fragile, meaningless assertions | Assert visible behavior (`toBeVisible`, `toBeDisabled`, text content) |
| Asserting internal state | Couples to implementation | Assert rendered output or returned values |
| No `// arrange / act / assert` markers | Hard to read, easy to mix concerns | Always use the markers |
| Shared mutable state between tests | Flaky, order-dependent failures | Each test sets up its own state |
| `waitFor` with side effects | Race conditions, unpredictable | `waitFor` only wraps assertions |
| `sleep()` / fixed timeouts | Slow, flaky | Use `findBy*` queries or `waitFor` |

---

## Quick Reference: Test Cheat Sheet

```tsx
// Rendering
render(<MyComponent prop="value" />);              // with providers
render(<MyComponent />, { wrapper: CustomWrapper }); // custom wrapper

// Queries (by priority)
screen.getByRole('button', { name: 'Submit' });     // 1st choice
screen.getByLabelText('Email');                       // 2nd choice
screen.getByText('Welcome');                          // 3rd choice
screen.getByTestId('custom-element');                 // last resort

// Async queries
await screen.findByText('Loaded');                    // waits for element

// Absence
expect(screen.queryByText('Error')).not.toBeInTheDocument();

// User interaction
const user = userEvent.setup();
await user.click(screen.getByRole('button'));
await user.type(screen.getByRole('textbox'), 'hello');

// Hook testing
const { result } = renderHook(() => useMyHook());
act(() => { result.current.doSomething(); });
expect(result.current.value).toBe('expected');

// MSW override for one test
server.use(
  http.get('/api/v1/foo', () => HttpResponse.json(null, { status: 500 })),
);

// Vitest matchers
expect(element).toBeInTheDocument();
expect(element).toBeVisible();
expect(element).toBeDisabled();
expect(element).toHaveTextContent('hello');
expect(fn).toHaveBeenCalledWith('arg');
expect(fn).toHaveBeenCalledTimes(1);
```

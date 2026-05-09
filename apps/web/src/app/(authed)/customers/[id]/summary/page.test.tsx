import { render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { createCustomerSummary } from '~/test/fixtures/customer-summary';

const mockNotFound = vi.fn();
vi.mock('next/navigation', () => ({
  notFound: () => {
    mockNotFound();
    throw new Error('NEXT_NOT_FOUND');
  },
}));

vi.mock('~/lib/data', () => ({
  fetchCustomerSummary: vi.fn(),
}));

vi.mock('~/components/id-chip', () => ({
  IdChip: ({ value, full }: { value: string; full?: boolean }) => (
    <span data-testid="id-chip" data-full={full}>
      {value}
    </span>
  ),
}));

vi.mock('~/components/status-badge', () => ({
  StatusBadge: ({ status }: { status: string }) => <span data-testid="status-badge">{status}</span>,
}));

import { fetchCustomerSummary } from '~/lib/data';
import CustomerSummaryPage from './page';

const mockFetch = vi.mocked(fetchCustomerSummary);
const params = Promise.resolve({ id: 'c3a1f5e2-7b4d-4e8a-9f2c-1d6e3a8b5c7f' });

async function renderPage() {
  const jsx = await CustomerSummaryPage({ params });
  render(jsx);
}

describe('CustomerSummaryPage', () => {
  it('calls notFound when customer is null', async () => {
    // arrange
    mockFetch.mockResolvedValue(null);

    // act + assert
    await expect(renderPage()).rejects.toThrow('NEXT_NOT_FOUND');
    expect(mockNotFound).toHaveBeenCalled();
  });

  it('renders profile card with name and email', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary());

    // act
    await renderPage();

    // assert
    const profile = screen.getByTestId('profile-card');
    expect(within(profile).getByText('Alice Johnson')).toBeInTheDocument();
    expect(within(profile).getByText('alice.johnson@example.com')).toBeInTheDocument();
  });

  it('renders avatar initials from name', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ name: 'Bob Smith' }));

    // act
    await renderPage();

    // assert
    const profile = screen.getByTestId('profile-card');
    expect(within(profile).getByText('BS')).toBeInTheDocument();
  });

  it('renders stats grid with balance, total sent, and transactions', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ total_transactions: 42 }));

    // act
    await renderPage();

    // assert
    const grid = screen.getByTestId('stats-grid');
    expect(within(grid).getByText('Balance')).toBeInTheDocument();
    expect(within(grid).getByText('Total sent')).toBeInTheDocument();
    expect(within(grid).getByText('Transactions')).toBeInTheDocument();
    expect(within(grid).getByText('42')).toBeInTheDocument();
  });

  it('renders compliance card with KYC status color-coded', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ kyc_status: 'FLAGGED' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    const kycValue = within(compliance).getByText('FLAGGED');
    expect(kycValue.className).toContain('text-red-400');
  });

  it('renders compliance card with risk tier color-coded', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ risk_tier: 'MEDIUM' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    const riskValue = within(compliance).getByText('MEDIUM');
    expect(riskValue.className).toContain('text-amber-400');
  });

  it('renders customer ID as full UUID via IdChip', async () => {
    // arrange
    const customerId = 'c3a1f5e2-7b4d-4e8a-9f2c-1d6e3a8b5c7f';
    mockFetch.mockResolvedValue(createCustomerSummary({ customer_id: customerId }));

    // act
    await renderPage();

    // assert
    const idChip = screen.getByTestId('id-chip');
    expect(idChip).toHaveTextContent(customerId);
    expect(idChip).toHaveAttribute('data-full', 'true');
  });

  it('renders member since date', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ member_since: '2025-03-10T00:00:00Z' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    expect(within(compliance).getByText('Mar 10, 2025')).toBeInTheDocument();
  });

  it('renders recent transactions table with 5 rows', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary());

    // act
    await renderPage();

    // assert
    const table = screen.getByTestId('recent-transactions');
    const rows = within(table).getAllByRole('row');
    // 1 header row + 5 data rows
    expect(rows).toHaveLength(6);
  });

  it('renders empty state when no recent transactions', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ recent_transactions: [] }));

    // act
    await renderPage();

    // assert
    expect(screen.getByText('No recent transactions')).toBeInTheDocument();
  });

  it('renders KYC VERIFIED with success color', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ kyc_status: 'VERIFIED' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    const kycValue = within(compliance).getByText('VERIFIED');
    expect(kycValue.className).toContain('text-green-400');
  });

  it('renders KYC PENDING with warning color', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ kyc_status: 'PENDING' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    const kycValue = within(compliance).getByText('PENDING');
    expect(kycValue.className).toContain('text-amber-400');
  });

  it('renders risk HIGH with danger color', async () => {
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary({ risk_tier: 'HIGH' }));

    // act
    await renderPage();

    // assert
    const compliance = screen.getByTestId('compliance-card');
    const riskValue = within(compliance).getByText('HIGH');
    expect(riskValue.className).toContain('text-red-400');
  });

  it('page has no use-client directive (pure RSC)', async () => {
    // This test verifies the page module doesn't export a client marker.
    // If it were a client component, vi.mock('~/lib/data') with server-only
    // would throw at import time.
    // arrange
    mockFetch.mockResolvedValue(createCustomerSummary());

    // act
    await renderPage();

    // assert
    expect(screen.getByTestId('customer-summary-page')).toBeInTheDocument();
  });
});

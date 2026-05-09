import { notFound } from 'next/navigation';
import { Amount } from '~/components/amount';
import { IdChip } from '~/components/id-chip';
import { StatusBadge } from '~/components/status-badge';
import { Avatar, AvatarFallback } from '~/components/ui/avatar';
import { fetchCustomerSummary } from '~/lib/data';
import { cn } from '~/lib/utils';
import type { CustomerSummaryDto } from '~/types/api';

type KycStatus = CustomerSummaryDto['kyc_status'];
type RiskTier = CustomerSummaryDto['risk_tier'];
type StatusColor = 'success' | 'warning' | 'danger';

const kycColor: Record<KycStatus, StatusColor> = {
  VERIFIED: 'success',
  PENDING: 'warning',
  FLAGGED: 'danger',
};

const riskColor: Record<RiskTier, StatusColor> = {
  LOW: 'success',
  MEDIUM: 'warning',
  HIGH: 'danger',
};

const colorClasses: Record<StatusColor, string> = {
  success: 'text-green-400',
  warning: 'text-amber-400',
  danger: 'text-red-400',
};

function getInitials(name: string): string {
  return name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');
}

export default async function CustomerSummaryPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const customer = await fetchCustomerSummary(id);
  if (!customer) notFound();

  return (
    <div className="page" data-testid="customer-summary-page">
      {/* Profile card */}
      <div
        data-testid="profile-card"
        className="relative overflow-hidden rounded-card border border-border-1 bg-surface-2 p-6"
      >
        <div className="pointer-events-none absolute inset-0 bg-gradient-to-br from-solana-purple/10 via-transparent to-solana-magenta/8" />
        <div className="relative flex items-center gap-4">
          <Avatar size="lg">
            <AvatarFallback className="bg-surface-3 text-[14px] font-semibold text-fg-2">
              {getInitials(customer.name)}
            </AvatarFallback>
          </Avatar>
          <div>
            <h1 className="text-[20px] font-bold leading-tight tracking-tight text-fg-1">
              {customer.name}
            </h1>
            <p className="mt-0.5 text-[13px] text-fg-3">{customer.email}</p>
          </div>
        </div>
      </div>

      {/* Stats grid */}
      <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3" data-testid="stats-grid">
        <div className="rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]">
          <span className="sp-eyebrow text-[10px]">Balance</span>
          <div className="mt-[14px] text-[28px] font-bold leading-none tracking-[-0.025em] text-fg-1">
            <Amount
              value={customer.balance.amount}
              currency={customer.balance.currency}
              size="xl"
            />
          </div>
        </div>
        <div className="rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]">
          <span className="sp-eyebrow text-[10px]">Total sent</span>
          <div className="mt-[14px] text-[28px] font-bold leading-none tracking-[-0.025em] text-fg-1">
            <Amount
              value={customer.total_sent.amount}
              currency={customer.total_sent.currency}
              size="xl"
            />
          </div>
        </div>
        <div className="rounded-card border border-border-1 bg-surface-2 px-5 py-[18px]">
          <span className="sp-eyebrow text-[10px]">Transactions</span>
          <div className="mt-[14px] text-[28px] font-bold leading-none tracking-[-0.025em] text-fg-1">
            {customer.total_transactions.toLocaleString('en-US')}
          </div>
        </div>
      </div>

      {/* Compliance card */}
      <div
        data-testid="compliance-card"
        className="mt-5 rounded-card border border-border-1 bg-surface-2 p-5"
      >
        <h2 className="sp-eyebrow mb-4 text-[10px]">Compliance</h2>
        <div className="grid grid-cols-2 gap-x-8 gap-y-4 sm:grid-cols-4">
          <div>
            <span className="text-[11px] text-fg-3">KYC status</span>
            <div
              className={cn(
                'mt-1 text-[13px] font-semibold',
                colorClasses[kycColor[customer.kyc_status]],
              )}
            >
              {customer.kyc_status}
            </div>
          </div>
          <div>
            <span className="text-[11px] text-fg-3">Risk tier</span>
            <div
              className={cn(
                'mt-1 text-[13px] font-semibold',
                colorClasses[riskColor[customer.risk_tier]],
              )}
            >
              {customer.risk_tier}
            </div>
          </div>
          <div>
            <span className="text-[11px] text-fg-3">Customer ID</span>
            <div className="mt-1">
              <IdChip value={customer.customer_id} full />
            </div>
          </div>
          <div>
            <span className="text-[11px] text-fg-3">Member since</span>
            <div className="mt-1 text-[13px] text-fg-1">
              {new Date(customer.member_since).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Recent transactions table */}
      <div
        data-testid="recent-transactions"
        className="mt-5 overflow-hidden rounded-card border border-border-1 bg-surface-2"
      >
        <div className="px-5 pt-4 pb-2">
          <h2 className="sp-eyebrow text-[10px]">Recent transactions</h2>
        </div>
        <table className="w-full border-collapse" style={{ tableLayout: 'fixed' }}>
          <thead>
            <tr className="border-b border-border-1">
              <th className="px-5 py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
                Ref
              </th>
              <th className="px-5 py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
                Direction
              </th>
              <th className="px-5 py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
                Type
              </th>
              <th className="px-5 py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
                Amount
              </th>
              <th className="px-5 py-[9px] text-left text-[10px] font-semibold uppercase tracking-[0.09em] text-fg-3">
                Status
              </th>
            </tr>
          </thead>
          <tbody>
            {customer.recent_transactions.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-5 py-8 text-center text-[13px] text-fg-3">
                  No recent transactions
                </td>
              </tr>
            ) : (
              customer.recent_transactions.map((txn) => (
                <tr
                  key={txn.ref}
                  className="border-b border-[rgba(255,255,255,0.04)] transition-colors duration-100"
                >
                  <td className="px-5 py-[11px] font-mono text-[12px] text-fg-2">{txn.ref}</td>
                  <td className="px-5 py-[11px] text-[12px] text-fg-2">{txn.direction}</td>
                  <td className="px-5 py-[11px] text-[12px] text-fg-2">{txn.type}</td>
                  <td className="px-5 py-[11px] text-[12px]">
                    <Amount value={txn.amount.amount} currency={txn.amount.currency} size="sm" />
                  </td>
                  <td className="px-5 py-[11px]">
                    <StatusBadge status={txn.customer_status} />
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

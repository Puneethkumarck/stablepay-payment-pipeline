'use client';

import type { LucideIcon } from 'lucide-react';
import {
  Activity,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Clock,
  CreditCard,
  Home,
  Users,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { UserDropdown } from '~/components/layout/user-dropdown';
import { cn } from '~/lib/utils';

const STORAGE_KEY = 'sp4_sidebar_collapsed';

interface NavItem {
  key: string;
  label: string;
  icon: LucideIcon;
  href: string;
}

const customerLinks: NavItem[] = [
  { key: 'dashboard', label: 'Dashboard', icon: Home, href: '/' },
  { key: 'transactions', label: 'Transactions', icon: CreditCard, href: '/transactions' },
  { key: 'flows', label: 'Flows', icon: Activity, href: '/flows' },
  { key: 'customers', label: 'Customers', icon: Users, href: '/customers' },
];

const adminLinks: NavItem[] = [
  { key: 'dlq', label: 'DLQ Inspector', icon: AlertTriangle, href: '/admin/dlq' },
  { key: 'stuck', label: 'Stuck', icon: Clock, href: '/admin/stuck' },
];

interface SidebarProps {
  activePage: string;
  onNavigate: (key: string, href: string) => void;
  email: string;
  role: string;
  customerId?: string;
  onSignOut: () => void;
}

function SidebarNavItem({
  item,
  active,
  collapsed,
  onNavigate,
}: {
  item: NavItem;
  active: boolean;
  collapsed: boolean;
  onNavigate: (key: string, href: string) => void;
}) {
  return (
    <button
      type="button"
      onClick={() => onNavigate(item.key, item.href)}
      data-testid={`nav-${item.key}`}
      className={cn(
        'relative mx-[6px] my-[1px] flex items-center rounded-[9px] transition-all duration-[120ms]',
        collapsed ? 'justify-center py-[9px]' : 'gap-[9px] px-[11px] py-2',
        active
          ? 'bg-[rgba(153,69,255,0.13)] text-[#C4B5FD]'
          : 'text-[rgba(255,255,255,0.42)] hover:bg-[rgba(255,255,255,0.04)] hover:text-[rgba(255,255,255,0.80)]',
      )}
    >
      {active && !collapsed && (
        <span className="absolute left-0 top-[18%] bottom-[18%] w-[2.5px] rounded-r-sm bg-gradient-to-b from-solana-purple to-solana-magenta" />
      )}
      <item.icon size={15} />
      {!collapsed && <span className="text-[13px] font-medium leading-none">{item.label}</span>}
    </button>
  );
}

export function Sidebar({
  activePage,
  onNavigate,
  email,
  role,
  customerId,
  onSignOut,
}: SidebarProps) {
  const [collapsed, setCollapsed] = useState(() => {
    if (typeof window === 'undefined') return false;
    return localStorage.getItem(STORAGE_KEY) === 'true';
  });

  const toggle = useCallback(() => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(STORAGE_KEY, String(next));
      return next;
    });
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === 'b') {
        e.preventDefault();
        toggle();
      }
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [toggle]);

  return (
    <nav
      data-testid="sidebar"
      data-collapsed={collapsed}
      aria-label="Primary navigation"
      className={cn(
        'sticky top-0 flex h-screen shrink-0 flex-col overflow-hidden border-r border-border-1 bg-sidebar transition-[width] duration-200',
        collapsed ? 'w-14' : 'w-[212px]',
      )}
      style={{ transitionTimingFunction: 'var(--ease-out)' }}
    >
      {/* Logo */}
      <div
        className={cn(
          'flex min-h-14 shrink-0 items-center border-b border-[rgba(255,255,255,0.05)]',
          collapsed ? 'justify-center px-0 py-4' : 'gap-[9px] px-[14px] py-4',
        )}
      >
        <div className="grid size-[26px] shrink-0 place-items-center rounded-[7px] bg-gradient-to-br from-solana-teal via-solana-purple to-solana-magenta">
          <span className="text-[13px] font-bold tracking-[-0.05em] text-surface-0">S</span>
        </div>
        {!collapsed && (
          <span className="text-[14px] font-bold tracking-tight text-fg-1 whitespace-nowrap">
            stable<span className="sp-gradient-text">pay</span>
          </span>
        )}
      </div>

      {/* Nav links */}
      <div className="flex-1 overflow-y-auto py-2">
        {!collapsed && (
          <div className="sp-eyebrow px-[17px] pt-[10px] pb-1 text-[10px] text-[rgba(255,255,255,0.20)]">
            Customer
          </div>
        )}
        {collapsed && <div className="h-[18px]" />}
        {customerLinks.map((item) => (
          <SidebarNavItem
            key={item.key}
            item={item}
            active={activePage === item.key || (item.key === 'dashboard' && activePage === 'home')}
            collapsed={collapsed}
            onNavigate={onNavigate}
          />
        ))}
        {!collapsed && (
          <div className="sp-eyebrow px-[17px] pt-4 pb-1 text-[10px] text-[rgba(255,255,255,0.20)]">
            Admin
          </div>
        )}
        {collapsed && <div className="h-[10px]" />}
        {adminLinks.map((item) => (
          <SidebarNavItem
            key={item.key}
            item={item}
            active={activePage === item.key}
            collapsed={collapsed}
            onNavigate={onNavigate}
          />
        ))}
      </div>

      {/* Footer */}
      <div
        className={cn(
          'shrink-0 border-t border-[rgba(255,255,255,0.05)]',
          collapsed ? 'py-[10px]' : 'px-[6px] py-[10px]',
        )}
      >
        {!collapsed && (
          <UserDropdown email={email} role={role} customerId={customerId} onSignOut={onSignOut} />
        )}
        <button
          type="button"
          onClick={toggle}
          data-testid="sidebar-toggle"
          className="mx-[6px] flex w-[calc(100%-12px)] cursor-pointer justify-center rounded-[7px] p-[5px] text-[rgba(255,255,255,0.25)] transition-colors duration-[120ms] hover:text-[rgba(255,255,255,0.55)]"
        >
          {collapsed ? <ChevronRight size={13} /> : <ChevronLeft size={13} />}
        </button>
      </div>
    </nav>
  );
}

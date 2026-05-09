'use client';

import { LogOut, User } from 'lucide-react';
import Link from 'next/link';
import { ThemeSwitch } from '~/components/theme-switch';
import { Avatar, AvatarFallback } from '~/components/ui/avatar';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '~/components/ui/dropdown-menu';

interface UserDropdownProps {
  email: string;
  role: string;
  customerId?: string;
  onSignOut: () => void;
}

function getInitial(email: string): string {
  return email.charAt(0).toUpperCase();
}

export function UserDropdown({ email, role, customerId, onSignOut }: UserDropdownProps) {
  return (
    <DropdownMenu>
      <DropdownMenuTrigger
        data-testid="user-dropdown-trigger"
        className="flex w-full cursor-pointer items-center gap-2 rounded-md px-[10px] py-[6px] transition-colors duration-[120ms] hover:bg-[rgba(255,255,255,0.04)]"
      >
        <Avatar size="sm">
          <AvatarFallback className="bg-gradient-to-br from-solana-purple to-solana-magenta text-[10px] font-bold text-white">
            {getInitial(email)}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 text-left">
          <div className="truncate text-[11px] font-semibold leading-snug text-fg-1">{email}</div>
          <div className="text-[10px] text-fg-3">{role}</div>
        </div>
      </DropdownMenuTrigger>
      <DropdownMenuContent side="top" sideOffset={8} align="start" className="w-56">
        <DropdownMenuLabel className="text-xs text-fg-3">{email}</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <div className="px-1.5 py-1.5">
          <ThemeSwitch />
        </div>
        <DropdownMenuSeparator />
        {customerId && (
          <DropdownMenuItem
            render={
              <Link
                href={{ pathname: `/customers/${customerId}/summary` }}
                data-testid="user-dropdown-summary"
              />
            }
          >
            <User size={14} />
            View my customer summary
          </DropdownMenuItem>
        )}
        <DropdownMenuItem onClick={onSignOut} data-testid="user-dropdown-signout">
          <LogOut size={14} />
          Sign out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

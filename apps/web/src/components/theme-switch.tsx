'use client';

import { Moon, Sun } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Switch } from '~/components/ui/switch';

const STORAGE_KEY = 'sp4_theme';

function getInitialTheme(): 'dark' | 'light' {
  if (typeof window === 'undefined') return 'dark';

  const stored = localStorage.getItem(STORAGE_KEY);
  if (stored === 'light' || stored === 'dark') return stored;

  return window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
}

export function ThemeSwitch() {
  const [theme, setTheme] = useState<'dark' | 'light'>('dark');
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setTheme(getInitialTheme());
    setMounted(true);
  }, []);

  useEffect(() => {
    if (!mounted) return;
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem(STORAGE_KEY, theme);
  }, [theme, mounted]);

  const toggle = () => {
    setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  if (!mounted) return null;

  return (
    <div data-testid="theme-switch" className="flex items-center gap-2">
      <Sun size={14} className="text-fg-3" />
      <Switch
        size="sm"
        checked={theme === 'dark'}
        onCheckedChange={toggle}
        aria-label="Toggle theme"
      />
      <Moon size={14} className="text-fg-3" />
    </div>
  );
}

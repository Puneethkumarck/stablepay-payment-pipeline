'use client';

import { Search } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '~/lib/utils';

interface SearchBarProps {
  placeholder?: string;
  value?: string;
  onChange: (value: string) => void;
  debounceMs?: number;
  className?: string;
}

export function SearchBar({
  placeholder = 'Search…',
  value: controlledValue,
  onChange,
  debounceMs = 300,
  className,
}: SearchBarProps) {
  const [localValue, setLocalValue] = useState(controlledValue ?? '');
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    if (controlledValue !== undefined) {
      setLocalValue(controlledValue);
    }
  }, [controlledValue]);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const next = e.target.value;
      setLocalValue(next);

      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => {
        onChange(next);
      }, debounceMs);
    },
    [onChange, debounceMs],
  );

  useEffect(() => {
    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, []);

  return (
    <div data-testid="search-bar" className={cn('relative', className)}>
      <div className="pointer-events-none absolute left-[10px] top-1/2 -translate-y-1/2 text-fg-3">
        <Search size={13} />
      </div>
      <input
        type="text"
        value={localValue}
        onChange={handleChange}
        placeholder={placeholder}
        className="w-full rounded-[9px] border border-border-1 bg-[rgba(255,255,255,0.05)] py-[7px] pr-3 pl-8 font-sans text-[13px] text-fg-1 outline-none placeholder:text-fg-3 focus:border-ring"
      />
    </div>
  );
}

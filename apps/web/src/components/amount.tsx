import { formatMoney, type MoneyPrecision } from '~/lib/format/money';
import { cn } from '~/lib/utils';

type AmountSize = 'sm' | 'base' | 'lg' | 'xl';

const sizeClasses: Record<AmountSize, string> = {
  sm: 'text-[12px]',
  base: 'text-[14px]',
  lg: 'text-[20px]',
  xl: 'text-[32px]',
};

interface AmountProps {
  value: number;
  currency?: string;
  size?: AmountSize;
  precision?: MoneyPrecision;
  className?: string;
}

export function Amount({
  value,
  currency = 'USDC',
  size = 'base',
  precision = 'default',
  className,
}: AmountProps) {
  return (
    <span data-testid="amount" className={cn('sp-amount', sizeClasses[size], className)}>
      {formatMoney(value, currency, precision)}
    </span>
  );
}

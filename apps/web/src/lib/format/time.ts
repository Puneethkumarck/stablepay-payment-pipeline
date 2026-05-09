const SECOND = 1_000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

const relativeFormatter = new Intl.RelativeTimeFormat('en', { numeric: 'always' });
const absoluteFormatter = new Intl.DateTimeFormat('en', { dateStyle: 'medium' });
const utcFormatter = new Intl.DateTimeFormat('en', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false,
  timeZone: 'UTC',
});
const localFormatter = new Intl.DateTimeFormat('en', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZoneName: 'short',
});

export function formatRelativeTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso);
  const diffMs = now.getTime() - date.getTime();

  if (diffMs < MINUTE) return 'just now';
  if (diffMs < HOUR) return relativeFormatter.format(-Math.floor(diffMs / MINUTE), 'minute');
  if (diffMs < DAY) return relativeFormatter.format(-Math.floor(diffMs / HOUR), 'hour');
  if (diffMs < 7 * DAY) return relativeFormatter.format(-Math.floor(diffMs / DAY), 'day');

  return absoluteFormatter.format(date);
}

export function formatDuration(ms: number): string {
  if (ms < MINUTE) return `${Math.floor(ms / SECOND)}s`;
  if (ms < HOUR) return `${Math.floor(ms / MINUTE)}m`;
  return `${Math.floor(ms / HOUR)}h`;
}

export function formatAbsoluteTooltip(iso: string): string {
  const date = new Date(iso);
  const parts = utcFormatter.formatToParts(date);
  const get = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((p) => p.type === type)?.value ?? '';

  const utcStr = `${get('year')}-${get('month')}-${get('day')} ${get('hour')}:${get('minute')}:${get('second')} UTC`;
  const localStr = localFormatter.format(date);

  return `${utcStr} (${localStr})`;
}

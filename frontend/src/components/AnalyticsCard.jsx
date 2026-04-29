import { CalendarDays, Clock3, MousePointer2, TrendingUp } from 'lucide-react';

const RELATIVE_UNITS = [
  ['year', 365 * 24 * 60 * 60 * 1000],
  ['month', 30 * 24 * 60 * 60 * 1000],
  ['day', 24 * 60 * 60 * 1000],
  ['hour', 60 * 60 * 1000],
  ['minute', 60 * 1000],
];

function parseApiDate(value) {
  if (!value) {
    return null;
  }

  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value;
  }

  if (typeof value !== 'string') {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  const trimmedValue = value.trim();
  const hasTimeZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(trimmedValue);
  const normalizedValue = hasTimeZone ? trimmedValue : `${trimmedValue}Z`;
  const date = new Date(normalizedValue);

  return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateTime(value) {
  const date = parseApiDate(value);

  if (!date) {
    return 'Not available';
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
    timeZoneName: 'short',
  }).format(date);
}

function relativeTime(value, { emptyLabel = 'Not available', allowFuture = false } = {}) {
  const date = parseApiDate(value);

  if (!date) {
    return emptyLabel;
  }

  const diffMs = Date.now() - date.getTime();

  if (Math.abs(diffMs) < 60 * 1000) {
    return 'just now';
  }

  if (diffMs < 0 && !allowFuture) {
    return 'just now';
  }

  const relativeFormatter = new Intl.RelativeTimeFormat(undefined, {
    numeric: 'auto',
    style: 'short',
  });

  const absoluteDiffMs = Math.abs(diffMs);

  for (const [unit, unitMs] of RELATIVE_UNITS) {
    if (absoluteDiffMs >= unitMs) {
      const amount = Math.round(absoluteDiffMs / unitMs);
      return relativeFormatter.format(diffMs < 0 ? amount : -amount, unit);
    }
  }

  return 'just now';
}

function Metric({ icon: Icon, label, value, detail, pill }) {
  return (
    <div className="flex min-w-0 items-center gap-4 border-slate-200 py-1 lg:border-r lg:pr-7 last:lg:border-r-0">
      <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-indigo-50 text-indigo-600">
        <Icon className="h-7 w-7" aria-hidden="true" />
      </span>
      <div className="min-w-0">
        <p className="text-sm font-medium text-slate-600">{label}</p>
        <div className="mt-1 flex flex-wrap items-center gap-2">
          <p className="text-2xl font-black text-slate-950">{value}</p>
          {pill && (
            <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-bold text-emerald-700">
              {pill}
            </span>
          )}
        </div>
        <p className="mt-2 text-sm leading-snug text-slate-500">{detail}</p>
      </div>
    </div>
  );
}

function AnalyticsCard({ result, stats, isLoading, statusMessage }) {
  const clickCount = stats?.analytics?.totalClicks ?? stats?.clickCount ?? 0;
  // Backend responses expose ISO LocalDateTime strings as createdAt, expiresAt,
  // lastAccessedAt, and analytics.lastAccessedAt.
  const createdAt = stats?.createdAt ?? result?.createdAt;
  const lastAccessedAt = stats?.analytics?.lastAccessedAt ?? stats?.lastAccessedAt;
  const expiresAt = stats?.expiresAt ?? result?.expiresAt;

  return (
    <section
      id="analytics"
      className="animate-fade-up rounded-2xl border border-slate-200 bg-white/92 p-5 shadow-soft backdrop-blur sm:p-7"
    >
      <div className="grid gap-6 lg:grid-cols-4">
        <Metric
          icon={MousePointer2}
          label="Total Clicks"
          value={isLoading ? '...' : clickCount}
          detail={result ? 'All time' : 'Create a link to begin'}
          pill={clickCount > 0 ? '+ live' : undefined}
        />
        <Metric
          icon={CalendarDays}
          label="Created"
          value={createdAt ? relativeTime(createdAt) : 'Pending'}
          detail={formatDateTime(createdAt)}
        />
        <Metric
          icon={TrendingUp}
          label="Last Accessed"
          value={relativeTime(lastAccessedAt, { emptyLabel: 'Never' })}
          detail={formatDateTime(lastAccessedAt)}
        />
        <Metric
          icon={Clock3}
          label="Expires"
          value={relativeTime(expiresAt, { emptyLabel: 'Never', allowFuture: true })}
          detail={expiresAt ? formatDateTime(expiresAt) : 'This link never expires'}
        />
      </div>
      {statusMessage && (
        <p className="mt-5 rounded-xl bg-amber-50 px-4 py-3 text-sm font-semibold text-amber-700">
          {statusMessage}
        </p>
      )}
    </section>
  );
}

export default AnalyticsCard;

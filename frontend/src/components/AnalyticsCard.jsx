import { CalendarDays, Clock3, MousePointer2, TrendingUp } from 'lucide-react';

function formatDateTime(value) {
  if (!value) {
    return 'Not available';
  }

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value));
}

function relativeTime(value) {
  if (!value) {
    return 'Never';
  }

  const date = new Date(value);
  const seconds = Math.round((Date.now() - date.getTime()) / 1000);

  if (Math.abs(seconds) < 60) {
    return 'just now';
  }

  const minutes = Math.round(seconds / 60);
  if (Math.abs(minutes) < 60) {
    return `${minutes} min ago`;
  }

  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) {
    return `${hours} hr ago`;
  }

  const days = Math.round(hours / 24);
  return `${days} day${Math.abs(days) === 1 ? '' : 's'} ago`;
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
        <p className="mt-2 truncate text-sm text-slate-500">{detail}</p>
      </div>
    </div>
  );
}

function AnalyticsCard({ result, stats, isLoading, statusMessage }) {
  const clickCount = stats?.analytics?.totalClicks ?? stats?.clickCount ?? 0;
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
          value={lastAccessedAt ? relativeTime(lastAccessedAt) : 'Never'}
          detail={formatDateTime(lastAccessedAt)}
        />
        <Metric
          icon={Clock3}
          label="Expires"
          value={expiresAt ? relativeTime(expiresAt) : 'Never'}
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

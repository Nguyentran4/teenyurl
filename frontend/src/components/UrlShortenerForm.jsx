import { Link2, ShieldCheck, Zap } from 'lucide-react';
import { useState } from 'react';

function UrlShortenerForm({ error, isLoading, onSubmit }) {
  const [originalUrl, setOriginalUrl] = useState('');
  const [alias, setAlias] = useState('');
  const [touched, setTouched] = useState(false);

  const isInvalid = touched && originalUrl.trim().length === 0;

  function handleSubmit(event) {
    event.preventDefault();
    setTouched(true);

    if (!originalUrl.trim()) {
      return;
    }

    onSubmit({
      originalUrl: originalUrl.trim(),
      alias: alias.trim() || undefined,
    });
  }

  return (
    <form
      id="home"
      onSubmit={handleSubmit}
      aria-busy={isLoading}
      className="animate-fade-up rounded-2xl border border-slate-200 bg-white/92 p-5 shadow-soft backdrop-blur sm:p-7"
    >
      <div className="grid gap-5 lg:grid-cols-[1fr_0.52fr_auto] lg:items-end">
        <label className="grid gap-2 text-sm font-semibold text-slate-950">
          Paste your long URL
          <span className="relative">
            <Link2 className="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
            <input
              type="url"
              value={originalUrl}
              onBlur={() => setTouched(true)}
              onChange={(event) => setOriginalUrl(event.target.value)}
              placeholder="https://example.com/very-long-url"
              aria-invalid={isInvalid}
              className={`h-16 w-full rounded-xl border bg-white pl-14 pr-4 text-base text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100 ${
                isInvalid ? 'border-rose-300 ring-4 ring-rose-50' : 'border-slate-200'
              }`}
            />
          </span>
        </label>

        <label className="grid gap-2 text-sm font-semibold text-slate-950">
          Custom alias <span className="font-normal text-slate-500">(optional)</span>
          <span className="relative">
            <Link2 className="pointer-events-none absolute left-5 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={alias}
              onChange={(event) => setAlias(event.target.value)}
              placeholder="my-custom-link"
              className="h-16 w-full rounded-xl border border-slate-200 bg-white pl-14 pr-4 text-base text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100"
            />
          </span>
        </label>

        <button
          type="submit"
          disabled={isLoading}
          className="inline-flex h-16 items-center justify-center gap-3 rounded-xl bg-indigo-600 px-7 text-base font-bold text-white shadow-lg shadow-indigo-500/25 transition hover:-translate-y-0.5 hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-70 disabled:hover:translate-y-0 lg:min-w-48"
        >
          <Zap className="h-5 w-5 fill-white" aria-hidden="true" />
          {isLoading ? 'Shortening...' : 'Shorten URL'}
        </button>
      </div>

      <div className="mt-5 flex flex-col gap-3 text-sm sm:flex-row sm:items-center sm:justify-between">
        <p className="flex items-start gap-2 text-slate-500">
          <ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-slate-500" aria-hidden="true" />
          <span>
            By shortening a URL, you agree to our{' '}
            <a className="font-semibold text-indigo-600 hover:text-indigo-700" href="#terms">
              Terms of Service
            </a>{' '}
            and{' '}
            <a className="font-semibold text-indigo-600 hover:text-indigo-700" href="#privacy">
              Privacy Policy
            </a>
            .
          </span>
        </p>
        {(isInvalid || error) && (
          <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm font-semibold text-rose-600">
            {error || 'Enter a valid URL to continue.'}
          </p>
        )}
      </div>
    </form>
  );
}

export default UrlShortenerForm;

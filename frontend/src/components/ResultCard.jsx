import { Check, Copy, ExternalLink, QrCode } from 'lucide-react';
import { useState } from 'react';

function ResultCard({ result }) {
  const [copied, setCopied] = useState(false);

  async function handleCopy() {
    await navigator.clipboard.writeText(result.shortUrl);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  }

  function handleOpen() {
    window.open(result.shortUrl, '_blank', 'noopener,noreferrer');
  }

  return (
    <section className="animate-fade-up rounded-2xl border border-emerald-200 bg-emerald-50/70 p-5 shadow-soft sm:p-7">
      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
        <div className="flex min-w-0 items-center gap-5">
          <span className="grid h-14 w-14 shrink-0 place-items-center rounded-full bg-emerald-500 text-white shadow-lg shadow-emerald-500/20">
            <Check className="h-8 w-8" aria-hidden="true" />
          </span>
          <div className="min-w-0">
            <p className="font-bold text-emerald-700">Your short URL is ready!</p>
            <a
              href={result.shortUrl}
              target="_blank"
              rel="noreferrer"
              className="mt-2 block truncate text-xl font-black text-indigo-600 transition hover:text-indigo-700 sm:text-2xl"
            >
              {result.shortUrl}
            </a>
          </div>
        </div>

        <div className="grid grid-cols-[1fr_1fr_auto] gap-3 sm:flex">
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex h-12 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 font-bold text-slate-950 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-200 hover:text-indigo-600"
          >
            <Copy className="h-5 w-5" aria-hidden="true" />
            {copied ? 'Copied' : 'Copy'}
          </button>
          <button
            type="button"
            onClick={handleOpen}
            className="inline-flex h-12 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-4 font-bold text-slate-950 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-200 hover:text-indigo-600"
          >
            <ExternalLink className="h-5 w-5" aria-hidden="true" />
            Open
          </button>
          <button
            type="button"
            className="grid h-12 w-12 place-items-center rounded-xl border border-slate-200 bg-white text-slate-700 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-200 hover:text-indigo-600"
            title="QR code"
            aria-label="QR code"
          >
            <QrCode className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>
      </div>
    </section>
  );
}

export default ResultCard;

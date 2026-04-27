import { useState } from 'react';
import AnalyticsCard from './components/AnalyticsCard.jsx';
import FeatureCards from './components/FeatureCards.jsx';
import Navbar from './components/Navbar.jsx';
import ResultCard from './components/ResultCard.jsx';
import UrlShortenerForm from './components/UrlShortenerForm.jsx';
import { createShortUrl, getUrlStats } from './services/api.js';

function App() {
  const [createdUrl, setCreatedUrl] = useState(null);
  const [stats, setStats] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isStatsLoading, setIsStatsLoading] = useState(false);
  const [error, setError] = useState('');
  const [statsError, setStatsError] = useState('');

  async function handleShorten(formValues) {
    setIsLoading(true);
    setError('');
    setStatsError('');
    setCreatedUrl(null);
    setStats(null);

    try {
      const response = await createShortUrl(formValues);
      setCreatedUrl(response);
      setIsStatsLoading(true);

      try {
        const statsResponse = await getUrlStats(response.shortCode);
        setStats(statsResponse);
      } catch {
        setStatsError('Stats are not available yet, but your short link is ready.');
      } finally {
        setIsStatsLoading(false);
      }
    } catch (requestError) {
      setError(requestError.message || 'Unable to create a short URL right now.');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <main className="min-h-screen overflow-hidden bg-slate-50 text-slate-950">
      <div className="pointer-events-none fixed inset-0 -z-10 bg-[radial-gradient(circle_at_18%_18%,rgba(99,102,241,0.14),transparent_28%),radial-gradient(circle_at_78%_8%,rgba(168,85,247,0.12),transparent_26%),linear-gradient(180deg,#ffffff_0%,#f8fafc_58%,#ffffff_100%)]" />
      <div className="pointer-events-none fixed right-4 top-36 -z-10 h-40 w-40 rounded-full border border-indigo-100 opacity-60 [background-image:radial-gradient(#c7d2fe_1px,transparent_1px)] [background-size:18px_18px] sm:right-16" />
      <Navbar />

      <section className="mx-auto flex w-full max-w-6xl flex-col items-center px-4 pb-10 pt-9 sm:px-6 lg:px-8 lg:pt-12">
        <div className="animate-fade-up text-center">
          <span className="inline-flex rounded-full border border-indigo-100 bg-indigo-50 px-4 py-1.5 text-sm font-semibold text-indigo-600 shadow-sm">
            Fast &middot; Simple &middot; Reliable
          </span>
          <h1 className="mx-auto mt-5 max-w-3xl text-4xl font-black leading-tight tracking-normal text-slate-950 sm:text-5xl lg:text-6xl">
            Shorten your links, share{' '}
            <span className="text-indigo-600">anywhere.</span>
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg">
            Create short links, track clicks, and share with confidence.
          </p>
        </div>

        <div className="mt-8 grid w-full gap-5">
          <UrlShortenerForm
            error={error}
            isLoading={isLoading}
            onSubmit={handleShorten}
          />
          {createdUrl && <ResultCard result={createdUrl} />}
          <AnalyticsCard
            result={createdUrl}
            stats={stats}
            isLoading={isStatsLoading}
            statusMessage={statsError}
          />
          <FeatureCards />
        </div>

        <footer className="mt-8 text-center text-sm text-slate-500">
          <p>Made with care by TeenyURL</p>
          <p className="mt-2">&copy; 2026 TeenyURL. All rights reserved.</p>
        </footer>
      </section>
    </main>
  );
}

export default App;

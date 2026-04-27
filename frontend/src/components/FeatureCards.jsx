import { BarChart3, ShieldCheck, Zap } from 'lucide-react';

const features = [
  {
    title: 'Secure & Reliable',
    description: 'Your links are protected with clean validation and production-minded uptime.',
    icon: ShieldCheck,
  },
  {
    title: 'Real-time Analytics',
    description: 'Track clicks, access timing, and practical engagement signals.',
    icon: BarChart3,
  },
  {
    title: 'Blazing Fast',
    description: 'Optimized for quick redirects with cache-friendly backend design.',
    icon: Zap,
  },
];

function FeatureCards() {
  return (
    <section id="my-links" className="animate-fade-up rounded-2xl border border-slate-200 bg-white/92 p-5 shadow-soft sm:p-6">
      <div className="grid gap-5 md:grid-cols-3">
        {features.map((feature, index) => (
          <article
            key={feature.title}
            className={`flex gap-4 md:border-slate-200 md:pr-5 ${
              index < features.length - 1 ? 'md:border-r' : ''
            }`}
          >
            <span className="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-indigo-50 text-indigo-600">
              <feature.icon className="h-8 w-8" aria-hidden="true" />
            </span>
            <div>
              <h2 className="font-black text-slate-950">{feature.title}</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600">{feature.description}</p>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

export default FeatureCards;

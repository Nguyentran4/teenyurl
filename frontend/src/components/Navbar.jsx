import { Github, Link2, Moon } from 'lucide-react';

const navItems = ['Home', 'My Links', 'Analytics', 'API'];

function Navbar() {
  return (
    <header className="sticky top-0 z-20 border-b border-slate-200/80 bg-white/85 backdrop-blur-xl">
      <nav className="mx-auto flex h-20 max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-8">
        <a href="/" className="flex items-center gap-3" aria-label="TeenyURL home">
          <span className="grid h-11 w-11 place-items-center rounded-xl bg-indigo-600 text-white shadow-lg shadow-indigo-500/25">
            <Link2 className="h-5 w-5" aria-hidden="true" />
          </span>
          <span className="text-2xl font-black tracking-normal">
            Teeny<span className="text-indigo-600">URL</span>
          </span>
        </a>

        <div className="hidden items-center gap-9 md:flex">
          {navItems.map((item) => (
            <a
              key={item}
              href={`#${item.toLowerCase().replace(' ', '-')}`}
              className={`relative text-sm font-bold transition hover:text-indigo-600 ${
                item === 'Home' ? 'text-indigo-600' : 'text-slate-950'
              }`}
            >
              {item}
              {item === 'Home' && (
                <span className="absolute -bottom-7 left-0 h-0.5 w-full rounded-full bg-indigo-600" />
              )}
            </a>
          ))}
        </div>

        <div className="flex items-center gap-2 sm:gap-4">
          <button
            type="button"
            className="grid h-10 w-10 place-items-center rounded-full text-slate-700 transition hover:bg-slate-100 hover:text-indigo-600"
            title="Dark mode"
            aria-label="Dark mode"
          >
            <Moon className="h-5 w-5" aria-hidden="true" />
          </button>
          <a
            href="https://github.com"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-bold text-white shadow-lg shadow-indigo-500/25 transition hover:-translate-y-0.5 hover:bg-indigo-700"
          >
            <Github className="h-4 w-4" aria-hidden="true" />
            <span className="hidden sm:inline">GitHub</span>
          </a>
        </div>
      </nav>
    </header>
  );
}

export default Navbar;

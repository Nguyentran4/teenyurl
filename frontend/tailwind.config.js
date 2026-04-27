/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      boxShadow: {
        glow: '0 24px 80px rgba(79, 70, 229, 0.16)',
        soft: '0 18px 60px rgba(15, 23, 42, 0.08)',
      },
      animation: {
        'fade-up': 'fadeUp 600ms ease-out both',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: '0', transform: 'translateY(14px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};

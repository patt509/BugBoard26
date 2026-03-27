import { Moon, Sun } from 'lucide-react';

function ThemeToggle({ isDarkMode, onToggle }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
      className="fixed right-4 top-4 z-[70] inline-flex items-center gap-2 rounded-full border border-gray-300 bg-white/90 px-3 py-2 text-sm font-medium text-gray-700 shadow-lg backdrop-blur transition-colors hover:bg-white"
    >
      {isDarkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
      <span>{isDarkMode ? 'Light' : 'Dark'}</span>
    </button>
  );
}

export default ThemeToggle;

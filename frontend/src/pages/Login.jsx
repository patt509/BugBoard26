import { useState } from 'react';
import { AlertCircle, Bug, Lock, Mail } from 'lucide-react';
import { authService } from '../services/auth.service';
import ThemeToggle from '../components/ThemeToggle';

function Login({ onLoginSuccess, isDarkMode, onToggleTheme }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const user = await authService.login({ email, password });
      localStorage.setItem('user', JSON.stringify(user));
      if (user?.id != null) {
        localStorage.setItem('userId', String(user.id));
      }
      onLoginSuccess(user);
    } catch (err) {
      setError(err.message || 'Connection error. Make sure the backend is running.');
      console.error('Login error:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <ThemeToggle
        isDarkMode={isDarkMode}
        onToggle={onToggleTheme}
        className="fixed right-4 top-4 z-[70]"
      />
      <div className="w-full max-w-md">
        <div className="mb-8 text-center">
          <div className="mb-4 flex items-center justify-center gap-3">
            <div className="rounded-xl bg-blue-600 p-3">
              <Bug className="h-10 w-10 text-white" />
            </div>
            <h1 className="text-4xl font-bold text-gray-900">BugBoard</h1>
          </div>
          <p className="text-gray-600">Sign in to your account</p>
        </div>

        <div className="rounded-2xl bg-white p-8 shadow-xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="email" className="mb-2 block text-sm font-medium text-gray-700">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 transform text-gray-400" />
                <input
                  type="email"
                  id="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 py-3 pl-11 pr-4 transition-all focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="admin@bugboard.com"
                  required
                  autoFocus
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="mb-2 block text-sm font-medium text-gray-700">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 transform text-gray-400" />
                <input
                  type="password"
                  id="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full rounded-lg border border-gray-300 py-3 pl-11 pr-4 transition-all focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                  placeholder="********"
                  required
                />
              </div>
            </div>

            {error && (
              <div className="flex items-center gap-2 rounded-lg border border-red-200 bg-red-50 p-4">
                <AlertCircle className="h-5 w-5 flex-shrink-0 text-red-600" />
                <p className="text-sm text-red-800">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-blue-600 py-3 font-medium text-white transition-all hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <div className="mt-6 rounded-lg border border-blue-100 bg-blue-50 p-4">
            <p className="mb-1 text-sm font-medium text-blue-900">Default credentials:</p>
            <p className="text-sm text-blue-700">
              Email: <span className="font-mono font-semibold">admin@bugboard.com</span>
            </p>
            <p className="text-sm text-blue-700">
              Password: <span className="font-mono font-semibold">admin</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;

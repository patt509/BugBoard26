import { useState } from 'react';
import { AlertCircle } from 'lucide-react';

function MandatoryUsernameModal({ onSubmit }) {
  const [username, setUsername] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const normalizedUsername = username.trim();

    if (normalizedUsername.length < 3) {
      setError('Username must be at least 3 characters.');
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await onSubmit(normalizedUsername);
    } catch (submitError) {
      setError(submitError.message || 'Unable to save username.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/70 px-4">
      <div className="w-full max-w-md rounded-2xl border border-gray-300 bg-white p-6 shadow-2xl">
        <h2 className="text-xl font-bold text-gray-900">Complete Your Profile</h2>
        <p className="mt-2 text-sm text-gray-600">
          You must choose a unique username before continuing.
        </p>

        <form onSubmit={handleSubmit} className="mt-5 space-y-4">
          <div>
            <label htmlFor="mandatory-username" className="mb-2 block text-sm font-medium text-gray-900">
              Username
            </label>
            <input
              id="mandatory-username"
              type="text"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoFocus
              required
              minLength={3}
              maxLength={30}
              className="w-full rounded-lg border border-gray-300 px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="e.g. luca.dev"
            />
          </div>

          {error && (
            <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3">
              <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-red-600" />
              <p className="text-sm text-red-800">{error}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-lg bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? 'Saving...' : 'Save Username'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default MandatoryUsernameModal;

import { useState } from 'react';
import { AlertCircle, CheckCircle } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { authService } from '../services/auth.service';

function CreateUserAccount({ user, onLogout, onNavigate, isDarkMode, onToggleTheme }) {
  const currentPage = 'create-user';
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    role: 'USER',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setSuccess(null);
    setLoading(true);

    try {
      const response = await authService.createUserByAdmin(
        {
          email: formData.email,
          password: formData.password,
          role: formData.role,
        },
        user?.id
      );

      setSuccess({
        message: response?.message || 'User created successfully.',
        email: response?.email || formData.email.trim(),
        temporaryPassword: response?.temporaryPassword || null,
      });
      setFormData((prev) => ({
        ...prev,
        email: '',
        password: '',
      }));
    } catch (err) {
      console.error('Error creating user:', err);
      setError(err.message || 'Failed to create user.');
    } finally {
      setLoading(false);
    }
  };

  if (user?.role !== 'ADMIN') {
    return (
      <div className="flex h-screen bg-gray-50">
        <Sidebar currentPage="issues" onNavigate={onNavigate} userRole={user?.role} />
        <main className="flex-1 flex items-center justify-center p-8">
          <div className="max-w-md rounded-xl border border-red-200 bg-red-50 p-6 text-center">
            <AlertCircle className="mx-auto h-10 w-10 text-red-600" />
            <h2 className="mt-3 text-lg font-semibold text-red-800">Access denied</h2>
            <p className="mt-1 text-sm text-red-700">
              Only administrators can create new users.
            </p>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        <header className="bg-white border-b border-gray-200 px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Create User</h1>
              <p className="text-sm text-gray-500">Create a new USER or ADMIN account</p>
            </div>
            <div className="flex items-center gap-3">
              <ThemeToggle isDarkMode={isDarkMode} onToggle={onToggleTheme} />
              <UserIdentity user={user} />
              <button
                onClick={onLogout}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        <div className="p-8">
          <div className="max-w-2xl rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label htmlFor="email" className="mb-2 block text-sm font-medium text-gray-900">
                  Email
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleInputChange}
                  placeholder="new.user@bugboard.com"
                  required
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              <div>
                <label htmlFor="password" className="mb-2 block text-sm font-medium text-gray-900">
                  Password
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  required
                  minLength={8}
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="mt-1 text-xs text-gray-500">Minimum 8 characters.</p>
              </div>

              <div>
                <label htmlFor="role" className="mb-2 block text-sm font-medium text-gray-900">
                  Role
                </label>
                <select
                  id="role"
                  name="role"
                  value={formData.role}
                  onChange={handleInputChange}
                  className="w-full rounded-lg border border-gray-300 bg-white px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="USER">Normal user</option>
                  <option value="ADMIN">Administrator</option>
                </select>
              </div>

              {error && (
                <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3">
                  <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-red-600" />
                  <p className="text-sm text-red-800">{error}</p>
                </div>
              )}

              {success && (
                <div className="rounded-lg border border-green-200 bg-green-50 p-3">
                  <div className="flex items-start gap-2">
                    <CheckCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-green-600" />
                    <div className="space-y-1">
                      <p className="text-sm font-medium text-green-800">{success.message}</p>
                      <p className="text-sm text-green-700">
                        Created account: <span className="font-medium">{success.email}</span>
                      </p>
                      {success.temporaryPassword && (
                        <p className="text-sm text-green-700">
                          Temporary password: <span className="font-mono font-semibold">{success.temporaryPassword}</span>
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              )}

              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={loading}
                  className="rounded-lg bg-blue-600 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {loading ? 'Creating...' : 'Create User'}
                </button>
              </div>
            </form>
          </div>
        </div>
      </main>
    </div>
  );
}

export default CreateUserAccount;

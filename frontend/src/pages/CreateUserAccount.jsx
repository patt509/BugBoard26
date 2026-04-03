import { useEffect, useRef, useState } from 'react';
import { AlertCircle, Check, ChevronDown, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { authService } from '../services/auth.service';

const ROLE_OPTIONS = [
  { value: 'USER', label: 'Normal user' },
  { value: 'ADMIN', label: 'Administrator' },
  { value: 'STAKEHOLDER', label: 'Stakeholder (Read-only)' },
];

function RoleDropdown({ value, onChange, disabled = false }) {
  const [open, setOpen] = useState(false);
  const [openUpward, setOpenUpward] = useState(false);
  const rootRef = useRef(null);
  const selectedOption = ROLE_OPTIONS.find((option) => option.value === value) || ROLE_OPTIONS[0];

  useEffect(() => {
    if (!open) {
      return undefined;
    }

    const handleOutsideClick = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, [open]);

  useEffect(() => {
    if (!open || !rootRef.current) {
      return;
    }

    const rect = rootRef.current.getBoundingClientRect();
    const spaceBelow = window.innerHeight - rect.bottom;
    const spaceAbove = rect.top;
    const estimatedMenuHeight = Math.min(240, ROLE_OPTIONS.length * 44 + 12);

    setOpenUpward(spaceBelow < estimatedMenuHeight && spaceAbove > spaceBelow);
  }, [open]);

  return (
    <div ref={rootRef} className="relative min-w-0">
      <button
        type="button"
        onClick={() => !disabled && setOpen((prev) => !prev)}
        disabled={disabled}
        className="inline-flex h-12 w-full items-center justify-between gap-2 rounded-lg border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
      >
        <span className="truncate text-left">{selectedOption.label}</span>
        <ChevronDown className={`h-4 w-4 text-gray-500 transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>

      {open && !disabled && (
        <div
          className={`absolute left-0 z-20 w-full rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl ${
            openUpward ? 'bottom-[calc(100%+8px)]' : 'top-[calc(100%+8px)]'
          }`}
        >
          <div className="max-h-60 overflow-y-auto">
            {ROLE_OPTIONS.map((option) => (
              <button
                key={option.value}
                type="button"
                onClick={() => {
                  onChange(option.value);
                  setOpen(false);
                }}
                className="flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-100"
              >
                <span>{option.label}</span>
                {value === option.value && <Check className="h-4 w-4 text-blue-600" />}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

function CreateUserAccount({ user, onLogout, onNavigate, isDarkMode, onToggleTheme }) {
  const currentPage = 'create-user';
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    role: 'USER',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [createdUserModal, setCreatedUserModal] = useState(null);

  const handleInputChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError(null);
    setCreatedUserModal(null);
    setLoading(true);

    const normalizedPassword = String(formData.password ?? '').trim();
    if (normalizedPassword.length > 0 && normalizedPassword.length < 8) {
      setError('Password must be at least 8 characters long when provided.');
      setLoading(false);
      return;
    }

    try {
      const response = await authService.createUserByAdmin(
        {
          email: formData.email,
          password: normalizedPassword.length > 0 ? normalizedPassword : undefined,
          role: formData.role,
        },
        user?.id
      );

      const generatedTemporaryPassword = response?.temporaryPassword || null;
      const effectivePassword = generatedTemporaryPassword || normalizedPassword || null;

      setCreatedUserModal({
        message: response?.message || 'User created successfully.',
        email: response?.email || formData.email.trim(),
        role: response?.role || formData.role,
        password: effectivePassword,
        isTemporaryPassword: Boolean(generatedTemporaryPassword),
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
        <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Create User</h1>
              <p className="text-sm text-gray-500">Create a new USER, ADMIN or STAKEHOLDER account</p>
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

        <div className="mx-auto w-full max-w-6xl p-8 pt-28">
          <div className="mx-auto w-full max-w-2xl rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
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
                  Password (Optional)
                </label>
                <input
                  id="password"
                  name="password"
                  type="password"
                  value={formData.password}
                  onChange={handleInputChange}
                  placeholder="Leave blank to auto-generate"
                  className="w-full rounded-lg border border-gray-300 px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <p className="mt-1 text-xs text-gray-500">If empty, a temporary password will be generated automatically.</p>
              </div>

              <div>
                <label htmlFor="role" className="mb-2 block text-sm font-medium text-gray-900">
                  Role
                </label>
                <RoleDropdown
                  value={formData.role}
                  onChange={(roleValue) => setFormData((prev) => ({ ...prev, role: roleValue }))}
                  disabled={loading}
                />
              </div>

              {error && (
                <div className="flex items-start gap-2 rounded-lg border border-red-200 bg-red-50 p-3">
                  <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-red-600" />
                  <p className="text-sm text-red-800">{error}</p>
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

        {createdUserModal && (
          <div className="fixed inset-0 z-[90] flex items-center justify-center bg-black/45 px-4 backdrop-blur-sm">
            <div className="w-full max-w-lg rounded-xl border border-gray-200 bg-white shadow-xl">
              <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
                <h2 className="text-lg font-semibold text-gray-900">User Created</h2>
                <button
                  type="button"
                  onClick={() => setCreatedUserModal(null)}
                  className="rounded-lg p-1.5 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
                  aria-label="Close created user popup"
                >
                  <X className="h-5 w-5" />
                </button>
              </div>

              <div className="space-y-3 px-5 py-4">
                <p className="text-sm font-medium text-green-700">{createdUserModal.message}</p>
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-3">
                  <div className="flex justify-between gap-3 text-sm">
                    <span className="text-gray-500">Email</span>
                    <span className="font-medium text-gray-900">{createdUserModal.email}</span>
                  </div>
                  <div className="mt-2 flex justify-between gap-3 text-sm">
                    <span className="text-gray-500">Role</span>
                    <span className="font-medium text-gray-900">{createdUserModal.role}</span>
                  </div>
                  <div className="mt-2 flex justify-between gap-3 text-sm">
                    <span className="text-gray-500">
                      {createdUserModal.isTemporaryPassword ? 'Temporary password' : 'Password'}
                    </span>
                    <span className="font-mono font-semibold text-gray-900">{createdUserModal.password || 'N/A'}</span>
                  </div>
                </div>
                {createdUserModal.isTemporaryPassword ? (
                  <p className="text-xs text-gray-500">
                    Communicate this temporary password to the user. They can change profile details at first login.
                  </p>
                ) : (
                  <p className="text-xs text-gray-500">
                    User created with the provided custom password.
                  </p>
                )}
              </div>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default CreateUserAccount;

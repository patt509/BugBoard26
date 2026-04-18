import { useCallback, useEffect, useState } from 'react';
import Login from './pages/Login';
import Issues from './pages/Issues';
import ArchivedIssues from './pages/ArchivedIssues';
import CreateIssue from './pages/CreateIssue';
import IssueDetail from './pages/IssueDetail';
import AdminDashboard from './pages/AdminDashboard';
import CreateUserAccount from './pages/CreateUserAccount';
import MandatoryUsernameModal from './components/MandatoryUsernameModal';
import { authService } from './services/auth.service';

const THEME_STORAGE_KEY = 'bugboard-theme';

const getInitialTheme = () => {
  const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);
  if (storedTheme === 'dark' || storedTheme === 'light') {
    return storedTheme;
  }

  if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }

  return 'light';
};

const isAdminRole = (role) => String(role ?? '').trim().toUpperCase().includes('ADMIN');
const isStakeholderRole = (role) => String(role ?? '').trim().toUpperCase() === 'STAKEHOLDER';
const canAccessDashboardRole = (role) => isAdminRole(role) || isStakeholderRole(role);

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentView, setCurrentView] = useState('issues');
  const [issueListView, setIssueListView] = useState('issues');
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const [editingIssue, setEditingIssue] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [theme, setTheme] = useState(() => getInitialTheme());

  const clearLocalSession = useCallback(() => {
    localStorage.removeItem('user');
    localStorage.removeItem('userId');
    localStorage.removeItem('accessToken');
    localStorage.removeItem('accessTokenExpiresAt');
    setUser(null);
    setCurrentView('issues');
    setIssueListView('issues');
    setSelectedIssueId(null);
    setEditingIssue(null);
  }, []);

  useEffect(() => {
    const storedTokenExpiry = Number(localStorage.getItem('accessTokenExpiresAt'));
    if (!Number.isNaN(storedTokenExpiry) && storedTokenExpiry > 0) {
      const nowEpochSeconds = Math.floor(Date.now() / 1000);
      if (storedTokenExpiry <= nowEpochSeconds) {
        clearLocalSession();
      }
    }

    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        const parsedUser = JSON.parse(savedUser);
        setUser(parsedUser);
        if (parsedUser?.id != null) {
          localStorage.setItem('userId', String(parsedUser.id));
        }
      } catch (error) {
        console.error('Error parsing saved user:', error);
        clearLocalSession();
      }
    }

    setLoading(false);
  }, [clearLocalSession]);

  useEffect(() => {
    if (currentView === 'create-user' && !isAdminRole(user?.role)) {
      setCurrentView('issues');
      return;
    }

    if (currentView === 'dashboard' && !canAccessDashboardRole(user?.role)) {
      setCurrentView('issues');
    }
  }, [currentView, user]);

  useEffect(() => {
    if ((currentView === 'create' || currentView === 'edit') && isStakeholderRole(user?.role)) {
      setEditingIssue(null);
      setCurrentView(selectedIssueId ? 'detail' : 'issues');
    }
  }, [currentView, user, selectedIssueId]);

  useEffect(() => {
    const root = document.documentElement;
    root.classList.toggle('dark', theme === 'dark');
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  useEffect(() => {
    const handleUnauthorized = () => {
      clearLocalSession();
    };

    window.addEventListener('auth:unauthorized', handleUnauthorized);
    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, [clearLocalSession]);

  useEffect(() => {
    const storedTokenExpiry = Number(localStorage.getItem('accessTokenExpiresAt'));
    if (!user || Number.isNaN(storedTokenExpiry) || storedTokenExpiry <= 0) {
      return undefined;
    }

    const nowEpochSeconds = Math.floor(Date.now() / 1000);
    const remainingMilliseconds = (storedTokenExpiry - nowEpochSeconds) * 1000;
    if (remainingMilliseconds <= 0) {
      clearLocalSession();
      return undefined;
    }

    const expiryTimer = window.setTimeout(() => {
      clearLocalSession();
    }, remainingMilliseconds);

    return () => window.clearTimeout(expiryTimer);
  }, [user, clearLocalSession]);

  const handleLoginSuccess = (userData) => {
    setUser(userData);
    if (userData?.id != null) {
      localStorage.setItem('userId', String(userData.id));
    }
    setCurrentView('issues');
  };

  const handleLogout = async () => {
    try {
      await authService.logout();
    } catch (error) {
      console.warn('Logout request failed, clearing local session anyway.', error);
    }

    clearLocalSession();
  };

  const handleCreateIssue = () => {
    if (isStakeholderRole(user?.role)) {
      return;
    }

    setEditingIssue(null);
    setCurrentView('create');
  };

  const handleEditIssue = (issue) => {
    if (isStakeholderRole(user?.role)) {
      return;
    }

    setEditingIssue(issue);
    setCurrentView('edit');
  };

  const handleCancelCreate = () => {
    setEditingIssue(null);
    if (selectedIssueId) {
      setCurrentView('detail');
      return;
    }
    setCurrentView('issues');
  };

  const handleCreateSuccess = (issueInfo) => {
    const wasEditing = editingIssue !== null;
    setEditingIssue(null);

    if (wasEditing && selectedIssueId) {
      setCurrentView('detail');
      setSuccessMessage(`Issue #${issueInfo.id} '${issueInfo.title}' updated successfully!`);
    } else {
      setCurrentView('issues');
      setSuccessMessage(`Issue #${issueInfo.id} '${issueInfo.title}' created successfully!`);
    }

    setTimeout(() => {
      setSuccessMessage(null);
    }, 4000);
  };

  const handleIssueClick = (issueId) => {
    setIssueListView('issues');
    setSelectedIssueId(issueId);
    setCurrentView('detail');
  };

  const handleArchivedIssueClick = (issueId) => {
    setIssueListView('archived');
    setSelectedIssueId(issueId);
    setCurrentView('detail');
  };

  const handleBackFromDetail = () => {
    setSelectedIssueId(null);
    setCurrentView(issueListView === 'archived' ? 'archived' : 'issues');
  };

  const handleSidebarNavigate = (page) => {
    if (page === 'issues') {
      setCurrentView('issues');
      setIssueListView('issues');
      setSelectedIssueId(null);
      setEditingIssue(null);
      return;
    }

    if (page === 'archived') {
      setCurrentView('archived');
      setIssueListView('archived');
      setSelectedIssueId(null);
      setEditingIssue(null);
      return;
    }

    if (page === 'dashboard' && canAccessDashboardRole(user?.role)) {
      setCurrentView('dashboard');
      setSelectedIssueId(null);
      setEditingIssue(null);
      return;
    }

    if (page === 'create-user' && isAdminRole(user?.role)) {
      setCurrentView('create-user');
      setSelectedIssueId(null);
      setEditingIssue(null);
    }
  };

  const handleToggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === 'dark' ? 'light' : 'dark'));
  };

  const handleMandatoryUsernameSubmit = async (chosenUsername) => {
    if (user?.id == null) {
      throw new Error('User session not found. Please login again.');
    }

    const updatedUser = await authService.setUsername(user.id, chosenUsername);
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));
    localStorage.setItem('userId', String(updatedUser.id));
  };

  const requiresMandatoryUsername = Boolean(user) && user?.firstLogin === true;

  let pageContent;

  if (loading) {
    pageContent = (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  } else if (!user) {
    pageContent = (
      <Login
        onLoginSuccess={handleLoginSuccess}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else if (currentView === 'create' || currentView === 'edit') {
    pageContent = (
      <CreateIssue
        user={user}
        onLogout={handleLogout}
        onCancel={handleCancelCreate}
        onSuccess={handleCreateSuccess}
        onNavigate={handleSidebarNavigate}
        editingIssue={editingIssue}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else if (currentView === 'detail' && selectedIssueId) {
    pageContent = (
      <IssueDetail
        user={user}
        onLogout={handleLogout}
        issueId={selectedIssueId}
        onBack={handleBackFromDetail}
        onEditIssue={handleEditIssue}
        onNavigate={handleSidebarNavigate}
        listSource={issueListView}
        successMessage={successMessage}
        onDismissSuccess={() => setSuccessMessage(null)}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else if (currentView === 'archived') {
    pageContent = (
      <ArchivedIssues
        user={user}
        onLogout={handleLogout}
        onIssueClick={handleArchivedIssueClick}
        onNavigate={handleSidebarNavigate}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else if (currentView === 'dashboard' && canAccessDashboardRole(user?.role)) {
    pageContent = (
      <AdminDashboard
        user={user}
        onLogout={handleLogout}
        onNavigate={handleSidebarNavigate}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else if (currentView === 'create-user' && isAdminRole(user?.role)) {
    pageContent = (
      <CreateUserAccount
        user={user}
        onLogout={handleLogout}
        onNavigate={handleSidebarNavigate}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  } else {
    pageContent = (
      <Issues
        user={user}
        onLogout={handleLogout}
        onCreateIssue={handleCreateIssue}
        onIssueClick={handleIssueClick}
        onNavigate={handleSidebarNavigate}
        successMessage={successMessage}
        onDismissSuccess={() => setSuccessMessage(null)}
        isDarkMode={theme === 'dark'}
        onToggleTheme={handleToggleTheme}
      />
    );
  }

  return (
    <>
      {pageContent}
      {requiresMandatoryUsername && (
        <MandatoryUsernameModal onSubmit={handleMandatoryUsernameSubmit} />
      )}
    </>
  );
}

export default App;

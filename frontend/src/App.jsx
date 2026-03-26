import { useEffect, useState } from 'react';
import Login from './pages/Login';
import Issues from './pages/Issues';
import CreateIssue from './pages/CreateIssue';
import IssueDetail from './pages/IssueDetail';
import AdminDashboard from './pages/AdminDashboard';

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentView, setCurrentView] = useState('issues');
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const [editingIssue, setEditingIssue] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
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
        localStorage.removeItem('user');
        localStorage.removeItem('userId');
      }
    }

    setLoading(false);
  }, []);

  useEffect(() => {
    if (currentView === 'dashboard' && user?.role !== 'ADMIN') {
      setCurrentView('issues');
    }
  }, [currentView, user]);

  const handleLoginSuccess = (userData) => {
    setUser(userData);
    if (userData?.id != null) {
      localStorage.setItem('userId', String(userData.id));
    }
    setCurrentView('issues');
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    localStorage.removeItem('userId');
    setUser(null);
    setCurrentView('issues');
    setSelectedIssueId(null);
    setEditingIssue(null);
  };

  const handleCreateIssue = () => {
    setEditingIssue(null);
    setCurrentView('create');
  };

  const handleEditIssue = (issue) => {
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
    setSelectedIssueId(issueId);
    setCurrentView('detail');
  };

  const handleBackFromDetail = () => {
    setSelectedIssueId(null);
    setCurrentView('issues');
  };

  const handleSidebarNavigate = (page) => {
    if (page === 'issues') {
      setCurrentView('issues');
      setSelectedIssueId(null);
      setEditingIssue(null);
      return;
    }

    if (page === 'dashboard' && user?.role === 'ADMIN') {
      setCurrentView('dashboard');
      setSelectedIssueId(null);
      setEditingIssue(null);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return <Login onLoginSuccess={handleLoginSuccess} />;
  }

  if (currentView === 'create' || currentView === 'edit') {
    return (
      <CreateIssue
        user={user}
        onLogout={handleLogout}
        onCancel={handleCancelCreate}
        onSuccess={handleCreateSuccess}
        onNavigate={handleSidebarNavigate}
        editingIssue={editingIssue}
      />
    );
  }

  if (currentView === 'detail' && selectedIssueId) {
    return (
      <IssueDetail
        user={user}
        onLogout={handleLogout}
        issueId={selectedIssueId}
        onBack={handleBackFromDetail}
        onEditIssue={handleEditIssue}
        onNavigate={handleSidebarNavigate}
        successMessage={successMessage}
        onDismissSuccess={() => setSuccessMessage(null)}
      />
    );
  }

  if (currentView === 'dashboard' && user?.role === 'ADMIN') {
    return (
      <AdminDashboard
        user={user}
        onLogout={handleLogout}
        onNavigate={handleSidebarNavigate}
      />
    );
  }

  return (
    <Issues
      user={user}
      onLogout={handleLogout}
      onCreateIssue={handleCreateIssue}
      onIssueClick={handleIssueClick}
      onNavigate={handleSidebarNavigate}
      successMessage={successMessage}
      onDismissSuccess={() => setSuccessMessage(null)}
    />
  );
}

export default App;

import { useState, useEffect } from 'react'
import Login from './pages/Login'
import Issues from './pages/Issues'
import CreateIssue from './pages/CreateIssue'
import IssueDetail from './pages/IssueDetail'

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentView, setCurrentView] = useState('issues'); // 'issues', 'create', or 'detail'
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  useEffect(() => {
    // Check if user is already logged in
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch (e) {
        console.error('Error parsing saved user:', e);
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  const handleLoginSuccess = (userData) => {
    setUser(userData);
  };

  const handleLogout = () => {
    localStorage.removeItem('user');
    setUser(null);
  };

  const handleCreateIssue = () => {
    setCurrentView('create');
  };

  const handleCancelCreate = () => {
    setCurrentView('issues');
  };

  const handleCreateSuccess = (issueInfo) => {
    setCurrentView('issues');
    if (issueInfo) {
      setSuccessMessage(`Issue #${issueInfo.id} '${issueInfo.title}' created successfully!`);
      // Auto-dismiss after 4 seconds
      setTimeout(() => {
        setSuccessMessage(null);
      }, 4000);
    }
  };

  const handleIssueClick = (issueId) => {
    setSelectedIssueId(issueId);
    setCurrentView('detail');
  };

  const handleBackFromDetail = () => {
    setSelectedIssueId(null);
    setCurrentView('issues');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <>
      {user ? (
        currentView === 'create' ? (
          <CreateIssue
            user={user}
            onLogout={handleLogout}
            onCancel={handleCancelCreate}
            onSuccess={handleCreateSuccess}
          />
        ) : currentView === 'detail' && selectedIssueId ? (
          <IssueDetail
            user={user}
            onLogout={handleLogout}
            issueId={selectedIssueId}
            onBack={handleBackFromDetail}
          />
        ) : (
          <Issues
            user={user}
            onLogout={handleLogout}
            onCreateIssue={handleCreateIssue}
            onIssueClick={handleIssueClick}
            successMessage={successMessage}
            onDismissSuccess={() => setSuccessMessage(null)}
          />
        )
      ) : (
        <Login onLoginSuccess={handleLoginSuccess} />
      )}
    </>
  )
}

export default App

<<<<<<< HEAD
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import FinalizeProfile from './pages/FinalizeProfile';
import Dashboard from './pages/Dashboard';
import './App.css';

function App() {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          {/* Public Routes */}
          <Route path="/login" element={<Login />} />

          {/* Protected Routes */}
          <Route
            path="/finalize-profile"
            element={
              <ProtectedRoute>
                <FinalizeProfile />
              </ProtectedRoute>
            }
          />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
=======
import { useState, useEffect } from 'react'
import Login from './pages/Login'
import Issues from './pages/Issues'
import CreateIssue from './pages/CreateIssue'
import IssueDetail from './pages/IssueDetail'

function App() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentView, setCurrentView] = useState('issues'); // 'issues', 'create', 'edit', or 'detail'
  const [selectedIssueId, setSelectedIssueId] = useState(null);
  const [editingIssue, setEditingIssue] = useState(null); // Issue data for editing
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
    setEditingIssue(null);
    setCurrentView('create');
  };

  const handleEditIssue = (issue) => {
    setEditingIssue(issue);
    setCurrentView('edit');
  };

  const handleCancelCreate = () => {
    setEditingIssue(null);
    // If we were editing, go back to detail view
    if (editingIssue && selectedIssueId) {
      setCurrentView('detail');
    } else {
      setCurrentView('issues');
    }
  };

  const handleCreateSuccess = (issueInfo) => {
    const wasEditing = editingIssue !== null;
    setEditingIssue(null);
    
    if (wasEditing && selectedIssueId) {
      // After editing, go back to detail view
      setCurrentView('detail');
      setSuccessMessage(`Issue #${issueInfo.id} '${issueInfo.title}' updated successfully!`);
    } else {
      // After creating, go back to issues list
      setCurrentView('issues');
      setSuccessMessage(`Issue #${issueInfo.id} '${issueInfo.title}' created successfully!`);
    }
    
    // Auto-dismiss after 4 seconds
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
        (currentView === 'create' || currentView === 'edit') ? (
          <CreateIssue
            user={user}
            onLogout={handleLogout}
            onCancel={handleCancelCreate}
            onSuccess={handleCreateSuccess}
            editingIssue={editingIssue}
          />
        ) : currentView === 'detail' && selectedIssueId ? (
          <IssueDetail
            user={user}
            onLogout={handleLogout}
            issueId={selectedIssueId}
            onBack={handleBackFromDetail}
            onEditIssue={handleEditIssue}
            successMessage={successMessage}
            onDismissSuccess={() => setSuccessMessage(null)}
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
>>>>>>> frontend
}

export default App;

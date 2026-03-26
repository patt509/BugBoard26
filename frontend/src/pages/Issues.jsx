import { useState, useEffect, useCallback } from 'react';
import { Search, Plus, CheckCircle, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import { issueService } from '../services/issue.service';

function Issues({ user, onLogout, onCreateIssue, onIssueClick, onNavigate, successMessage, onDismissSuccess }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedSearchQuery, setDebouncedSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const currentPage = 'issues';

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery.trim());
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery]);

  const buildSearchParams = useCallback(() => {
    const params = {};

    if (debouncedSearchQuery) {
      params.term = debouncedSearchQuery;
    }

    if (statusFilter !== 'all') {
      params.status = statusFilter;
    }

    if (priorityFilter !== 'all') {
      params.priority = priorityFilter;
    }

    return params;
  }, [debouncedSearchQuery, statusFilter, priorityFilter]);

  const fetchIssues = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await issueService.search(buildSearchParams());
      setIssues(data);
    } catch (err) {
      console.error('Error fetching issues:', err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [buildSearchParams]);

  useEffect(() => {
    fetchIssues();
  }, [fetchIssues]);

  const handleSidebarNavigate = (page) => {
    if (onNavigate) {
      onNavigate(page);
    }
  };

  const getStatusBadgeClass = (status) => {
    const classes = {
      'TODO': 'bg-green-100 text-green-700',
      'Open': 'bg-green-100 text-green-700',
      'IN_PROGRESS': 'bg-blue-100 text-blue-700',
      'In Progress': 'bg-blue-100 text-blue-700',
      'RESOLVED': 'bg-purple-100 text-purple-700',
      'Resolved': 'bg-purple-100 text-purple-700',
      'CLOSED': 'bg-red-100 text-red-700',
      'Closed': 'bg-red-100 text-red-700',
    };
    return classes[status] || 'bg-gray-100 text-gray-700';
  };

  const getStatusLabel = (status) => {
    const labels = {
      'TODO': 'TODO',
      'IN_PROGRESS': 'In Progress',
      'RESOLVED': 'Resolved',
      'CLOSED': 'Closed',
    };
    return labels[status] || status;
  };

  const getPriorityBadgeClass = (priority) => {
    const classes = {
      'CRITICAL': 'bg-red-500 text-white',
      'Critical': 'bg-red-500 text-white',
      'HIGH': 'bg-red-100 text-red-700',
      'High': 'bg-red-100 text-red-700',
      'MEDIUM': 'bg-yellow-100 text-yellow-700',
      'Medium': 'bg-yellow-100 text-yellow-700',
      'LOW': 'bg-green-100 text-green-700',
      'Low': 'bg-green-100 text-green-700',
    };
    return classes[priority] || 'bg-gray-100 text-gray-700';
  };

  const getPriorityLabel = (priority) => {
    const labels = {
      'CRITICAL': 'Critical',
      'HIGH': 'High',
      'MEDIUM': 'Medium',
      'LOW': 'Low',
    };
    return labels[priority] || priority;
  };

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={handleSidebarNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        {/* Header */}
        <header className="bg-white border-b border-gray-200 px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">All Issues</h1>
            </div>
            <div className="flex items-center gap-4">
              <span className="text-sm text-gray-600">
                {user.username || user.email}
              </span>
              <button
                onClick={onLogout}
                className="text-sm text-gray-600 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        {/* Content */}
        <div className="p-8">
          {/* Filters and Search */}
          <div className="flex flex-wrap items-center gap-4 mb-6">
            {/* Search */}
            <div className="flex-1 min-w-[300px]">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                <input
                  type="text"
                  placeholder="Search issues..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Filters */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
            >
              <option value="all">Status</option>
              <option value="TODO">Open</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>

            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
            >
              <option value="all">Priority</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>

            <select className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
              <option>Assignee</option>
            </select>

            <select className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
              <option>Sort by</option>
            </select>

            {/* New Issue Button */}
            <button 
              onClick={onCreateIssue}
              className="ml-auto flex items-center gap-2 px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors font-medium"
            >
              <Plus size={20} />
              New Issue
            </button>
          </div>

          {/* Table */}
          <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                  <p className="text-gray-600">Loading issues...</p>
                </div>
              </div>
            ) : error ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="text-red-600 mb-2">Error loading issues</p>
                  <p className="text-gray-500 text-sm">{error}</p>
                  <button 
                    onClick={fetchIssues}
                    className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                  >
                    Retry
                  </button>
                </div>
              </div>
            ) : issues.length === 0 ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="text-gray-500 text-lg">No issues found</p>
                  <p className="text-gray-400 text-sm mt-2">Create your first issue to get started</p>
                </div>
              </div>
            ) : (
              <table className="w-full">
                <thead className="bg-gray-50 border-b border-gray-200">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      ID
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Title
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Priority
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Updated
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {issues.map((issue) => {
                    const isClosedIssue = issue.status === 'CLOSED';
                    const isResolvedIssue = issue.status === 'RESOLVED';
                    const isCompletedIssue = isClosedIssue || isResolvedIssue;
                    const rowClassName = isClosedIssue
                      ? 'bg-slate-300 hover:bg-slate-400'
                      : isResolvedIssue
                        ? 'bg-indigo-100 hover:bg-indigo-200'
                        : 'hover:bg-gray-50';

                    return (
                      <tr 
                        key={issue.id} 
                        className={`cursor-pointer transition-colors ${rowClassName}`}
                        onClick={() => onIssueClick && onIssueClick(issue.id)}
                      >
                        <td className={`px-6 py-4 whitespace-nowrap border-l-4 text-sm font-medium ${
                          isCompletedIssue ? 'border-l-slate-700 text-slate-700' : 'border-l-transparent text-gray-900'
                        }`}>
                          #{issue.id}
                        </td>
                        <td className={`px-6 py-4 text-sm ${
                          isCompletedIssue ? 'text-slate-700 line-through' : 'text-gray-900'
                        }`}>
                          {issue.title}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getStatusBadgeClass(issue.status)}`}>
                            {getStatusLabel(issue.status)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex px-2.5 py-1 rounded-full text-xs font-medium ${getPriorityBadgeClass(issue.priority)}`}>
                            {getPriorityLabel(issue.priority)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {issue.updatedAt ? new Date(issue.updatedAt).toLocaleString() : 'N/A'}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      </main>

      {/* Success Toast Notification */}
      {successMessage && (
        <div className="fixed bottom-8 left-1/2 transform -translate-x-1/2 animate-slide-up z-50">
          <div className="bg-green-50 border border-green-200 rounded-lg shadow-lg px-6 py-4 flex items-center gap-3 min-w-[400px]">
            <CheckCircle className="w-6 h-6 text-green-600 flex-shrink-0" />
            <span className="text-green-800 font-medium flex-1">{successMessage}</span>
            <button
              onClick={onDismissSuccess}
              className="text-green-600 hover:text-green-800 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Issues;

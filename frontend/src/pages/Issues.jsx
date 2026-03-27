import { useState, useEffect, useCallback, useRef } from 'react';
import { Search, Plus, CheckCircle, X } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import { issueService } from '../services/issue.service';
import { authService } from '../services/auth.service';

const API_TIMESTAMP_WITH_TIMEZONE_REGEX = /(Z|[+-]\d{2}:?\d{2})$/i;

const parseApiDate = (input) => {
  if (!input) {
    return null;
  }

  const rawValue = String(input).trim();
  if (!rawValue) {
    return null;
  }

  const normalizedValue = rawValue.includes('T')
    ? rawValue
    : rawValue.replace(' ', 'T');
  const valueWithTimezone = API_TIMESTAMP_WITH_TIMEZONE_REGEX.test(normalizedValue)
    ? normalizedValue
    : `${normalizedValue}Z`;
  const parsedDate = new Date(valueWithTimezone);

  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
};

const formatIssuePublicationDate = (createdAt) => {
  const parsedDate = parseApiDate(createdAt);
  if (!parsedDate) {
    return 'N/A';
  }

  return parsedDate.toLocaleDateString('en-US', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  });
};

function Issues({
  user,
  onLogout,
  onCreateIssue,
  onIssueClick,
  onNavigate,
  successMessage,
  onDismissSuccess,
  isDarkMode,
  onToggleTheme
}) {
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [priorityFilter, setPriorityFilter] = useState('all');
  const [typeFilter, setTypeFilter] = useState('all');
  const [assigneeFilter, setAssigneeFilter] = useState('all');
  const [debouncedFilters, setDebouncedFilters] = useState({
    term: '',
    status: 'all',
    priority: 'all',
    type: 'all',
    assigneeId: 'all'
  });
  const [assignableUsers, setAssignableUsers] = useState([]);
  const [assignableUsersLoading, setAssignableUsersLoading] = useState(false);
  const [assignableUsersError, setAssignableUsersError] = useState(null);
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const lastSearchKeyRef = useRef(null);
  const currentPage = 'issues';

  useEffect(() => {
    const timer = setTimeout(() => {
      const nextFilters = {
        term: searchQuery.trim(),
        status: statusFilter,
        priority: priorityFilter,
        type: typeFilter,
        assigneeId: assigneeFilter
      };
      setDebouncedFilters((prevFilters) => {
        const isUnchanged =
          prevFilters.term === nextFilters.term &&
          prevFilters.status === nextFilters.status &&
          prevFilters.priority === nextFilters.priority &&
          prevFilters.type === nextFilters.type &&
          prevFilters.assigneeId === nextFilters.assigneeId;

        return isUnchanged ? prevFilters : nextFilters;
      });
    }, 300);

    return () => clearTimeout(timer);
  }, [searchQuery, statusFilter, priorityFilter, typeFilter, assigneeFilter]);

  useEffect(() => {
    let mounted = true;

    (async () => {
      try {
        setAssignableUsersLoading(true);
        setAssignableUsersError(null);
        const users = await authService.getAssignableUsers(user?.id);
        if (!mounted) {
          return;
        }
        setAssignableUsers(Array.isArray(users) ? users : []);
      } catch (err) {
        console.warn('Unable to load assignable users for filters', err);
        if (mounted) {
          setAssignableUsers([]);
          setAssignableUsersError(err.message || 'Failed to load assignee options.');
        }
      } finally {
        if (mounted) {
          setAssignableUsersLoading(false);
        }
      }
    })();

    return () => {
      mounted = false;
    };
  }, [user?.id]);

  const buildSearchParams = useCallback(() => {
    const params = {};

    if (debouncedFilters.term) {
      params.term = debouncedFilters.term;
    }

    if (debouncedFilters.status !== 'all') {
      params.status = debouncedFilters.status;
    }

    if (debouncedFilters.priority !== 'all') {
      params.priority = debouncedFilters.priority;
    }

    if (debouncedFilters.type !== 'all') {
      params.type = debouncedFilters.type;
    }

    if (debouncedFilters.assigneeId !== 'all') {
      params.assigneeId = debouncedFilters.assigneeId;
    }

    return params;
  }, [debouncedFilters]);

  const fetchIssues = useCallback(async (forceRefresh = false) => {
    const searchParams = buildSearchParams();
    const searchKey = JSON.stringify(searchParams);
    if (!forceRefresh && lastSearchKeyRef.current === searchKey) {
      return;
    }

    try {
      lastSearchKeyRef.current = searchKey;
      setLoading(true);
      setError(null);
      const data = await issueService.search(searchParams);
      setIssues(data);
    } catch (err) {
      lastSearchKeyRef.current = null;
      console.error('Error fetching issues:', err);
      setError(err.message || 'Failed to load issues.');
    } finally {
      setLoading(false);
    }
  }, [buildSearchParams]);

  useEffect(() => {
    fetchIssues(false);
  }, [fetchIssues]);

  const handleSidebarNavigate = (page) => {
    if (onNavigate) {
      onNavigate(page);
    }
  };

  const getStatusBadgeClass = (status) => {
    const classes = {
      'TODO': 'bg-green-200 text-green-900',
      'Open': 'bg-green-200 text-green-900',
      'IN_PROGRESS': 'bg-blue-200 text-blue-900',
      'In Progress': 'bg-blue-200 text-blue-900',
      'RESOLVED': 'bg-purple-200 text-purple-900',
      'Resolved': 'bg-purple-200 text-purple-900',
      'CLOSED': 'bg-red-200 text-red-900',
      'Closed': 'bg-red-200 text-red-900',
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
      'CRITICAL': 'bg-red-700 text-white',
      'Critical': 'bg-red-700 text-white',
      'HIGH': 'bg-red-200 text-red-900',
      'High': 'bg-red-200 text-red-900',
      'MEDIUM': 'bg-yellow-200 text-yellow-900',
      'Medium': 'bg-yellow-200 text-yellow-900',
      'LOW': 'bg-green-200 text-green-900',
      'Low': 'bg-green-200 text-green-900',
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

  const getTypeLabel = (type) => {
    const labels = {
      BUG: 'Bug',
      FEATURE: 'Feature',
      DOCUMENTATION: 'Documentation',
      QUESTION: 'Question'
    };
    return labels[type] || type || 'Bug';
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
            <div className="flex items-center gap-3">
              <ThemeToggle isDarkMode={isDarkMode} onToggle={onToggleTheme} />
              <span className="hidden text-sm text-gray-600 md:inline">
                {user.username || user.email}
              </span>
              <button
                onClick={onLogout}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
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

            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white"
            >
              <option value="all">Type</option>
              <option value="BUG">Bug</option>
              <option value="FEATURE">Feature</option>
              <option value="DOCUMENTATION">Documentation</option>
              <option value="QUESTION">Question</option>
            </select>

            <select
              value={assigneeFilter}
              onChange={(e) => setAssigneeFilter(e.target.value)}
              className="px-4 py-2.5 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white disabled:bg-gray-100"
              disabled={assignableUsersLoading}
            >
              <option value="all">
                {assignableUsersLoading ? 'Loading assignees...' : 'Assignee'}
              </option>
              {assignableUsers.map((assignableUser) => (
                <option key={assignableUser.id} value={assignableUser.id}>
                  {assignableUser.username || assignableUser.email}
                </option>
              ))}
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
          {assignableUsersError && (
            <p className="mb-4 text-sm text-red-600">{assignableUsersError}</p>
          )}

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
                    onClick={() => fetchIssues(true)}
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
                      Type
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Assignee
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Published
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
                        : 'bg-white hover:bg-slate-100';

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
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                          {getTypeLabel(issue.type)}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                          {issue.assigneeUsername || 'Unassigned'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {formatIssuePublicationDate(issue.createdAt)}
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

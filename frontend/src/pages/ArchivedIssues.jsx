import { useCallback, useEffect, useMemo, useState } from 'react';
import { Loader2, RefreshCw, Search } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';

const API_TIMESTAMP_WITH_TIMEZONE_REGEX = /(Z|[+-]\d{2}:?\d{2})$/i;
const BADGE_BASE_CLASS = 'inline-flex rounded-full px-2.5 py-1 text-xs font-medium';

const STATUS_BADGE_CLASSES = {
  TODO: 'bg-green-200 text-green-900',
  OPEN: 'bg-green-200 text-green-900',
  IN_PROGRESS: 'bg-blue-200 text-blue-900',
  RESOLVED: 'bg-purple-200 text-purple-900',
  CLOSED: 'bg-red-200 text-red-900'
};

const PRIORITY_BADGE_CLASSES = {
  CRITICAL: 'bg-red-700 text-white',
  HIGH: 'bg-red-200 text-red-900',
  MEDIUM: 'bg-yellow-200 text-yellow-900',
  LOW: 'bg-green-200 text-green-900'
};

const TYPE_BADGE_CLASSES = {
  BUG: 'bg-rose-200 text-rose-900',
  FEATURE: 'bg-emerald-200 text-emerald-900',
  DOCUMENTATION: 'bg-sky-200 text-sky-900',
  QUESTION: 'bg-amber-200 text-amber-900'
};

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

const formatDate = (dateValue) => {
  const parsedDate = parseApiDate(dateValue);
  if (!parsedDate) {
    return 'N/A';
  }

  return parsedDate.toLocaleDateString('it-IT', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  });
};

const getStatusBadgeClass = (status) => {
  const normalizedStatus = String(status || '').trim().toUpperCase().replace(' ', '_');
  return STATUS_BADGE_CLASSES[normalizedStatus] || 'bg-gray-100 text-gray-700';
};

const getStatusLabel = (status) => {
  const labels = {
    TODO: 'TODO',
    IN_PROGRESS: 'In Progress',
    RESOLVED: 'Resolved',
    CLOSED: 'Closed'
  };
  return labels[status] || status;
};

const getPriorityBadgeClass = (priority) => {
  const normalizedPriority = String(priority || '').trim().toUpperCase();
  return PRIORITY_BADGE_CLASSES[normalizedPriority] || 'bg-gray-100 text-gray-700';
};

const getPriorityLabel = (priority) => {
  const labels = {
    CRITICAL: 'Critical',
    HIGH: 'High',
    MEDIUM: 'Medium',
    LOW: 'Low'
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

const getTypeBadgeClass = (type) => {
  const normalizedType = String(type || '').trim().toUpperCase();
  return TYPE_BADGE_CLASSES[normalizedType] || 'bg-gray-100 text-gray-700';
};

function ArchivedIssues({
  user,
  onLogout,
  onIssueClick,
  onNavigate,
  isDarkMode,
  onToggleTheme
}) {
  const [issues, setIssues] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const currentPage = 'archived';

  const fetchArchivedIssues = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await issueService.getArchived();
      setIssues(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Error fetching archived issues:', err);
      setError(err.message || 'Failed to load archived issues.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchArchivedIssues();
  }, [fetchArchivedIssues]);

  const filteredIssues = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    if (!normalizedQuery) {
      return issues;
    }

    return issues.filter((issue) => {
      const title = String(issue?.title ?? '').toLowerCase();
      const description = String(issue?.description ?? '').toLowerCase();
      const reporter = String(issue?.reporterName ?? '').toLowerCase();
      const assignee = String(issue?.assigneeUsername ?? '').toLowerCase();
      const id = String(issue?.id ?? '');
      return title.includes(normalizedQuery) ||
        description.includes(normalizedQuery) ||
        reporter.includes(normalizedQuery) ||
        assignee.includes(normalizedQuery) ||
        id.includes(normalizedQuery);
    });
  }, [issues, searchQuery]);

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        <header className="fixed inset-x-0 top-0 z-40 border-b border-gray-200 bg-white/95 px-8 py-4 pl-20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Archived Issues</h1>
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

        <div className="p-8 pt-28">
          <div className="mb-4 flex flex-wrap items-center gap-3">
            <div className="relative min-w-[280px] flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                placeholder="Search archived issues..."
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                className="h-11 w-full rounded-xl border border-gray-300 bg-white pl-10 pr-4 text-sm shadow-sm transition-colors focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <button
              type="button"
              onClick={fetchArchivedIssues}
              disabled={loading}
              className="inline-flex h-11 items-center gap-2 rounded-xl border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </button>
          </div>

          <div className="bg-white overflow-hidden rounded-lg border border-gray-200">
            {loading ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <Loader2 className="mx-auto mb-4 h-12 w-12 animate-spin text-blue-600" />
                  <p className="text-gray-600">Loading archived issues...</p>
                </div>
              </div>
            ) : error ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="mb-2 text-red-600">Error loading archived issues</p>
                  <p className="text-sm text-gray-500">{error}</p>
                </div>
              </div>
            ) : filteredIssues.length === 0 ? (
              <div className="flex items-center justify-center py-20">
                <div className="text-center">
                  <p className="text-lg text-gray-500">No archived issues found</p>
                </div>
              </div>
            ) : (
              <table className="w-full">
                <thead className="border-b border-gray-200 bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">ID</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Title</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Status</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Priority</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Type</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Assignee</th>
                    <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500">Archived</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-200 bg-white">
                  {filteredIssues.map((issue) => {
                    const isClosedIssue = issue.status === 'CLOSED';
                    const isResolvedIssue = issue.status === 'RESOLVED';
                    const isCompletedIssue = isClosedIssue || isResolvedIssue;
                    const rowClassName = isClosedIssue
                      ? 'issue-row-closed'
                      : isResolvedIssue
                        ? 'issue-row-resolved'
                        : 'issue-row-default';

                    return (
                      <tr
                        key={issue.id}
                        onClick={() => onIssueClick && onIssueClick(issue.id, 'archived')}
                        className={`cursor-pointer transition-colors ${rowClassName}`}
                      >
                        <td className={`border-l-4 px-6 py-4 text-sm font-medium ${
                          isCompletedIssue ? 'border-l-slate-700 text-slate-700' : 'border-l-transparent text-gray-900'
                        }`}>#{issue.id}</td>
                        <td className={`px-6 py-4 text-sm ${
                          isCompletedIssue ? 'text-slate-700 line-through' : 'text-gray-900'
                        }`}>{issue.title}</td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${getStatusBadgeClass(issue.status)}`}>
                            {getStatusLabel(issue.status)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-medium ${getPriorityBadgeClass(issue.priority)}`}>
                            {getPriorityLabel(issue.priority)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap">
                          <span className={`${BADGE_BASE_CLASS} ${getTypeBadgeClass(issue.type)}`}>
                            {getTypeLabel(issue.type)}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-700">{issue.assigneeUsername || 'Unassigned'}</td>
                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                          {formatDate(issue.archivedAt || issue.updatedAt || issue.createdAt)}
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
    </div>
  );
}

export default ArchivedIssues;

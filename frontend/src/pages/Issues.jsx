import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { Search, Plus, CheckCircle, X, ChevronDown, Check, Loader2 } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import UserIdentity from '../components/UserIdentity';
import { issueService } from '../services/issue.service';
import { authService } from '../services/auth.service';

const API_TIMESTAMP_WITH_TIMEZONE_REGEX = /(Z|[+-]\d{2}:?\d{2})$/i;
const DICEBEAR_BASE_URL = 'https://api.dicebear.com/9.x/glass/svg';

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

const getUserDisplayName = (targetUser) => targetUser?.username || targetUser?.email || 'Unknown user';

const getUserAvatarUrl = (targetUser) => {
  const seed = encodeURIComponent(`${getUserDisplayName(targetUser)}-${targetUser?.id ?? 'unknown'}`);
  return `${DICEBEAR_BASE_URL}?seed=${seed}&radius=50&size=64`;
};

const STATUS_FILTER_OPTIONS = [
  { value: 'all', label: 'Status' },
  { value: 'TODO', label: 'Open' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'RESOLVED', label: 'Resolved' },
  { value: 'CLOSED', label: 'Closed' },
];

const PRIORITY_FILTER_OPTIONS = [
  { value: 'all', label: 'Priority' },
  { value: 'CRITICAL', label: 'Critical' },
  { value: 'HIGH', label: 'High' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'LOW', label: 'Low' },
];

const TYPE_FILTER_OPTIONS = [
  { value: 'all', label: 'Type' },
  { value: 'BUG', label: 'Bug' },
  { value: 'FEATURE', label: 'Feature' },
  { value: 'DOCUMENTATION', label: 'Documentation' },
  { value: 'QUESTION', label: 'Question' },
];

function FilterDropdown({ value, options, onChange, disabled = false, loading = false }) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef(null);
  const selectedOption = options.find((option) => option.value === value) || options[0];

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
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, [open]);

  return (
    <div ref={rootRef} className="relative min-w-[150px]">
      <button
        type="button"
        onClick={() => !disabled && setOpen((prev) => !prev)}
        disabled={disabled}
        className="inline-flex w-full items-center justify-between gap-2 rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
      >
        <span className="truncate">{selectedOption?.label || 'Select'}</span>
        {loading ? (
          <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
        ) : (
          <ChevronDown className={`h-4 w-4 text-gray-500 transition-transform ${open ? 'rotate-180' : ''}`} />
        )}
      </button>

      {open && !disabled && (
        <div className="absolute left-0 top-[calc(100%+8px)] z-30 w-full rounded-xl border border-gray-200 bg-white p-1.5 shadow-xl">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
              className="flex w-full items-center justify-between rounded-lg px-3 py-2 text-left text-sm text-gray-700 transition-colors hover:bg-gray-100"
            >
              <span>{option.label}</span>
              {value === option.value && <Check className="h-4 w-4 text-blue-600" />}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

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
  const [showAssigneeModal, setShowAssigneeModal] = useState(false);
  const [assigneeSearchQuery, setAssigneeSearchQuery] = useState('');
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

  useEffect(() => {
    if (!showAssigneeModal) {
      return undefined;
    }

    const handleEscapeKey = (event) => {
      if (event.key === 'Escape') {
        setShowAssigneeModal(false);
      }
    };

    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [showAssigneeModal]);

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

  const assigneeFilterOptions = [
    { value: 'all', label: 'Assignee' },
    ...assignableUsers.map((assignableUser) => ({
      value: String(assignableUser.id),
      label: assignableUser.username || assignableUser.email,
    })),
  ];
  const selectedAssigneeOption =
    assigneeFilterOptions.find((option) => option.value === assigneeFilter) || assigneeFilterOptions[0];
  const selectedAssigneeUser =
    assignableUsers.find((assignableUser) => String(assignableUser.id) === assigneeFilter) || null;
  const filteredAssigneeUsers = useMemo(() => {
    const normalizedQuery = assigneeSearchQuery.trim().toLowerCase();
    if (!normalizedQuery) {
      return assignableUsers;
    }

    return assignableUsers.filter((assignableUser) => {
      const username = (assignableUser.username || '').toLowerCase();
      const email = (assignableUser.email || '').toLowerCase();
      return username.includes(normalizedQuery) || email.includes(normalizedQuery);
    });
  }, [assignableUsers, assigneeSearchQuery]);

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

        {/* Content */}
        <div className="p-8">
          {/* Filters and Search */}
          <div className="mb-4 flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
            <div className="flex flex-1 flex-wrap items-center gap-3">
              <div className="min-w-[280px] flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" size={20} />
                  <input
                    type="text"
                    placeholder="Search issues..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="w-full rounded-xl border border-gray-300 bg-white py-2.5 pl-10 pr-4 text-sm shadow-sm transition-colors focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <FilterDropdown
                value={statusFilter}
                options={STATUS_FILTER_OPTIONS}
                onChange={setStatusFilter}
              />
              <FilterDropdown
                value={priorityFilter}
                options={PRIORITY_FILTER_OPTIONS}
                onChange={setPriorityFilter}
              />
              <FilterDropdown
                value={typeFilter}
                options={TYPE_FILTER_OPTIONS}
                onChange={setTypeFilter}
              />
              <button
                type="button"
                onClick={() => setShowAssigneeModal(true)}
                disabled={assignableUsersLoading}
                className="inline-flex min-w-[180px] items-center justify-between gap-2 rounded-xl border border-gray-300 bg-white px-3 py-2.5 text-sm font-medium text-gray-700 shadow-sm transition-colors hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <div className="flex min-w-0 items-center gap-2">
                  {selectedAssigneeUser ? (
                    <div className="h-6 w-6 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
                      <img
                        src={getUserAvatarUrl(selectedAssigneeUser)}
                        alt={`${getUserDisplayName(selectedAssigneeUser)} avatar`}
                        className="h-full w-full object-cover"
                        loading="lazy"
                        referrerPolicy="no-referrer"
                      />
                    </div>
                  ) : (
                    <div className="flex h-6 w-6 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-[11px] font-semibold text-gray-500">
                      All
                    </div>
                  )}
                  <span className="truncate">{selectedAssigneeOption?.label || 'Assignee'}</span>
                </div>
                {assignableUsersLoading ? (
                  <Loader2 className="h-4 w-4 animate-spin text-gray-500" />
                ) : (
                  <ChevronDown className="h-4 w-4 text-gray-500" />
                )}
              </button>
            </div>

            {/* New Issue Button */}
            <button
              onClick={onCreateIssue}
              className="inline-flex shrink-0 items-center justify-center gap-2 rounded-xl bg-blue-600 px-6 py-2.5 font-medium text-white transition-colors hover:bg-blue-700"
            >
              <Plus size={20} />
              New Issue
            </button>
          </div>

          <div className="mb-2 min-h-[20px]">
            {loading && (
              <div className="inline-flex items-center gap-2 text-xs text-gray-500">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                Updating issues...
              </div>
            )}
          </div>

          {assignableUsersError && (
            <p className="mb-4 text-sm text-red-600">{assignableUsersError}</p>
          )}

          {showAssigneeModal && (
            <div
              className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 px-4"
              onClick={() => setShowAssigneeModal(false)}
            >
              <div
                className="w-full max-w-lg rounded-2xl border border-gray-200 bg-white shadow-xl"
                onClick={(event) => event.stopPropagation()}
              >
                <div className="flex items-center justify-between border-b border-gray-200 px-5 py-4">
                  <h2 className="text-lg font-semibold text-gray-900">Select assignee</h2>
                  <button
                    type="button"
                    onClick={() => setShowAssigneeModal(false)}
                    className="rounded-lg p-1 text-gray-500 transition-colors hover:bg-gray-100 hover:text-gray-700"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                <div className="px-5 py-4">
                  <div className="relative mb-3">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <input
                      type="text"
                      value={assigneeSearchQuery}
                      onChange={(event) => setAssigneeSearchQuery(event.target.value)}
                      placeholder="Search users..."
                      className="w-full rounded-xl border border-gray-300 bg-white py-2.5 pl-9 pr-3 text-sm text-gray-700 focus:border-transparent focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>

                  <div className="max-h-80 overflow-y-auto rounded-xl border border-gray-200">
                    <button
                      type="button"
                      onClick={() => {
                        setAssigneeFilter('all');
                        setShowAssigneeModal(false);
                      }}
                      className={`flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-gray-50 ${
                        assigneeFilter === 'all' ? 'bg-blue-50' : ''
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 items-center justify-center rounded-full border border-gray-200 bg-gray-100 text-xs font-semibold text-gray-600">
                          All
                        </div>
                        <div>
                          <p className="text-sm font-medium text-gray-900">All assignees</p>
                          <p className="text-xs text-gray-500">No assignee filter applied</p>
                        </div>
                      </div>
                      {assigneeFilter === 'all' && <Check className="h-4 w-4 text-blue-600" />}
                    </button>

                    {filteredAssigneeUsers.length === 0 ? (
                      <div className="px-4 py-6 text-center text-sm text-gray-500">No users found.</div>
                    ) : (
                      filteredAssigneeUsers.map((assignableUser) => {
                        const optionValue = String(assignableUser.id);
                        const isSelected = assigneeFilter === optionValue;
                        const displayName = getUserDisplayName(assignableUser);

                        return (
                          <button
                            key={assignableUser.id}
                            type="button"
                            onClick={() => {
                              setAssigneeFilter(optionValue);
                              setShowAssigneeModal(false);
                            }}
                            className={`flex w-full items-center justify-between border-t border-gray-100 px-4 py-3 text-left transition-colors hover:bg-gray-50 ${
                              isSelected ? 'bg-blue-50' : ''
                            }`}
                          >
                            <div className="flex min-w-0 items-center gap-3">
                              <div className="h-8 w-8 overflow-hidden rounded-full border border-gray-200 bg-gray-100">
                                <img
                                  src={getUserAvatarUrl(assignableUser)}
                                  alt={`${displayName} avatar`}
                                  className="h-full w-full object-cover"
                                  loading="lazy"
                                  referrerPolicy="no-referrer"
                                />
                              </div>
                              <div className="min-w-0">
                                <p className="truncate text-sm font-medium text-gray-900">{displayName}</p>
                                {assignableUser.username && assignableUser.email && (
                                  <p className="truncate text-xs text-gray-500">{assignableUser.email}</p>
                                )}
                              </div>
                            </div>
                            {isSelected && <Check className="h-4 w-4 text-blue-600" />}
                          </button>
                        );
                      })
                    )}
                  </div>
                </div>

                <div className="flex justify-end border-t border-gray-200 px-5 py-3">
                  <button
                    type="button"
                    onClick={() => setShowAssigneeModal(false)}
                    className="rounded-lg border border-gray-300 px-4 py-2 text-sm text-gray-700 transition-colors hover:bg-gray-50"
                  >
                    Close
                  </button>
                </div>
              </div>
            </div>
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
                      ? 'issue-row-closed'
                      : isResolvedIssue
                        ? 'issue-row-resolved'
                        : 'issue-row-default';

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

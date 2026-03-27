import { useCallback, useEffect, useMemo, useState } from 'react';
import { Activity, CheckCircle2, CircleSlash, Clock3, FolderOpen } from 'lucide-react';
import Sidebar from '../components/Sidebar';
import ThemeToggle from '../components/ThemeToggle';
import { issueService } from '../services/issue.service';

const STATUS_COLORS = {
  TODO: 'bg-green-500',
  IN_PROGRESS: 'bg-blue-500',
  RESOLVED: 'bg-purple-500',
  CLOSED: 'bg-gray-500'
};

const PRIORITY_COLORS = {
  CRITICAL: 'bg-red-600',
  HIGH: 'bg-red-500',
  MEDIUM: 'bg-yellow-500',
  LOW: 'bg-green-500'
};

const formatLabel = (value) =>
  value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');

const formatAverageResolutionTime = (hours) => {
  if (typeof hours !== 'number' || Number.isNaN(hours)) {
    return 'N/A';
  }

  return `${hours.toFixed(1)} h`;
};

function MetricCard({ title, value, icon: Icon, accentClass }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-sm font-medium text-gray-500">{title}</p>
          <p className="mt-2 text-2xl font-bold text-gray-900">{value}</p>
        </div>
        <div className={`rounded-lg p-2 ${accentClass}`}>
          <Icon className="h-5 w-5 text-white" />
        </div>
      </div>
    </div>
  );
}

function DistributionList({ title, data, colorsMap }) {
  const entries = useMemo(() => Object.entries(data || {}), [data]);
  const maxValue = useMemo(() => {
    if (entries.length === 0) {
      return 1;
    }
    return Math.max(...entries.map(([_, count]) => Number(count) || 0), 1);
  }, [entries]);

  return (
    <div className="rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
      <div className="mt-4 space-y-4">
        {entries.length === 0 ? (
          <p className="text-sm text-gray-500">No data available.</p>
        ) : (
          entries.map(([key, value]) => {
            const numericValue = Number(value) || 0;
            const width = `${Math.round((numericValue / maxValue) * 100)}%`;
            const barColor = colorsMap[key] || 'bg-gray-400';

            return (
              <div key={key}>
                <div className="mb-1 flex items-center justify-between text-sm">
                  <span className="font-medium text-gray-700">{formatLabel(key)}</span>
                  <span className="text-gray-600">{numericValue}</span>
                </div>
                <div className="h-2 w-full rounded-full bg-gray-100">
                  <div
                    className={`h-2 rounded-full ${barColor}`}
                    style={{ width }}
                  />
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

function AdminDashboard({ user, onLogout, onNavigate, isDarkMode, onToggleTheme }) {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const currentPage = 'dashboard';

  const loadDashboardStats = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await issueService.getDashboardStats(user?.id);
      setStats(data);
    } catch (err) {
      console.error('Error loading dashboard stats:', err);
      setError(err.message || 'Failed to load dashboard stats');
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    loadDashboardStats();
  }, [loadDashboardStats]);

  const metricCards = [
    {
      title: 'Total Issues',
      value: stats?.totalIssues ?? 0,
      icon: FolderOpen,
      accentClass: 'bg-blue-600'
    },
    {
      title: 'Open Issues',
      value: stats?.openIssues ?? 0,
      icon: Activity,
      accentClass: 'bg-amber-500'
    },
    {
      title: 'Resolved',
      value: stats?.resolvedIssues ?? 0,
      icon: CheckCircle2,
      accentClass: 'bg-emerald-600'
    },
    {
      title: 'Closed',
      value: stats?.closedIssues ?? 0,
      icon: CircleSlash,
      accentClass: 'bg-slate-600'
    },
    {
      title: 'Avg Resolution Time',
      value: formatAverageResolutionTime(stats?.avgResolutionTimeHours),
      icon: Clock3,
      accentClass: 'bg-indigo-600'
    }
  ];

  return (
    <div className="flex h-screen bg-gray-50">
      <Sidebar currentPage={currentPage} onNavigate={onNavigate} userRole={user?.role} />

      <main className="flex-1 overflow-auto">
        <header className="border-b border-gray-200 bg-white px-8 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
              <p className="text-sm text-gray-500">Live issue analytics overview</p>
            </div>
            <div className="flex items-center gap-3">
              <ThemeToggle isDarkMode={isDarkMode} onToggle={onToggleTheme} />
              <span className="hidden text-sm text-gray-600 md:inline">{user?.username || user?.email}</span>
              <button
                onClick={onLogout}
                className="rounded-lg border border-gray-300 px-3 py-1.5 text-sm text-gray-600 transition-colors hover:bg-gray-50 hover:text-gray-900"
              >
                Logout
              </button>
            </div>
          </div>
        </header>

        <div className="space-y-6 p-8">
          {loading ? (
            <div className="flex items-center justify-center py-24">
              <div className="text-center">
                <div className="mx-auto mb-4 h-12 w-12 animate-spin rounded-full border-b-2 border-blue-600" />
                <p className="text-gray-600">Loading dashboard...</p>
              </div>
            </div>
          ) : error ? (
            <div className="rounded-xl border border-red-200 bg-red-50 p-6">
              <p className="font-medium text-red-700">Error loading dashboard data</p>
              <p className="mt-1 text-sm text-red-600">{error}</p>
              <button
                onClick={loadDashboardStats}
                className="mt-4 rounded-lg bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
              >
                Retry
              </button>
            </div>
          ) : (
            <>
              <section className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">
                {metricCards.map((card) => (
                  <MetricCard
                    key={card.title}
                    title={card.title}
                    value={card.value}
                    icon={card.icon}
                    accentClass={card.accentClass}
                  />
                ))}
              </section>

              <section className="grid grid-cols-1 gap-6 xl:grid-cols-2">
                <DistributionList
                  title="Status Distribution"
                  data={stats?.issuesByStatus}
                  colorsMap={STATUS_COLORS}
                />
                <DistributionList
                  title="Priority Distribution"
                  data={stats?.issuesByPriority}
                  colorsMap={PRIORITY_COLORS}
                />
              </section>

              <section>
                <DistributionList
                  title="Open Issues by Assignee"
                  data={stats?.issuesAssignedPerUser}
                  colorsMap={{}}
                />
              </section>
            </>
          )}
        </div>
      </main>
    </div>
  );
}

export default AdminDashboard;

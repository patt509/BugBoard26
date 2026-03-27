import { useState } from 'react';
import { Bug, LayoutDashboard, ListTodo, Menu, UserPlus, X } from 'lucide-react';

function Sidebar({ currentPage, onNavigate, userRole }) {
  const [isOpen, setIsOpen] = useState(true);

  const menuItems = [
    ...(userRole === 'ADMIN'
      ? [
          { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
          { id: 'create-user', label: 'Create User', icon: UserPlus },
        ]
      : []),
    { id: 'issues', label: 'Issues', icon: ListTodo }
  ];

  const handleNavigate = (pageId) => {
    if (onNavigate) {
      onNavigate(pageId);
    }
  };

  return (
    <>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="fixed left-4 top-4 z-50 rounded-md bg-white p-2 shadow-md lg:hidden"
      >
        {isOpen ? <X size={24} /> : <Menu size={24} />}
      </button>

      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 transform border-r border-gray-200 bg-gray-50 transition-transform duration-200 ease-in-out lg:static ${
          isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
        }`}
      >
        <div className="flex h-full flex-col">
          <div className="flex items-center gap-2 border-b border-gray-200 px-6 py-5">
            <Bug className="h-8 w-8 text-blue-600" />
            <h1 className="text-xl font-bold text-gray-900">BugBoard</h1>
          </div>

          <nav className="flex-1 px-3 py-4">
            <ul className="space-y-1">
              {menuItems.map((item) => (
                <li key={item.id}>
                  <button
                    onClick={() => handleNavigate(item.id)}
                    className={`flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                      currentPage === item.id
                        ? 'bg-gray-200 text-gray-900'
                        : 'text-gray-700 hover:bg-gray-100'
                    }`}
                  >
                    <item.icon className="h-5 w-5" />
                    <span>{item.label}</span>
                  </button>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </aside>

      {isOpen && (
        <div
          className="fixed inset-0 z-30 bg-black bg-opacity-50 lg:hidden"
          onClick={() => setIsOpen(false)}
        />
      )}
    </>
  );
}

export default Sidebar;

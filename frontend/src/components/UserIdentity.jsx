const DICEBEAR_BASE_URL = 'https://api.dicebear.com/9.x/adventurer/svg';

const getUserDisplayName = (user) => user?.username || user?.email || 'User';

const getUserAvatarSeed = (user) => {
  const displayName = getUserDisplayName(user);
  return `${displayName}-${user?.id ?? 'guest'}`;
};

const getDiceBearAvatarUrl = (user) => {
  const seed = encodeURIComponent(getUserAvatarSeed(user));
  return `${DICEBEAR_BASE_URL}?seed=${seed}&radius=50&size=64`;
};

function UserIdentity({ user, className = '' }) {
  const displayName = getUserDisplayName(user);
  const avatarUrl = getDiceBearAvatarUrl(user);

  return (
    <div className={`inline-flex items-center gap-2 ${className}`}>
      <div className="user-avatar-glass">
        <img
          src={avatarUrl}
          alt={`${displayName} profile avatar`}
          className="h-full w-full rounded-full object-cover"
          loading="lazy"
          referrerPolicy="no-referrer"
        />
      </div>
      <span className="hidden text-sm text-gray-600 md:inline">{displayName}</span>
    </div>
  );
}

export default UserIdentity;

import { ChevronDown, LogOut, User, UserRoundCog } from 'lucide-react';
import { useState } from 'react';
import type { UserProfile } from '../../types';

type ProfileMenuProps = {
  profile: UserProfile;
  isLoading: boolean;
  error: string | null;
  onOpenMyPage: () => void;
  onLogout: () => void;
};

export function ProfileMenu({
  profile,
  isLoading,
  error,
  onOpenMyPage,
  onLogout,
}: ProfileMenuProps) {
  const [isOpen, setIsOpen] = useState(false);
  const displayName = isLoading ? '불러오는 중' : profile.username;
  const displaySubtext = error ? '프로필 확인 필요' : profile.name;

  const handleSelect = (handler: () => void) => {
    setIsOpen(false);
    handler();
  };

  return (
    <div className="profile-menu">
      <button
        className="profile-button"
        type="button"
        aria-label={`${profile.username} 프로필 메뉴`}
        aria-expanded={isOpen}
        onClick={() => setIsOpen((current) => !current)}
      >
        <User size={22} aria-hidden="true" />
        <span>
          {displayName}
          <small>{displaySubtext}</small>
        </span>
        <ChevronDown size={17} aria-hidden="true" />
      </button>

      {isOpen ? (
        <div className="profile-dropdown" role="menu">
          <div className="profile-dropdown-summary">
            <strong>{profile.name}</strong>
            <span>{profile.username}</span>
          </div>
          <button type="button" role="menuitem" onClick={() => handleSelect(onOpenMyPage)}>
            <UserRoundCog size={17} aria-hidden="true" />
            마이페이지
          </button>
          <button type="button" role="menuitem" onClick={() => handleSelect(onLogout)}>
            <LogOut size={17} aria-hidden="true" />
            로그아웃
          </button>
        </div>
      ) : null}
    </div>
  );
}

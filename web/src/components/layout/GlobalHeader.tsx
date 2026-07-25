import { Bell, Clock3, Moon, Sun } from 'lucide-react';
import { ProfileMenu } from '../account/ProfileMenu';
import type { DashboardSnapshot, ThemeMode, UserProfile } from '../../types';

type GlobalHeaderProps = {
  snapshot: DashboardSnapshot;
  profile: UserProfile;
  isProfileLoading: boolean;
  profileError: string | null;
  isLoading: boolean;
  error: string | null;
  isLive: boolean;
  currentTime: string;
  themeMode: ThemeMode;
  onToggleTheme: () => void;
  onGoHome: () => void;
  onOpenMyPage: () => void;
  onLogout: () => void;
};

export function GlobalHeader({
  snapshot,
  profile,
  isProfileLoading,
  profileError,
  isLoading,
  error,
  isLive,
  currentTime,
  themeMode,
  onToggleTheme,
  onGoHome,
  onOpenMyPage,
  onLogout,
}: GlobalHeaderProps) {
  const syncLabel = getSyncLabel(isLoading, error, isLive);
  const ThemeIcon = themeMode === 'dark' ? Sun : Moon;
  const nextThemeLabel = themeMode === 'dark' ? '화이트 테마로 전환' : '블랙 테마로 전환';

  return (
    <header className="global-header">
      <button className="brand-block" type="button" aria-label="대시보드 홈으로 이동" onClick={onGoHome}>
        <span className="brand-copy">
          <strong>통합 관제 센터</strong>
          <span>Smart Factory Monitoring</span>
        </span>
      </button>
      <div className="header-actions">
        <span className="system-pill" data-sync={syncLabel?.tone}>
          <i aria-hidden="true" />
          시스템 상태 <b>{snapshot.systemStatus}</b>
        </span>
        {syncLabel ? (
          <span className="sync-pill" aria-live="polite">
            {syncLabel.text}
          </span>
        ) : null}
        <time className="time-pill">
          <Clock3 size={18} aria-hidden="true" />
          {currentTime}
        </time>
        <button className="theme-button" type="button" aria-label={nextThemeLabel} onClick={onToggleTheme}>
          <ThemeIcon size={20} aria-hidden="true" />
        </button>
        <button className="bell-button" type="button" aria-label="알림">
          <Bell size={22} aria-hidden="true" />
        </button>
        <ProfileMenu
          profile={profile}
          isLoading={isProfileLoading}
          error={profileError}
          onOpenMyPage={onOpenMyPage}
          onLogout={onLogout}
        />
      </div>
    </header>
  );
}

function getSyncLabel(isLoading: boolean, error: string | null, isLive: boolean) {
  if (isLoading) {
    return { text: '대시보드 데이터 동기화 중…', tone: 'loading' };
  }

  if (error) {
    return null;
  }

  if (isLive) {
    return { text: '실시간 데이터 수신', tone: 'live' };
  }

  return null;
}

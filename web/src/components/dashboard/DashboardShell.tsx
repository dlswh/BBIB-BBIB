import { MyPagePanel } from '../account/MyPagePanel';
import { NameEditPage } from '../account/NameEditPage';
import { KpiGrid } from './KpiGrid';
import { MachineStatusPanel } from './MachineStatusPanel';
import { ConnectionPanel } from './ConnectionPanel';
import { LogStreamPanel } from './LogStreamPanel';
import { DistributionPanel } from './DistributionPanel';
import { MachineStatusPage } from './MachineStatusPage';
import { GlobalHeader } from '../layout/GlobalHeader';
import { Sidebar, type SidebarNavKey } from '../layout/Sidebar';
import { LogPage } from '../ipc-logs/LogPage';
import { IpcFleetSettingsPage } from '../settings/IpcFleetSettingsPage';
import { IpcErrorDialog } from './IpcErrorDialog'; // ✅ 추가
import { useComputerTime } from '../../hooks/useComputerTime';
import { useDashboardSnapshot } from '../../hooks/useDashboardSnapshot';
import { useIpcFleetSettings } from '../../hooks/useIpcFleetSettings';
import { useIpcStateStream } from '../../hooks/useIpcStateStream';
import { useIpcStream } from '../../hooks/useIpcStream';
import { useMachineStatusKpis } from '../../hooks/useMachineStatusKpis';
import { useNow } from '../../hooks/useNow';
import { useUserProfile } from '../../hooks/useUserProfile';
import type { AuthSession, ThemeMode } from '../../types';
import { useSessionSnapshot } from '../../hooks/useSessionSnapshot';
import { WorkerPage } from '../admin/workerPage';
import { useState, useEffect } from 'react';

type AccountView = 'myPage' | 'nameEdit' | null;
type DashboardView = 'overview' | 'machines' | 'logs' | 'settings' | 'operators';

type DashboardShellProps = {
  session: AuthSession;
  themeMode: ThemeMode;
  onToggleTheme: () => void;
  onLogout: () => void;
  onProfileNameChange: (name: string) => void;
};

export function DashboardShell({
  session,
  themeMode,
  onToggleTheme,
  onLogout,
  onProfileNameChange,
}: DashboardShellProps) {
  const { snapshot, isLoading, error, isLive } = useDashboardSnapshot();
  // ✅ errorAlert, dismissErrorAlert 추가
  const { logs, paused, setPaused, errorAlert, dismissErrorAlert } = useIpcStream();
  const ipcStateStream = useIpcStateStream();
  const { machines: ipcMachines } = ipcStateStream;
  const fleetSettings = useIpcFleetSettings();
  const now = useNow();
  const machineStatusMetrics = useMachineStatusKpis(ipcMachines, fleetSettings.setting, now);
  const userProfile = useUserProfile(session);
  const computerTime = useComputerTime();
  const [accountView, setAccountView] = useState<AccountView>(null);
  const [dashboardView, setDashboardView] = useState<DashboardView>(getInitialDashboardView);
  const totalDevices = String(machineStatusMetrics.summary.totalCount);
  const { snapshot: connectionSnapshot } = useSessionSnapshot();

  useEffect(() => {
    const handlePopState = () => {
      setDashboardView(getInitialDashboardView());
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigateDashboardView = (view: DashboardView) => {
    const nextPath = getDashboardPath(view);
    if (window.location.pathname !== nextPath) {
      window.history.pushState({}, '', nextPath);
    }
    setDashboardView(view);
  };

  const handleSidebarSelect = (item: SidebarNavKey) => {
    if (item === 'machines') return navigateDashboardView('machines');
    if (item === 'logs') return navigateDashboardView('logs');
    if (item === 'settings') return navigateDashboardView('settings');
    if (item === 'operators') return navigateDashboardView('operators');
    navigateDashboardView('overview');
  };

  const handleNameSubmit = async (name: string) => {
    const nextProfile = await userProfile.updateName(name);
    onProfileNameChange(nextProfile.name);
    setAccountView('myPage');
  };

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        본문으로 건너뛰기
      </a>
      <GlobalHeader
        snapshot={snapshot}
        profile={userProfile.profile}
        isProfileLoading={userProfile.isLoading}
        profileError={userProfile.error}
        isLoading={isLoading}
        error={error}
        isLive={isLive}
        currentTime={computerTime}
        themeMode={themeMode}
        onToggleTheme={onToggleTheme}
        onGoHome={() => navigateDashboardView('overview')}
        onOpenMyPage={() => setAccountView('myPage')}
        onLogout={onLogout}
      />
      <Sidebar
        activeItem={getActiveSidebarItem(dashboardView)}
        onSelect={handleSidebarSelect}
      />

      <main className="workspace" id="main-content">
        {dashboardView === 'logs' ? (
          <LogPage />
        ) : null}

        {dashboardView === 'machines' ? (
          <MachineStatusPage
            machines={machineStatusMetrics.machines}
            summary={machineStatusMetrics.summary}
            connected={ipcStateStream.connected}
            error={ipcStateStream.error}
            events={ipcStateStream.events}
            stateCount={ipcStateStream.stateCount}
          />
        ) : null}

        {dashboardView === 'settings' ? (
          <IpcFleetSettingsPage
            setting={fleetSettings.setting}
            isLoading={fleetSettings.isLoading}
            isSaving={fleetSettings.isSaving}
            error={fleetSettings.error}
            onSave={fleetSettings.saveSetting}
          />
        ) : null}

        {dashboardView === 'operators' ? (
          <WorkerPage onBack={() => navigateDashboardView('overview')} />
        ) : null}

        {dashboardView === 'overview' ? (
          <div className="dashboard-overview">
            <KpiGrid kpis={machineStatusMetrics.kpis} />
            <section className="dashboard-grid">
              <MachineStatusPanel machines={machineStatusMetrics.machines} summary={machineStatusMetrics.summary} />
              <ConnectionPanel connection={connectionSnapshot} />
              <LogStreamPanel
                logs={logs}
                paused={paused}
                onTogglePause={() => setPaused((prev) => !prev)}
                onMoreClick={() => navigateDashboardView('logs')}
              />
              <DistributionPanel
                distribution={machineStatusMetrics.distribution}
                totalDevices={totalDevices}
                onViewAll={() => navigateDashboardView('machines')}
              />
            </section>
          </div>
        ) : null}
      </main>

      {accountView === 'myPage' ? (
        <MyPagePanel
          profile={userProfile.profile}
          isLoading={userProfile.isLoading}
          error={userProfile.error}
          onEditName={() => setAccountView('nameEdit')}
          onClose={() => setAccountView(null)}
        />
      ) : null}

      {accountView === 'nameEdit' ? (
        <NameEditPage
          profile={userProfile.profile}
          isUpdating={userProfile.isUpdating}
          serverError={userProfile.error}
          onSubmit={handleNameSubmit}
          onClose={() => setAccountView(null)}
        />
      ) : null}

      {/* ✅ IPC 에러 다이얼로그 — 어떤 뷰에서든 표시 */}
      {errorAlert ? (
        <IpcErrorDialog
          ipcId={errorAlert.ipcId}
          occurredAt={errorAlert.occurredAt}
          data={errorAlert.data}
          onClose={dismissErrorAlert}
        />
      ) : null}
    </div>
  );
}

function getInitialDashboardView(): DashboardView {
  if (window.location.pathname === '/dashboard/logs') return 'logs';
  if (window.location.pathname === '/dashboard/machines') return 'machines';
  if (window.location.pathname === '/dashboard/settings') return 'settings';
  if (window.location.pathname === '/dashboard/operators') return 'operators';
  return 'overview';
}

function getDashboardPath(view: DashboardView) {
  switch (view) {
    case 'machines': return '/dashboard/machines';
    case 'logs': return '/dashboard/logs';
    case 'settings': return '/dashboard/settings';
    case 'operators': return '/dashboard/operators';
    default: return '/dashboard';
  }
}

function getActiveSidebarItem(view: DashboardView): SidebarNavKey {
  switch (view) {
    case 'machines': return 'machines';
    case 'logs': return 'logs';
    case 'settings': return 'settings';
    case 'operators': return 'operators';
    default: return 'overview';
  }
}
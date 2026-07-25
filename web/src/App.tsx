import { useEffect } from 'react';
import { AdminLoginPage } from './components/auth/AdminLoginPage';
import { DashboardShell } from './components/dashboard/DashboardShell';
import { LandingPage } from './components/landing/LandingPage';
import { useAppRoute } from './hooks/useAppRoute';
import { useAuthSession } from './hooks/useAuthSession';
import { useThemeMode } from './hooks/useThemeMode';

function App() {
  return <RoutedApp />;
}

function RoutedApp() {
  const auth = useAuthSession();
  const { themeMode, toggleThemeMode } = useThemeMode();
  const { view, navigate } = useAppRoute();

  useEffect(() => {
    if (auth.isAdmin && auth.session && view !== 'dashboard') {
      navigate('dashboard', 'replace');
      return;
    }

    if (!auth.isAdmin && view === 'dashboard') {
      navigate('login', 'replace');
    }
  }, [auth.isAdmin, auth.session, navigate, view]);

  if (auth.isAdmin && auth.session) {
    if (view !== 'dashboard') {
      return null;
    }

    return (
      <DashboardShell
        session={auth.session}
        themeMode={themeMode}
        onToggleTheme={toggleThemeMode}
        onProfileNameChange={auth.updateSessionName}
        onLogout={async () => {
          await auth.logout();
          navigate('landing', 'replace');
        }}
      />
    );
  }

  if (view === 'login' || view === 'dashboard') {
    if (view === 'dashboard') {
      return null;
    }

    return (
      <AdminLoginPage
        themeMode={themeMode}
        isSubmitting={auth.isSubmitting}
        error={auth.error}
        onToggleTheme={toggleThemeMode}
        onBack={() => navigate('landing')}
        onLogin={auth.login}
        onLoginSuccess={() => navigate('dashboard')}
      />
    );
  }

  return (
    <LandingPage
      themeMode={themeMode}
      onToggleTheme={toggleThemeMode}
      onLoginClick={() => navigate('login')}
    />
  );
}

export default App;

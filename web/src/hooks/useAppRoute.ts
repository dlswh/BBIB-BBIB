import { useCallback, useEffect, useState } from 'react';
import type { AppView } from '../types';

type NavigationMode = 'push' | 'replace';

const viewPaths: Record<AppView, string> = {
  landing: '/',
  login: '/login',
  dashboard: '/dashboard',
};

export function useAppRoute() {
  const [view, setView] = useState<AppView>(readCurrentView);

  useEffect(() => {
    const handlePopState = () => setView(readCurrentView());

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const navigate = useCallback((nextView: AppView, mode: NavigationMode = 'push') => {
    const nextPath = viewPaths[nextView];

    if (window.location.pathname !== nextPath) {
      const method = mode === 'replace' ? 'replaceState' : 'pushState';
      window.history[method]({}, '', nextPath);
    }

    setView(nextView);
  }, []);

  return { view, navigate };
}

function readCurrentView(): AppView {
  if (window.location.pathname === viewPaths.login) {
    return 'login';
  }

  if (window.location.pathname.startsWith(viewPaths.dashboard)) {
    return 'dashboard';
  }

  return 'landing';
}

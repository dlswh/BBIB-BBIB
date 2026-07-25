import { useLayoutEffect, useState } from 'react';
import type { ThemeMode } from '../types';

const THEME_STORAGE_KEY = 'smart-factory-dashboard-theme';

export function useThemeMode() {
  const [themeMode, setThemeMode] = useState<ThemeMode>(getInitialThemeMode);

  useLayoutEffect(() => {
    document.documentElement.dataset.theme = themeMode;
  }, [themeMode]);

  const toggleThemeMode = () => {
    setThemeMode((current) => {
      const nextTheme = current === 'dark' ? 'light' : 'dark';
      localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
      return nextTheme;
    });
  };

  return { themeMode, toggleThemeMode };
}

function getInitialThemeMode(): ThemeMode {
  const storedTheme = localStorage.getItem(THEME_STORAGE_KEY);

  if (storedTheme === 'dark' || storedTheme === 'light') {
    return storedTheme;
  }

  if (window.matchMedia('(prefers-color-scheme: light)').matches) {
    return 'light';
  }

  return 'dark';
}

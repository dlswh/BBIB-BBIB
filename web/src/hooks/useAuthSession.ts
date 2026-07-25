import { useState, useEffect  } from 'react';
import { loginAdmin, logoutAdmin } from '../api/auth';
import { authEvents } from '../api/config';
import type { AuthSession, LoginRequest } from '../types';

const AUTH_STORAGE_KEY = 'smartFactory.auth';

export function useAuthSession() {
  const [session, setSession] = useState<AuthSession | null>(readStoredSession);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const handleForceLogout = () => {
      localStorage.removeItem(AUTH_STORAGE_KEY);
      setSession(null);
      setError('다른 기기에서 로그인되어 자동 로그아웃되었습니다.');
    };

    authEvents.addEventListener('logout', handleForceLogout);
    return () => authEvents.removeEventListener('logout', handleForceLogout);
  }, []);

  const login = async (credentials: LoginRequest) => {
    setIsSubmitting(true);
    setError(null);

    try {
      const response = await loginAdmin(credentials);

      if (response.role !== 'ADMIN') {
        setError('관리자 계정만 관제센터에 접근할 수 있습니다.');
        return false;
      }

      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(response));
      setSession(response);
      return true;
    } catch (loginError) {
      setError(loginError instanceof Error ? loginError.message : '로그인에 실패했습니다.');
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  const logout = async () => {
    const accessToken = session?.accessToken;
    localStorage.removeItem(AUTH_STORAGE_KEY);
    setSession(null);
    setError(null);

    try {
      await logoutAdmin(accessToken);
    } catch {
      // 로컬 세션 정리가 우선이므로 서버 로그아웃 실패는 화면 흐름을 막지 않는다.
    }
  };

  const updateSessionName = (name: string) => {
    setSession((current) => {
      if (!current) return current;

      const nextSession = { ...current, name };
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(nextSession));
      return nextSession;
    });
  };

  return {
    session,
    isAdmin: session?.role === 'ADMIN',
    isSubmitting,
    error,
    login,
    logout,
    updateSessionName,
  };
}

function readStoredSession(): AuthSession | null {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!stored) return null;

  try {
    const parsed = JSON.parse(stored) as Partial<AuthSession>;

    if (
      typeof parsed.accessToken === 'string' &&
      typeof parsed.refreshToken === 'string' &&
      typeof parsed.username === 'string' &&
      typeof parsed.name === 'string' &&
      typeof parsed.role === 'string'
    ) {
      return parsed as AuthSession;
    }
  } catch {
    localStorage.removeItem(AUTH_STORAGE_KEY);
  }

  return null;
}

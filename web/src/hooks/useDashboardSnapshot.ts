import { useEffect, useState } from 'react';
import { getDashboardSnapshot, subscribeDashboardSnapshot } from '../api/dashboard';
import { mockDashboardSnapshot } from '../data/mockDashboard';
import type { DashboardSnapshot } from '../types';

export function useDashboardSnapshot() {
  const [snapshot, setSnapshot] = useState<DashboardSnapshot>({ ...mockDashboardSnapshot, logs: [] });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isLive, setIsLive] = useState(false);

  useEffect(() => {
    let ignore = false;

    getDashboardSnapshot()
      .then((nextSnapshot) => {
        if (!ignore) {
          setSnapshot(nextSnapshot);
          setError(null);
        }
      })
      .catch((error) => {
        if (!ignore) {
          setError('대시보드 API 연결 실패');
        }
        console.warn(error);
      })
      .finally(() => {
        if (!ignore) {
          setIsLoading(false);
        }
      });

    const unsubscribe = subscribeDashboardSnapshot(
      (nextSnapshot) => {
        setSnapshot(nextSnapshot);
        setError(null);
        setIsLive(true);
      },
      (nextError) => {
        setIsLive(false);
        setError('실시간 스트림 연결 점검 필요');
        console.warn(nextError);
      },
    );

    return () => {
      ignore = true;
      unsubscribe();
    };
  }, []);

  return { snapshot, isLoading, error, isLive };
}

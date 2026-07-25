import { mockDashboardSnapshot } from '../data/mockDashboard';
import type { DashboardSnapshot } from '../types';
import { buildApiUrl, fetchWithAuth, useMockDashboard } from './config';

async function fetchJson<T>(path: string): Promise<T> {
  if (useMockDashboard) {
    return mockDashboardSnapshot as T;
  }
  const response = await fetchWithAuth(path, {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`API 요청 실패: ${response.status} ${response.statusText}`);
  }
  return response.json() as Promise<T>;
}

export function getDashboardSnapshot() {
  return fetchJson<DashboardSnapshot>('/api/monitoring/dashboard');
}

export function subscribeDashboardSnapshot(
  onMessage: (snapshot: DashboardSnapshot) => void,
  onError?: (error: Error) => void,
) {
  if (useMockDashboard || typeof EventSource === 'undefined') {
    return () => undefined;
  }

  const source = new EventSource(buildApiUrl('/api/monitoring/dashboard/stream'));
  source.onmessage = (event) => {
    try {
      onMessage(JSON.parse(event.data) as DashboardSnapshot);
    } catch (error) {
      onError?.(error instanceof Error ? error : new Error('SSE 메시지 파싱 실패'));
    }
  };
  source.onerror = () => {
    onError?.(new Error('실시간 대시보드 스트림 연결 실패'));
  };

  return () => source.close();
}
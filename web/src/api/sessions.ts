import type { SessionEvent } from '../types';
import { fetchWithAuth } from './config';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '');

function getToken(): string {
  const stored = localStorage.getItem('smartFactory.auth');
  if (!stored) return '';
  try {
    return JSON.parse(stored).accessToken ?? '';
  } catch {
    return '';
  }
}

function authHeaders() {
  return {
    Accept: 'application/json',
    Authorization: `Bearer ${getToken()}`,
  };
}

export async function getSessions(): Promise<SessionEvent[]> {
  const response = await fetchWithAuth('/api/sessions', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`세션 조회 실패: ${response.status}`);
  return response.json();
}

export async function getTodayCount(): Promise<number> {
  const response = await fetchWithAuth('/api/sessions/today-count', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`금일 접속자 조회 실패: ${response.status}`);
  return response.json();
}

export async function getTotalWorkers(): Promise<number> {
  const response = await fetchWithAuth('/api/sessions/total-workers', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`전체 작업자 조회 실패: ${response.status}`);
  return response.json();
}
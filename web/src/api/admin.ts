import type { WorkerUser } from '../types';
import { fetchWithAuth } from './config';


export async function getWorkers(): Promise<WorkerUser[]> {
  const response = await fetchWithAuth('/api/admin/users', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) throw new Error(`작업자 목록 조회 실패: ${response.status}`);
  return response.json();
}

export async function createWorker(username: string, password: string, name: string): Promise<void> {
  const response = await fetchWithAuth('/api/admin/users', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ username, password, name, role: 'WORKER' }),
  });
  if (!response.ok) throw new Error(`계정 생성 실패: ${response.status}`);
}

export async function disableWorker(id: number): Promise<void> {
  const response = await fetchWithAuth(`/api/admin/users/${id}`, {
    method: 'DELETE',
  });
  if (!response.ok) throw new Error(`계정 비활성화 실패: ${response.status}`);
}

export async function enableWorker(id: number): Promise<void> {
  const response = await fetchWithAuth(`/api/admin/users/${id}/enable`, {
    method: 'PATCH',
  });
  if (!response.ok) throw new Error(`계정 활성화 실패: ${response.status}`);
}
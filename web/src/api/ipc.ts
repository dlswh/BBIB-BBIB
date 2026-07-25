import type { LogEvent } from '../types';
import { fetchWithAuth } from './config';

export type IpcLogSearchParams = {
  from?: string;
  to?: string;
  type?: string[];
  ipcId?: string;
  page?: number;
  size?: number;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  last: boolean;
};

export async function getIpcLogPage(params: IpcLogSearchParams = {}): Promise<Page<LogEvent>> {
  const query = new URLSearchParams();
  if (params.from)  query.set('from', params.from);
  if (params.to)    query.set('to', params.to);
  if (params.ipcId) query.set('ipcId', params.ipcId);
  if (params.type?.length) params.type.forEach((t) => query.append('type', t));
  if (params.page !== undefined) query.set('page', String(params.page));
  if (params.size !== undefined) query.set('size', String(params.size));

  const response = await fetchWithAuth(`/api/ipc/logs?${query.toString()}`, {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) throw new Error(`로그 조회 실패: ${response.status} ${response.statusText}`);
  return response.json() as Promise<Page<LogEvent>>;
}

export async function getIpcIds(): Promise<string[]> {
  const response = await fetchWithAuth('/api/ipc/logs/ipc-ids', {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) throw new Error(`IPC 목록 조회 실패: ${response.status} ${response.statusText}`);
  return response.json() as Promise<string[]>;
}
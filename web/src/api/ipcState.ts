import type { IpcStateEvent } from '../types';
import { buildApiUrl, fetchWithAuth } from './config';

export async function getIpcStateSnapshot() {
  const response = await fetchWithAuth('/api/ipc/states', {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) throw new Error(`IPC state snapshot request failed: ${response.status}`);
  return response.json() as Promise<IpcStateEvent[]>;
}

export function subscribeIpcStateStream(
  handlers: {
    onConnect?: () => void;
    onSnapshot?: (events: IpcStateEvent[]) => void;
    onState?: (event: IpcStateEvent) => void;
    onError?: () => void;
  },
) {
  const source = new EventSource(buildApiUrl('/api/ipc/events'));

  source.addEventListener('connect', () => handlers.onConnect?.());
  source.addEventListener('snapshot', (event) => {
    handlers.onSnapshot?.(JSON.parse(event.data) as IpcStateEvent[]);
  });
  source.addEventListener('state', (event) => {
    handlers.onState?.(JSON.parse(event.data) as IpcStateEvent);
  });
  source.onerror = () => handlers.onError?.();

  return () => source.close();
}
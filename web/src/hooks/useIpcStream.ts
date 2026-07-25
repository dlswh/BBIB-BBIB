import { useEffect, useRef, useState } from 'react';
import type { LogEvent, IpcErrorData } from '../types';
import { buildApiUrl } from '../api/config';
import { getIpcLogPage } from '../api/ipc';
import { getStoredAccessToken } from '../api/config';

const AUTH_STORAGE_KEY = 'smartFactory.auth';

export type IpcErrorAlert = {
  ipcId: string;
  occurredAt: string;
  data: IpcErrorData;
};

export function useIpcStream() {
  const [logs, setLogs] = useState<LogEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const [paused, setPaused] = useState(false);
  const [errorAlert, setErrorAlert] = useState<IpcErrorAlert | null>(null);

  const bufferRef = useRef<LogEvent[]>([]);
  const pausedRef = useRef(false);

  useEffect(() => {
    pausedRef.current = paused;
  }, [paused]);

  useEffect(() => {
    if (paused) return;
    if (bufferRef.current.length === 0) return;

    const buffered = bufferRef.current;
    bufferRef.current = [];
    setLogs((prev) => [...buffered, ...prev].slice(0, 500));
  }, [paused]);

  // SSE 연결
  useEffect(() => {
    const accessToken = getStoredAccessToken();
    const es = new EventSource(buildApiUrl(`/api/ipc/events?token=${accessToken}`));

    es.addEventListener('connect', () => setConnected(true));

    (['response', 'result', 'error'] as const).forEach((type) => {
      es.addEventListener(type, (e: MessageEvent) => {
        const log: LogEvent = JSON.parse(e.data);

        // ERROR 타입이면 다이얼로그 표시 (최신 1개로 교체)
        if (log.type === 'ERROR') {
          setErrorAlert({
            ipcId: log.ipcId,
            occurredAt: log.occurredAt,
            data: log.data as IpcErrorData,
          });
        }

        if (pausedRef.current) {
          bufferRef.current.unshift(log);
          if (bufferRef.current.length > 500) {
            bufferRef.current.length = 500;
          }
        } else {
          setLogs((prev) => [log, ...prev].slice(0, 500));
        }
      });
    });

    es.onerror = () => setConnected(false);

    return () => es.close();
  }, []);

  // SSE 연결 확인 후 초기 로그 조회
  useEffect(() => {
    if (!connected) return;
    getIpcLogPage({ size: 50, type: ['RESPONSE', 'RESULT', 'ERROR'] })
      .then((data) => setLogs(data.content.filter((log) => log.type !== 'STATE')))
      .catch(() => {});
  }, [connected]);

  const dismissErrorAlert = () => setErrorAlert(null);

  return { logs, connected, paused, setPaused, errorAlert, dismissErrorAlert };
}
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useEffect, useRef, useState } from 'react';
import { getSessions, getTodayCount, getTotalWorkers } from '../api/sessions';
import type { ConnectionSnapshot, SessionEvent } from '../types';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '';

function toSnapshot(
  sessions: SessionEvent[],
  todayCount: number,
  totalWorkers: number,
): ConnectionSnapshot {
  return {
    activeOperators: sessions.filter((s) => s.role === 'WORKER').length,
    activeSessions: sessions.length,
    blockedDuplicateLogins: todayCount,
    currentOperators: sessions
      .sort((a, b) => new Date(b.loginAt).getTime() - new Date(a.loginAt).getTime())
      .map((s) => ({
        operatorId: s.username,
        role: s.role,
        ipAddress: s.ipAddress,
        connectedAt: new Date(s.loginAt).toLocaleTimeString('ko-KR', {
          hour: '2-digit', minute: '2-digit', second: '2-digit',
        }),
      })),
    extraOnlineCount: totalWorkers,
  };
}

export function useSessionSnapshot() {
  const [sessions, setSessions] = useState<SessionEvent[]>([]);
  const [todayCount, setTodayCount] = useState(0);
  const [totalWorkers, setTotalWorkers] = useState(0);
  const [isLive, setIsLive] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const clientRef = useRef<Client | null>(null);

  useEffect(() => {
    getSessions().then(setSessions).catch((e: Error) => setError(e.message));
    getTodayCount().then(setTodayCount).catch((e: Error) => setError(e.message));
    getTotalWorkers().then(setTotalWorkers).catch((e: Error) => setError(e.message));

    const client = new Client({
      webSocketFactory: () => new SockJS(`${apiBaseUrl}/api/ws`) as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        setIsLive(true);
        client.subscribe('/topic/sessions', (msg: IMessage) => {
          const event: SessionEvent = JSON.parse(msg.body);
          setSessions((prev) => {
            if (event.eventType === 'LOGIN') {
              return [...prev, event];
            } else {
              return prev.filter((s) => s.username !== event.username);
            }
          });
          if (event.eventType === 'LOGIN') {
            getTodayCount().then(setTodayCount).catch((e: Error) => setError(e.message));
          }
        });
      },
      onDisconnect: () => setIsLive(false),
      onStompError: () => setError('WebSocket 연결 실패'),
    });

    client.activate();
    clientRef.current = client;

    return () => { client.deactivate(); };
  }, []);

  return { snapshot: toSnapshot(sessions, todayCount, totalWorkers), isLive, error };
}
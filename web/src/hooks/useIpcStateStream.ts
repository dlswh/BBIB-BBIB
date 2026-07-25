import { useEffect, useMemo, useState } from 'react';
import { getIpcStateSnapshot, subscribeIpcStateStream } from '../api/ipcState';
import type { IpcStateEvent, Machine, MachineStatus } from '../types';

export type IpcStateStreamEvent = {
  id: string;
  ipcId: string;
  status: MachineStatus;
  position: number;
  speed: number;
  timestamp: string;
};

export function useIpcStateStream() {
  const [statesByIpcId, setStatesByIpcId] = useState<Map<string, IpcStateEvent>>(new Map());
  const [events, setEvents] = useState<IpcStateStreamEvent[]>([]);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    getIpcStateSnapshot()
      .then((snapshot) => {
        if (!ignore) {
          setStatesByIpcId(toStateMap(snapshot));
        }
      })
      .catch((reason) => {
        if (!ignore) {
          setError(reason instanceof Error ? reason.message : 'Failed to load IPC state snapshot');
        }
      });

    const unsubscribe = subscribeIpcStateStream({
      onConnect: () => {
        setConnected(true);
        setError(null);
      },
      onSnapshot: (snapshot) => {
        setStatesByIpcId(toStateMap(snapshot));
      },
      onState: (state) => {
        setStatesByIpcId((prev) => {
          const next = new Map(prev);
          next.set(state.ipcId, state);
          return next;
        });
        setEvents((prev) => [toStreamEvent(state), ...prev].slice(0, 30));
      },
      onError: () => {
        setConnected(false);
        setError('IPC state SSE connection lost');
      },
    });

    return () => {
      ignore = true;
      unsubscribe();
    };
  }, []);

  const machines = useMemo<Machine[]>(() => {
    return Array.from(statesByIpcId.values())
      .sort((a, b) => a.ipcId.localeCompare(b.ipcId))
      .map((state) => ({
        id: state.ipcId,
        status: mapMachineStatus(state.payload.data.status),
        position: `${state.payload.data.position.toFixed(1)} cm`,
        speed: `${state.payload.data.speed.toFixed(1)} cm/s`,
        lastSeenAt: state.payload.timestamp,
      }));
  }, [statesByIpcId]);

  return {
    connected,
    error,
    events,
    machines,
    stateCount: statesByIpcId.size,
  };
}

function toStateMap(events: IpcStateEvent[]) {
  return new Map(events.map((event) => [event.ipcId, event]));
}

function toStreamEvent(event: IpcStateEvent): IpcStateStreamEvent {
  return {
    id: event.payload.id,
    ipcId: event.ipcId,
    status: mapMachineStatus(event.payload.data.status),
    position: event.payload.data.position,
    speed: event.payload.data.speed,
    timestamp: event.payload.timestamp,
  };
}

function mapMachineStatus(status: IpcStateEvent['payload']['data']['status']): MachineStatus {
  switch (status) {
    case 'IDLE':
      return 'READY';
    case 'E_STOPPED':
      return 'ESTOPPED';
    default:
      return status;
  }
}

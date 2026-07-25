import { useCallback, useEffect, useRef, useState } from 'react';
import type { LogEvent } from '../types';
import { getIpcLogPage, getIpcIds } from '../api/ipc';
import type { IpcLogSearchParams, Page } from '../api/ipc';
import { buildApiUrl, getStoredAccessToken } from '../api/config';

const PAGE_SIZE = 50;

export type LogFilter = {
  from?: string;
  to?: string;
  type?: string[];
  ipcId?: string;
};

function matchesFilter(log: LogEvent, filter: LogFilter): boolean {
  if (filter.ipcId && log.ipcId !== filter.ipcId) return false;
  if (filter.type?.length && !filter.type.includes(log.type)) return false;
  if (filter.from && log.occurredAt < filter.from) return false;
  if (filter.to && log.occurredAt > filter.to) return false;
  return true;
}

export function useIpcLogSearch() {
  const [logs, setLogs] = useState<LogEvent[]>([]);
  const [ipcIds, setIpcIds] = useState<string[]>([]);
  const [filter, setFilter] = useState<LogFilter>({});
  const [loading, setLoading] = useState(false);
  const [hasMore, setHasMore] = useState(true);

  const pageRef    = useRef(0);
  const filterRef  = useRef<LogFilter>({});
  const loadingRef = useRef(false);

  // IPC 드롭다운 목록 초기 로드
  useEffect(() => {
    getIpcIds().then(setIpcIds).catch(() => {});
  }, []);

  const fetchLogs = async (f: LogFilter, page: number, reset = false) => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const params: IpcLogSearchParams = { ...f, page, size: PAGE_SIZE };
      const data: Page<LogEvent> = await getIpcLogPage(params);
      setLogs((prev) => reset ? data.content : [...prev, ...data.content]);
      setHasMore(!data.last);
      pageRef.current = page + 1;
    } catch {
      // 에러 무시
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  };

  // 필터 변경 시 초기화 후 재조회
  useEffect(() => {
    filterRef.current = filter;
    pageRef.current = 0;
    setLogs([]);
    setHasMore(true);
    fetchLogs(filter, 0, true);
  }, [filter]);

  // SSE - 마운트 시 한 번만 연결, 프론트에서 필터링
  useEffect(() => {
    const accessToken = getStoredAccessToken();

    const es = new EventSource(buildApiUrl(`/api/ipc/logs/stream?token=${accessToken}`));

    (['response', 'result', 'error', 'state'] as const).forEach((type) => {
      es.addEventListener(type, (e: MessageEvent) => {
        const log: LogEvent = JSON.parse(e.data);
        if (matchesFilter(log, filterRef.current)) {
          setLogs((prev) => {
            if (prev.some((l) => l.messageId === log.messageId)) return prev;
            return [log, ...prev];
          });
        }
      });
    });

    es.onerror = () => es.close();

    return () => es.close();
  }, []);

  const loadMore = useCallback(() => {
    if (!hasMore || loadingRef.current) return;
    fetchLogs(filterRef.current, pageRef.current);
  }, [hasMore]);

  return {
    logs,
    ipcIds,
    filter,
    setFilter,
    loading,
    hasMore,
    loadMore,
  };
}
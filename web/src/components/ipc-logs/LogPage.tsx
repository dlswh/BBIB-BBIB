import { useCallback, useEffect, useRef } from 'react';
import { useIpcLogSearch } from '../../hooks/useIpcLogSearch';
import type { LogEvent, IpcResponseData, IpcResultData, IpcErrorData } from '../../types';

const TYPE_FILTERS = ['ALL', 'RESPONSE', 'RESULT', 'ERROR', 'STATE'] as const;

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false,
  });
}

function formatSummary(log: LogEvent): string {
  switch (log.type) {
    case 'RESPONSE': {
      const d = log.data as IpcResponseData;
      return `${d.cmd} → ${d.status}  ${d.message}`;
    }
    case 'RESULT': {
      const d = log.data as IpcResultData;
      const pos = d.position !== undefined ? `  pos: ${d.position}` : '';
      return `${d.cmd} → ${d.status}${pos}  ${d.message}`;
    }
    case 'ERROR': {
      const d = log.data as IpcErrorData;
      return `[${d.errorCode}] ${d.errorName}: ${d.context}`;
    }
    case 'STATE':
      return `${log.data.status} | 위치 ${log.data.position}° | 속도 ${log.data.speed}°/s`;
  }
}

function getLevelClass(type: LogEvent['type']): string {
  switch (type) {
    case 'RESPONSE': return 'level-info';
    case 'RESULT': return 'level-warn';
    case 'ERROR': return 'level-error';
    case 'STATE': return '';
  }
}

function getRowClass(type: LogEvent['type']): string {
  switch (type) {
    case 'RESPONSE': return 'log-row-info';
    case 'RESULT': return 'log-row-warn';
    case 'ERROR': return 'log-row-error';
    case 'STATE': return '';
  }
}

export function LogPage() {
  const {
    logs, ipcIds, filter, setFilter,
    loading, hasMore, loadMore,
  } = useIpcLogSearch();

  const bottomRef = useRef<HTMLDivElement>(null);
  const selectedTypes = filter.type ?? [];

  useEffect(() => {
    const el = bottomRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      (entries) => { if (entries[0].isIntersecting) loadMore(); },
      { threshold: 0.1 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [loadMore]);

  const toggleType = useCallback((t: string) => {
    if (t === 'ALL') {
      setFilter((f) => ({ ...f, type: undefined }));
      return;
    }
    setFilter((f) => {
      const prev = f.type ?? [];
      const next = prev.includes(t) ? prev.filter((x) => x !== t) : [...prev, t];
      return { ...f, type: next.length ? next : undefined };
    });
  }, [setFilter]);

  const hasDateFilter = filter.from || filter.to || filter.ipcId;

  return (
    <div className="log-page">

      {/* 헤더 */}
      <div className="log-page-header">
        <h2>로그 검색</h2>
      </div>

      {/* 필터 */}
      <div className="log-page-filters">
        <div className="log-page-filter-row">
          <input
            type="datetime-local"
            value={filter.from?.slice(0, 16) ?? ''}
            onChange={(e) =>
              setFilter((f) => ({ ...f, from: e.target.value ? `${e.target.value}:00` : undefined }))
            }
          />
          <span className="log-page-filter-sep">~</span>
          <input
            type="datetime-local"
            value={filter.to?.slice(0, 16) ?? ''}
            onChange={(e) =>
              setFilter((f) => ({ ...f, to: e.target.value ? `${e.target.value}:59` : undefined }))
            }
          />
          <select
            value={filter.ipcId ?? ''}
            onChange={(e) =>
              setFilter((f) => ({ ...f, ipcId: e.target.value || undefined }))
            }
          >
            <option value="">전체 IPC</option>
            {ipcIds.map((id) => (
              <option key={id} value={id}>{id}</option>
            ))}
          </select>
          {hasDateFilter && (
            <button
              className="log-page-reset"
              type="button"
              onClick={() => setFilter((f) => ({ ...f, from: undefined, to: undefined, ipcId: undefined }))}
            >
              초기화
            </button>
          )}
        </div>

        <div className="log-page-filter-row">
          {TYPE_FILTERS.map((t) => {
            const isAll = t === 'ALL';
            const active = isAll ? !selectedTypes.length : selectedTypes.includes(t);
            return (
              <button
                key={t}
                type="button"
                className={`log-page-type-btn${active ? ' active' : ''}`}
                onClick={() => toggleType(t)}
              >
                {t}
              </button>
            );
          })}
        </div>
      </div>

      {/* 로그 목록 */}
      {/* 로그 목록 */}
      <div className="log-page-body">
        <table className="log-page-table">
          <thead>
            <tr>
              <th>시각</th>
              <th>장비</th>
              <th>타입</th>
              <th>내용</th>
            </tr>
          </thead>
        </table>
        <div className="log-page-scroll">
          <table className="log-page-table">
            <tbody>
              {logs.length === 0 && !loading ? (
                <tr>
                  <td colSpan={4}>
                    <div className="log-page-empty">조회된 로그가 없습니다.</div>
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <tr
                    key={log.messageId}
                    className={`log-row ${getRowClass(log.type)}`}
                  >
                    <td style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                      {formatDate(log.occurredAt)}
                    </td>
                    <td>{log.ipcId}</td>
                    <td>
                      <span className={`log-level ${getLevelClass(log.type)}`}>
                        {log.type}
                      </span>
                    </td>
                    <td>{formatSummary(log)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          <div ref={bottomRef} className="log-page-footer">
            {loading && '로드 중...'}
            {!loading && !hasMore && logs.length > 0 && '모든 로그를 불러왔습니다.'}
          </div>
        </div>
      </div>
    </div>
  );
}
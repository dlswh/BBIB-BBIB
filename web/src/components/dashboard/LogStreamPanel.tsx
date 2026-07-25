import { Pause, Play, ChevronDown } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import type { LogEvent, IpcResponseData, IpcResultData, IpcErrorData } from '../../types';
import { PanelTitle } from '../common/PanelTitle';

type LogStreamPanelProps = {
  logs: LogEvent[];
  paused: boolean;
  onTogglePause: () => void;
  onMoreClick: () => void;  // 추가
};

type FilterType = 'ALL' | 'RESPONSE' | 'RESULT' | 'ERROR';

const FILTER_OPTIONS: { value: FilterType; label: string }[] = [
  { value: 'ALL', label: '타입 전체' },
  { value: 'RESPONSE', label: 'RESPONSE' },
  { value: 'RESULT', label: 'RESULT' },
  { value: 'ERROR', label: 'ERROR' },
];

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
      return '';
  }
}

function getTypeClass(type: LogEvent['type']): string {
  switch (type) {
    case 'RESPONSE': return 'log-row-info';
    case 'RESULT': return 'log-row-warn';
    case 'ERROR': return 'log-row-error';
    case 'STATE': return '';
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

export function LogStreamPanel({ logs, paused, onTogglePause, onMoreClick }: LogStreamPanelProps) {
  const [filter, setFilter] = useState<FilterType>('ALL');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const selectedLabel = FILTER_OPTIONS.find((o) => o.value === filter)?.label ?? '타입 전체';
  const filteredLogs = filter === 'ALL' ? logs : logs.filter((log) => log.type === filter);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <section className="panel log-panel" aria-labelledby="log-stream-title">
      <PanelTitle
        title="실시간 로그 스트림"
        titleId="log-stream-title"
        action={
          <span className="log-actions">
            <button
              type="button"
              onClick={onTogglePause}
              className={paused ? 'log-paused' : ''}
            >
              {paused ? (
                <>
                  <Play size={14} aria-hidden="true" />
                  재개
                </>
              ) : (
                <>
                  <Pause size={14} aria-hidden="true" />
                  일시정지
                </>
              )}
            </button>

            <div className="log-filter-dropdown" ref={dropdownRef}>
              <button
                type="button"
                onClick={() => setDropdownOpen((prev) => !prev)}
              >
                {selectedLabel}
                <ChevronDown size={12} aria-hidden="true" />
              </button>
              {dropdownOpen && (
                <ul className="log-filter-menu" role="listbox">
                  {FILTER_OPTIONS.map((option) => (
                    <li
                      key={option.value}
                      role="option"
                      aria-selected={filter === option.value}
                      className={`log-filter-item${filter === option.value ? ' log-filter-item-active' : ''}`}
                      onClick={() => {
                        setFilter(option.value);
                        setDropdownOpen(false);
                      }}
                    >
                      {option.label}
                    </li>
                  ))}
                </ul>
              )}
            </div>

            <button type="button" onClick={onMoreClick}>더 보기</button>
          </span>
        }
      />
      <table className="log-table">
        <thead>
          <tr>
            <th>시간</th>
            <th>장비</th>
            <th>타입</th>
            <th>내용</th>
          </tr>
        </thead>
      </table>
      <div className="log-table-scroll">
        <table className="log-table">
          <tbody>
            {filteredLogs.length > 0 ? (
              filteredLogs.map((log) => (
                <tr
                  className={`log-row ${getTypeClass(log.type)}`}
                  key={`${log.occurredAt}-${log.ipcId}-${log.messageId}`}
                >
                  <td>{log.occurredAt}</td>
                  <td>{log.ipcId}</td>
                  <td>
                    <span className={`log-level ${getLevelClass(log.type)}`}>
                      {log.type}
                    </span>
                  </td>
                  <td>{formatSummary(log)}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4}>수집된 로그가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </section>
  );
}
import type { ReactNode } from 'react';
import { Activity, RadioTower, Server, TriangleAlert } from 'lucide-react';
import type { Machine, MachineStatusSummary } from '../../types';
import type { IpcStateStreamEvent } from '../../hooks/useIpcStateStream';
import { MachineStatusPanel } from './MachineStatusPanel';

type MachineStatusPageProps = {
  machines: Machine[];
  summary: MachineStatusSummary;
  connected: boolean;
  error: string | null;
  events: IpcStateStreamEvent[];
  stateCount: number;
};

export function MachineStatusPage({
  machines,
  summary,
  connected,
  error,
  events,
  stateCount,
}: MachineStatusPageProps) {
  const attentionCount = summary.counts.ESTOPPED + summary.counts.ERROR + summary.counts.OFFLINE;

  return (
    <div className="machine-status-page">
      <section className="machine-status-page__header">
        <div>
          <span>Machine State</span>
          <h2>장비 상태</h2>
        </div>
        <p>수신된 IPC 상태와 등록 장비 목록을 기준으로 현재 카드 표시 상태를 확인합니다.</p>
      </section>

      <section className="machine-status-metrics" aria-label="장비 상태 요약">
        <Metric
          icon={<RadioTower size={20} aria-hidden="true" />}
          label="SSE 연결"
          value={connected ? 'CONNECTED' : 'DISCONNECTED'}
          tone={connected ? 'ok' : 'bad'}
        />
        <Metric
          icon={<Server size={20} aria-hidden="true" />}
          label="카드 표시 장비"
          value={`${machines.length} / ${summary.totalCount}`}
          tone="neutral"
        />
        <Metric
          icon={<Activity size={20} aria-hidden="true" />}
          label="최근 STATE"
          value={`${events.length}`}
          tone="neutral"
        />
        <Metric
          icon={<TriangleAlert size={20} aria-hidden="true" />}
          label="주의 필요"
          value={`${attentionCount}`}
          tone={attentionCount > 0 ? 'warn' : 'ok'}
        />
      </section>

      {error ? <p className="machine-status-page__error">{error}</p> : null}

      <section className="machine-status-page__content">
        <MachineStatusPanel machines={machines} summary={summary} />
        <section className="panel machine-status-events" aria-labelledby="machine-status-events-title">
          <div className="panel-title">
            <div>
              <span>Live Events</span>
              <h2 id="machine-status-events-title">최근 STATE 메시지</h2>
            </div>
            <strong className="machine-status-events__count">{stateCount}대 수신</strong>
          </div>
          <div className="machine-status-events__table-scroll" aria-live="polite">
            <table>
              <thead>
                <tr>
                  <th>시간</th>
                  <th>IPC</th>
                  <th>상태</th>
                  <th>위치</th>
                  <th>속도</th>
                </tr>
              </thead>
              <tbody>
                {events.length > 0 ? (
                  events.map((event) => (
                    <tr key={event.id}>
                      <td>{formatTimestamp(event.timestamp)}</td>
                      <td>{event.ipcId}</td>
                      <td>
                        <span className={`machine-status-events__status status-${event.status.toLowerCase()}`}>
                          {event.status}
                        </span>
                      </td>
                      <td>{event.position.toFixed(1)} cm</td>
                      <td>{event.speed.toFixed(1)} cm/s</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5}>아직 수신한 STATE 메시지가 없습니다.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </section>
    </div>
  );
}

function Metric({
  icon,
  label,
  value,
  tone,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  tone: 'ok' | 'bad' | 'warn' | 'neutral';
}) {
  return (
    <article className={`machine-status-metric machine-status-metric-${tone}`}>
      {icon}
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </article>
  );
}

function formatTimestamp(timestamp: string) {
  const date = new Date(timestamp);

  if (Number.isNaN(date.getTime())) {
    return timestamp;
  }

  return date.toLocaleTimeString('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

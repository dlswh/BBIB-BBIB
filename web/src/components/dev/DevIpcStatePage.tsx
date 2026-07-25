import type { ReactNode } from 'react';
import { Activity, RadioTower, Server } from 'lucide-react';
import { MachineStatusPanel } from '../dashboard/MachineStatusPanel';
import { useIpcStateStream } from '../../hooks/useIpcStateStream';

export function DevIpcStatePage() {
  const { connected, error, events, machines, stateCount } = useIpcStateStream();

  return (
    <main className="dev-ipc-state-page">
      <section className="dev-ipc-hero">
        <div>
          <p className="dev-ipc-eyebrow">Kafka to Spring SSE</p>
          <h1>IPC State Stream</h1>
          <p>원격 Kafka의 <code>ipc.motor.state</code> 메시지를 Spring SSE로 받아 기계 상태 갱신을 확인합니다.</p>
        </div>
        <a href="/" className="dev-ipc-back">Dashboard</a>
      </section>

      <section className="dev-ipc-metrics" aria-label="stream status">
        <Metric icon={<RadioTower size={20} />} label="SSE" value={connected ? 'CONNECTED' : 'DISCONNECTED'} tone={connected ? 'ok' : 'bad'} />
        <Metric icon={<Server size={20} />} label="Machines" value={`${stateCount}`} tone="neutral" />
        <Metric icon={<Activity size={20} />} label="Recent events" value={`${events.length}`} tone="neutral" />
      </section>

      {error && <p className="dev-ipc-error">{error}</p>}

      <section className="dev-ipc-content">
        <MachineStatusPanel machines={machines} />

        <section className="panel dev-ipc-events" aria-labelledby="dev-ipc-events-title">
          <div className="panel-title">
            <div>
              <span>Live Events</span>
              <h2 id="dev-ipc-events-title">최근 STATE 메시지</h2>
            </div>
            <strong className="dev-ipc-events__order">최신순</strong>
          </div>
          <div className="dev-ipc-events__table-scroll" aria-live="polite">
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
                      <td>{event.timestamp}</td>
                      <td>{event.ipcId}</td>
                      <td><span className={`dev-ipc-status status-${event.status.toLowerCase()}`}>{event.status}</span></td>
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
    </main>
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
  tone: 'ok' | 'bad' | 'neutral';
}) {
  return (
    <article className={`dev-ipc-metric dev-ipc-metric-${tone}`}>
      {icon}
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
    </article>
  );
}

import { Factory, RefreshCw } from 'lucide-react';
import type { Machine, MachineStatus, MachineStatusSummary } from '../../types';
import { PanelTitle } from '../common/PanelTitle';

const legendStatuses: MachineStatus[] = ['READY', 'MOVING', 'JOGGING', 'ESTOPPED', 'ERROR', 'OFFLINE'];

type MachineStatusPanelProps = {
  machines: Machine[];
  summary?: MachineStatusSummary;
};

export function MachineStatusPanel({ machines, summary }: MachineStatusPanelProps) {
  return (
    <section className="panel machine-panel" aria-labelledby="machine-status-title">
      <PanelTitle
        title="기계 현재 상태"
        titleId="machine-status-title"
        status={
          <>
            <RefreshCw size={17} aria-hidden="true" />
            <span>자동 갱신</span>
          </>
        }
      />
      <div className="machine-grid">
        {machines.length > 0 ? (
          machines.map((machine) => <MachineCard machine={machine} key={machine.id} />)
        ) : (
          <p className="empty-state">표시할 장비 상태가 없습니다.</p>
        )}
      </div>
      <StatusLegend machines={machines} summary={summary} />
    </section>
  );
}

function MachineCard({ machine }: { machine: Machine }) {
  return (
    <article className={`machine-card status-${machine.status.toLowerCase()}`}>
      <div className="machine-card__head">
        <Factory size={40} aria-hidden="true" />
        <div>
          <strong>{machine.id}</strong>
          <span className="status-badge">{machine.status}</span>
        </div>
        <i aria-hidden="true" />
      </div>
      <dl>
        <div>
          <dt>위치</dt>
          <dd>{machine.position}</dd>
        </div>
        <div>
          <dt>속도</dt>
          <dd>{machine.speed}</dd>
        </div>
      </dl>
    </article>
  );
}

function StatusLegend({ machines, summary }: { machines: Machine[]; summary?: MachineStatusSummary }) {
  const counts = new Map<MachineStatus, number>(legendStatuses.map((status) => [status, summary?.counts[status] ?? 0]));

  if (!summary) {
    for (const machine of machines) {
      counts.set(machine.status, (counts.get(machine.status) ?? 0) + 1);
    }
  }

  return (
    <div className="status-legend">
      {legendStatuses.map((status) => (
        <span className={`legend-${status.toLowerCase()}`} key={status}>
          <i aria-hidden="true" />
          {status}
          <b>{counts.get(status) ?? 0}</b>
        </span>
      ))}
      <em>전체 장비 {summary?.totalCount ?? machines.length}대</em>
    </div>
  );
}

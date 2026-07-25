import { AlertTriangle, Clock3, Factory, Gamepad2, Gauge, OctagonAlert, WifiOff } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { DashboardKpi } from '../../types';

const kpiIcons: Record<DashboardKpi['icon'], LucideIcon> = {
  factory: Factory,
  gauge: Gauge,
  clock: Clock3,
  jogging: Gamepad2,
  estop: OctagonAlert,
  warning: AlertTriangle,
  offline: WifiOff,
};

type KpiGridProps = {
  kpis: DashboardKpi[];
};

export function KpiGrid({ kpis }: KpiGridProps) {
  return (
    <section className="kpi-grid" aria-label="장비 요약">
      {kpis.map((kpi) => {
        const Icon = kpiIcons[kpi.icon];

        return (
          <article className={`kpi-card tone-${kpi.tone}`} key={kpi.label}>
            <Icon size={36} aria-hidden="true" />
            <div>
              <span>{kpi.label}</span>
              <strong>
                {kpi.value}
                <small>{kpi.unit}</small>
              </strong>
            </div>
            {kpi.percent ? <em>{kpi.percent}</em> : null}
          </article>
        );
      })}
    </section>
  );
}

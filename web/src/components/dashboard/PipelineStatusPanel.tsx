import {
  Activity,
  Cpu,
  Database,
  Gauge,
  MonitorCheck,
  RadioTower,
  Search,
  Server,
  Workflow,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { PipelineHealth, PipelineStage, PipelineSummaryMetric } from '../../types';

const stageIcons: Record<PipelineStage['icon'], LucideIcon> = {
  ipc: Server,
  ingestion: RadioTower,
  kafka: Workflow,
  consumer: Cpu,
  timescale: Database,
  elastic: Search,
  dashboard: MonitorCheck,
};

type PipelineStatusPanelProps = {
  pipeline: PipelineHealth;
};

export function PipelineStatusPanel({ pipeline }: PipelineStatusPanelProps) {
  return (
    <section className="panel pipeline-panel" aria-labelledby="pipeline-status-title">
      <div className="pipeline-identity">
        <span className="pipeline-eyebrow">DATA HIGHWAY</span>
        <h2 id="pipeline-status-title">데이터 파이프라인 상태</h2>
        <div className="pipeline-health">
          <Activity size={16} aria-hidden="true" />
          <span>Pipeline Health</span>
          <strong>{pipeline.status}</strong>
        </div>
        <small>Updated {pipeline.updatedAt}</small>
      </div>

      <PipelineStageFlow stages={pipeline.stages} />
      <PipelineSummary metrics={pipeline.summaryMetrics} />
    </section>
  );
}

function PipelineStageFlow({ stages }: { stages: PipelineStage[] }) {
  return (
    <ol className="pipeline-flow" aria-label="데이터 수집 처리 흐름">
      {stages.map((stage) => {
        const Icon = stageIcons[stage.icon];

        return (
          <li className={`pipeline-stage stage-${stage.status}`} key={stage.id}>
            <span className="pipeline-stage__icon">
              <Icon size={22} aria-hidden="true" />
            </span>
            <span className="pipeline-stage__text">
              <strong>{stage.label}</strong>
              <small>{stage.description}</small>
            </span>
          </li>
        );
      })}
    </ol>
  );
}

function PipelineSummary({ metrics }: { metrics: PipelineSummaryMetric[] }) {
  return (
    <div className="pipeline-summary" aria-label="파이프라인 요약 지표">
      {metrics.map((metric) => (
        <article className={`pipeline-summary-metric metric-${metric.tone}`} key={metric.label}>
          <span>{metric.label}</span>
          <strong>
            {metric.value}
            {metric.unit ? <small>{metric.unit}</small> : null}
          </strong>
        </article>
      ))}
      <Gauge className="pipeline-summary__mark" size={70} aria-hidden="true" />
    </div>
  );
}

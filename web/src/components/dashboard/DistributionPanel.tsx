import type { CSSProperties } from 'react';
import type { StatusDistribution } from '../../types';
import { PanelTitle } from '../common/PanelTitle';

type DistributionPanelProps = {
  distribution: StatusDistribution[];
  totalDevices: string;
  onViewAll?: () => void;
};

type DistributionChartItem = StatusDistribution & {
  count: number;
  ratio: number;
};

type DistributionStyle = CSSProperties & {
  '--distribution-ratio': string;
};

export function DistributionPanel({ distribution, totalDevices, onViewAll }: DistributionPanelProps) {
  const chartItems = normalizeDistribution(distribution, totalDevices);

  return (
    <section className="panel distribution-panel" aria-labelledby="distribution-title">
      <PanelTitle
        title="장비 상태 분포"
        titleId="distribution-title"
        actionLabel="전체 보기"
        onAction={onViewAll}
      />
      <div className="distribution-body">
        <div className="distribution-chart">
          <DonutChart distribution={chartItems} totalDevices={totalDevices} />
        </div>
        <ul className="distribution-legend">
          {chartItems.map((item) => (
            <li className={`dot-${item.color}`} key={item.label}>
              <span>{item.label}</span>
              <span className="distribution-bar" aria-hidden="true">
                <i style={{ '--distribution-ratio': `${item.ratio * 100}%` } as DistributionStyle} />
              </span>
              <strong>{item.value}</strong>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

function DonutChart({
  distribution,
  totalDevices,
}: {
  distribution: DistributionChartItem[];
  totalDevices: string;
}) {
  const radius = 44;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;
  const segments = distribution
    .filter((item) => item.count > 0 && item.ratio > 0)
    .map((item) => {
      const length = item.ratio * circumference;
      const segment = {
        ...item,
        dashArray: `${length} ${circumference - length}`,
        dashOffset: -offset,
      };
      offset += length;
      return segment;
    });
  const description = distribution.map((item) => `${item.label} ${item.value}`).join(', ');

  return (
    <div className="donut" role="img" aria-label={`전체 장비 ${totalDevices}대 상태 분포: ${description}`}>
      <svg className="donut-ring" viewBox="0 0 120 120" aria-hidden="true">
        <circle className="donut-track" cx="60" cy="60" r={radius} />
        {segments.map((item) => (
          <circle
            className={`donut-segment segment-${item.color}`}
            key={item.label}
            cx="60"
            cy="60"
            r={radius}
            strokeDasharray={item.dashArray}
            strokeDashoffset={item.dashOffset}
          />
        ))}
      </svg>
      <div className="donut-center">
        <strong>{totalDevices}</strong>
        <span>Total</span>
      </div>
    </div>
  );
}

function normalizeDistribution(distribution: StatusDistribution[], totalDevices: string): DistributionChartItem[] {
  const total = Number.parseInt(totalDevices, 10);
  const fallbackTotal = Number.isFinite(total) && total > 0 ? total : sumDistributionCounts(distribution);

  return distribution.map((item) => {
    const count = item.count ?? parseDistributionCount(item.value);
    const ratio = item.ratio ?? (fallbackTotal > 0 ? count / fallbackTotal : 0);

    return {
      ...item,
      count,
      ratio: clampRatio(ratio),
    };
  });
}

function sumDistributionCounts(distribution: StatusDistribution[]) {
  return distribution.reduce((sum, item) => sum + (item.count ?? parseDistributionCount(item.value)), 0);
}

function parseDistributionCount(value: string) {
  const count = Number.parseInt(value, 10);
  return Number.isFinite(count) ? count : 0;
}

function clampRatio(ratio: number) {
  if (!Number.isFinite(ratio)) {
    return 0;
  }

  return Math.min(Math.max(ratio, 0), 1);
}

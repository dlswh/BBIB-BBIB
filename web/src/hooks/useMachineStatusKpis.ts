import { useMemo } from 'react';
import type {
  DashboardKpi,
  IpcFleetSetting,
  Machine,
  MachineStatus,
  MachineStatusSummary,
  StatusDistribution,
} from '../types';

const statusKpiMeta: Array<{
  status: MachineStatus;
  label: string;
  icon: DashboardKpi['icon'];
  tone: DashboardKpi['tone'];
}> = [
  { status: 'READY', label: 'READY', icon: 'clock', tone: 'green' },
  { status: 'MOVING', label: 'MOVING', icon: 'gauge', tone: 'blue' },
  { status: 'JOGGING', label: 'JOGGING', icon: 'jogging', tone: 'violet' },
  { status: 'ESTOPPED', label: 'ESTOPPED', icon: 'estop', tone: 'orange' },
  { status: 'ERROR', label: 'ERROR', icon: 'warning', tone: 'red' },
  { status: 'OFFLINE', label: 'OFFLINE', icon: 'offline', tone: 'muted' },
];

const statusDistributionColor: Record<MachineStatus, StatusDistribution['color']> = {
  READY: 'green',
  MOVING: 'blue',
  JOGGING: 'violet',
  ESTOPPED: 'orange',
  ERROR: 'red',
  OFFLINE: 'gray',
};

const zeroCounts = (): Record<MachineStatus, number> => ({
  READY: 0,
  MOVING: 0,
  JOGGING: 0,
  ESTOPPED: 0,
  ERROR: 0,
  OFFLINE: 0,
});

export function useMachineStatusKpis(machines: Machine[], setting: IpcFleetSetting, now: number) {
  return useMemo(() => {
    const thresholdMs = setting.offlineThresholdSeconds * 1000;
    const configuredIpcIds = Array.from(new Set(setting.configuredIpcIds ?? []));
    const configuredIpcIdSet = new Set(configuredIpcIds);
    const machinesById = new Map(machines.map((machine) => [machine.id, machine]));
    const counts = zeroCounts();
    const displayMachines: Machine[] = [];
    let activeCount = 0;
    let recentUnregisteredCount = 0;

    for (const configuredIpcId of configuredIpcIds) {
      const machine = machinesById.get(configuredIpcId);
      if (machine && isMachineRecent(machine, now, thresholdMs) && machine.status !== 'OFFLINE') {
        counts[machine.status] += 1;
        activeCount += 1;
        displayMachines.push(machine);
        continue;
      }

      displayMachines.push(createOfflineMachine(configuredIpcId));
    }

    for (const machine of machines) {
      if (configuredIpcIdSet.has(machine.id)) {
        continue;
      }

      if (!isMachineRecent(machine, now, thresholdMs) || machine.status === 'OFFLINE') {
        continue;
      }

      counts[machine.status] += 1;
      activeCount += 1;
      recentUnregisteredCount += 1;
      displayMachines.push(machine);
    }

    displayMachines.sort((a, b) => a.id.localeCompare(b.id));

    const displayedIdentityCount = configuredIpcIds.length + recentUnregisteredCount;
    const total = Math.max(setting.totalCount, displayedIdentityCount);
    counts.OFFLINE = Math.max(0, total - activeCount);

    const kpis: DashboardKpi[] = [
      {
        label: '전체 장비',
        value: String(total),
        unit: '대',
        icon: 'factory',
        tone: 'neutral',
      },
      ...statusKpiMeta.map(({ status, label, icon, tone }) => {
        const value = counts[status];

        return {
          label,
          value: String(value),
          unit: '대',
          percent: formatPercent(value, total),
          icon,
          tone,
        };
      }),
    ];

    const summary: MachineStatusSummary = {
      totalCount: total,
      counts,
    };

    const distribution = statusKpiMeta.map(({ status, label }) => {
      const value = counts[status];

      return {
        label,
        value: `${value} (${formatPercent(value, total)})`,
        color: statusDistributionColor[status],
        count: value,
        ratio: total > 0 ? value / total : 0,
      };
    });

    return {
      distribution,
      kpis,
      machines: displayMachines,
      summary,
    };
  }, [machines, setting, now]);
}

function createOfflineMachine(id: string): Machine {
  return {
    id,
    status: 'OFFLINE',
    position: '-',
    speed: '-',
  };
}

function isMachineRecent(machine: Machine, now: number, thresholdMs: number) {
  if (!machine.lastSeenAt) {
    return machine.status !== 'OFFLINE';
  }

  const lastSeenTime = Date.parse(machine.lastSeenAt);
  if (Number.isNaN(lastSeenTime)) {
    return false;
  }

  return now - lastSeenTime <= thresholdMs;
}

function formatPercent(value: number, total: number) {
  if (total === 0) {
    return '0.00%';
  }

  return `${((value / total) * 100).toFixed(2)}%`;
}

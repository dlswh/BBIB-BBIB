export type MachineStatus = 'READY' | 'MOVING' | 'JOGGING' | 'ESTOPPED' | 'ERROR' | 'OFFLINE';
export type LogLevel = 'CRITICAL' | 'ERROR' | 'WARN' | 'INFO';
export type ThemeMode = 'dark' | 'light';
export type AppView = 'landing' | 'login' | 'dashboard';

export type LoginRequest = {
  username: string;
  password: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  username: string;
  name: string;
  role: string;
};

export type AuthSession = LoginResponse;

export type UserProfile = {
  id?: number;
  username: string;
  name: string;
  role: string;
  enabled?: boolean;
  createdAt?: string;
};

export type UpdateUserNameRequest = {
  name: string;
};

export type DashboardKpi = {
  label: string;
  value: string;
  unit: string;
  percent?: string;
  icon: 'factory' | 'gauge' | 'clock' | 'jogging' | 'estop' | 'warning' | 'offline';
  tone: 'neutral' | 'green' | 'blue' | 'yellow' | 'red' | 'orange' | 'violet' | 'muted';
};

export type Machine = {
  id: string;
  status: MachineStatus;
  position: string;
  speed: string;
  lastSeenAt?: string;
};

export type IpcFleetSetting = {
  totalCount: number;
  offlineThresholdSeconds: number;
  configuredIpcIds: string[];
  updatedAt?: string;
};

export type UpdateIpcFleetSettingRequest = {
  totalCount: number;
  offlineThresholdSeconds: number;
  configuredIpcIds?: string[] | null;
};

export type MachineStatusSummary = {
  totalCount: number;
  counts: Record<MachineStatus, number>;
};

// IPC 메시지 타입별 data 구조
export type IpcResponseData = {
  requestId: string;
  cmd: string;
  status: 'ACCEPTED' | 'REJECTED';
  message: string;
};
 
export type IpcResultData = {
  requestId: string;
  cmd: string;
  status: 'COMPLETED' | 'FAILED';
  message: string;
  position?: number;
  speed?: number;
};
 
export type IpcErrorData = {
  errorCode: number;
  errorName: string;
  category: string;
  severity: string;
  context: string;
  solution: string;
  requestId?: string;
};

export type IpcStateData = {
  status: 'IDLE' | 'MOVING' | 'JOGGING' | 'ERROR' | 'E_STOPPED';
  position: number;
  speed: number;
};

export type IpcStateEvent = {
  ipcId: string;
  payload: {
    type: 'STATE';
    id: string;
    timestamp: string;
    data: IpcStateData;
  };
};
 
export type LogEvent =
  | {
      messageId: string;      // ipc-1:res-260427-1
      ipcId: string;
      type: 'RESPONSE';
      occurredAt: string;     // timestamp → occurredAt
      data: IpcResponseData;
      status: string;
    }
  | {
      messageId: string;
      ipcId: string;
      type: 'RESULT';
      occurredAt: string;
      data: IpcResultData;
      status: string;
    }
  | {
      messageId: string;
      ipcId: string;
      type: 'ERROR';
      occurredAt: string;
      data: IpcErrorData;
      status: string | null;
    }
  | {
    messageId: string;
    ipcId: string;
    type: 'STATE';
    occurredAt: string;
    data: IpcStateData;
    status: string;
  };

export type LegacyDashboardLogEvent = {
  time: string;
  device: string;
  level: LogLevel;
  event: string;
  errorCode: string;
  message: string;
};

export type OperatorSession = {
  operatorId: string;
  role: string;
  ipAddress: string;
  connectedAt: string;
};

export type ConnectionSnapshot = {
  activeOperators: number;
  activeSessions: number;
  blockedDuplicateLogins: number;
  currentOperators: OperatorSession[];
  extraOnlineCount: number;
};

export type StatusDistribution = {
  label: string;
  value: string;
  color: 'green' | 'blue' | 'red' | 'yellow' | 'orange' | 'violet' | 'gray';
  count?: number;
  ratio?: number;
};

export type PipelineStage = {
  id: string;
  label: string;
  description: string;
  icon: 'ipc' | 'ingestion' | 'kafka' | 'consumer' | 'timescale' | 'elastic' | 'dashboard';
  status: 'ok' | 'warning' | 'critical';
};

export type PipelineSummaryMetric = {
  label: string;
  value: string;
  unit?: string;
  tone: 'green' | 'blue' | 'violet' | 'yellow' | 'red';
};

export type PipelineHealth = {
  status: '정상' | '주의' | '장애';
  updatedAt: string;
  stages: PipelineStage[];
  summaryMetrics: PipelineSummaryMetric[];
};

export type DashboardSnapshot = {
  generatedAt: string;
  systemStatus: '정상' | '주의' | '장애';
  currentUser: {
    id: string;
    role: string;
  };
  notificationCount: number;
  kpis: DashboardKpi[];
  machines: Machine[];
  connection: ConnectionSnapshot;
  logs: Array<LogEvent | LegacyDashboardLogEvent>;
  distribution: StatusDistribution[];
  pipeline: PipelineHealth;
};

// 백엔드 세션 이벤트 타입
export type SessionEvent = {
  eventType: 'LOGIN' | 'LOGOUT';
  username: string;
  name: string;
  role: 'ADMIN' | 'WORKER'; // 추가
  loginAt: string;
  ipAddress: string;
};

export type WorkerUser = {
  id: number;
  username: string;
  name: string;
  role: 'ADMIN' | 'WORKER';
  enabled: boolean;
  createdAt: string;
};

import { CalendarDays, Users, UsersRound } from 'lucide-react';
import type { ReactNode } from 'react';
import type { ConnectionSnapshot } from '../../types';
import { PanelTitle } from '../common/PanelTitle';

type ConnectionPanelProps = {
  connection: ConnectionSnapshot;
};

export function ConnectionPanel({ connection }: ConnectionPanelProps) {
  return (
    <section className="panel connection-panel" aria-labelledby="connection-title">
      <PanelTitle title="실시간 접속 현황" titleId="connection-title"/>
      <div className="connection-content">
        <div className="connection-summary">
          <ConnectionMetric
            label="현재 접속자"
            value={`${connection.activeOperators}명`}
            icon={<Users size={30} aria-hidden="true" />}
            tone="green"
          />
          <ConnectionMetric
            label="전체 작업자"
            value={`${connection.extraOnlineCount}명`}
            icon={<UsersRound size={30} aria-hidden="true" />}
            tone="blue"
          />
          <ConnectionMetric
            label="금일 접속자"
            value={`${connection.blockedDuplicateLogins}명`}
            icon={<CalendarDays size={30} aria-hidden="true" style={{ marginRight: '4px' }} />}
            tone="red"
          />
        </div>

        <strong className="connection-subtitle">현재 접속 작업자</strong>
        <div className="connection-table-wrapper">
        <table className="connection-table">
          <thead>
            <tr>
              <th>작업자 ID</th>
              <th>이름</th>
              <th>접속 IP</th>
              <th>접속 시간</th>
            </tr>
          </thead>
          <tbody>
            {connection.currentOperators.length > 0 ? (
              connection.currentOperators
              .filter((operator) => operator.role !== 'ADMIN')
              .map((operator) => (
                <tr key={operator.operatorId}>
                  <td>
                    <span className="online-dot" aria-hidden="true" />
                    {operator.operatorId}
                  </td>
                  <td>
                    <span className="role-badge">{operator.role}</span>
                  </td>
                  <td>{operator.ipAddress}</td>
                  <td>{operator.connectedAt}</td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan={4}>현재 접속 중인 작업자가 없습니다.</td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      </div>
    </section>
  );
}

function ConnectionMetric({
  label,
  value,
  icon,
  tone,
}: {
  label: string;
  value: string;
  icon: ReactNode;
  tone: 'green' | 'blue' | 'red';
}) {
  return (
    <article className={`connection-metric metric-${tone}`}>
      <div>
        <span>{label}</span>
        <strong>{value}</strong>
      </div>
      {icon}
    </article>
  );
}
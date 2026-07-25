import { ChevronRight } from 'lucide-react';
import type { ReactNode } from 'react';

type PanelTitleProps = {
  title: string;
  titleId?: string;
  status?: ReactNode;
  actionLabel?: string;
  onAction?: () => void;
  action?: ReactNode;
};

export function PanelTitle({ title, titleId, status, actionLabel, onAction, action }: PanelTitleProps) {
  return (
    <div className="panel-title">
      <h2 id={titleId}>{title}</h2>
      {status && <div className="panel-action">{status}</div>}
      {actionLabel && (
        <button className="panel-action panel-action-button" type="button" onClick={onAction}>
          {actionLabel}
          <ChevronRight size={16} aria-hidden="true" />
        </button>
      )}
      {action && <div className="panel-action">{action}</div>}
    </div>
  );
}

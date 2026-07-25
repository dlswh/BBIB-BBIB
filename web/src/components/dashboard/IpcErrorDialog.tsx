import { AlertTriangle } from 'lucide-react';
import type { IpcErrorData } from '../../types';

type IpcErrorDialogProps = {
  ipcId: string;
  occurredAt: string;
  data: IpcErrorData;
  onClose: () => void;
};

export function IpcErrorDialog({ ipcId, occurredAt, data, onClose }: IpcErrorDialogProps) {
  return (
    <>
      <div className="ipc-error-backdrop" aria-hidden="true" />
      <div
        className="ipc-error-dialog"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="ipc-error-title"
      >
        <header className="ipc-error-dialog__header">
          <span className="ipc-error-dialog__icon" aria-hidden="true">
            <AlertTriangle size={18} />
          </span>
          <h2 className="ipc-error-dialog__title" id="ipc-error-title">
            IPC 오류 감지
          </h2>
        </header>

        <div className="ipc-error-dialog__body">
          <dl className="ipc-error-detail">
            <div className="ipc-error-detail__row">
              <dt>장치</dt>
              <dd>{ipcId}</dd>
            </div>
            <div className="ipc-error-detail__row">
              <dt>발생 시각</dt>
              <dd>{new Date(occurredAt).toLocaleString('ko-KR')}</dd>
            </div>
            <div className="ipc-error-detail__row">
              <dt>오류 코드</dt>
              <dd>
                <span className="ipc-error-code-badge">{data.errorCode}</span>
                {data.errorName}
              </dd>
            </div>
            <div className="ipc-error-detail__row">
              <dt>분류</dt>
              <dd>{data.category} / {data.severity}</dd>
            </div>
            <div className="ipc-error-detail__row ipc-error-detail__row--multiline">
              <dt>상황</dt>
              <dd>{data.context}</dd>
            </div>
            <div className="ipc-error-detail__row ipc-error-detail__row--multiline">
              <dt>조치 방법</dt>
              <dd>{data.solution}</dd>
            </div>
            {data.requestId && (
              <div className="ipc-error-detail__row">
                <dt>요청 ID</dt>
                <dd>{data.requestId}</dd>
              </div>
            )}
          </dl>
        </div>

        <footer className="ipc-error-dialog__footer">
          <button
            className="ipc-error-dialog__confirm"
            type="button"
            onClick={onClose}
            autoFocus
          >
            확인
          </button>
        </footer>
      </div>
    </>
  );
}
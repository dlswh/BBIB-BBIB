import { ChangeEvent, FormEvent, useEffect, useRef, useState } from 'react';
import { FileUp, RotateCcw, Save, Settings } from 'lucide-react';
import type { IpcFleetSetting, UpdateIpcFleetSettingRequest } from '../../types';
import { parseIpcIdCsv } from '../../utils/ipcIdCsv';

type IpcFleetSettingsPageProps = {
  setting: IpcFleetSetting;
  isLoading: boolean;
  isSaving: boolean;
  error: string | null;
  onSave: (request: UpdateIpcFleetSettingRequest) => Promise<IpcFleetSetting>;
};

export function IpcFleetSettingsPage({
  setting,
  isLoading,
  isSaving,
  error,
  onSave,
}: IpcFleetSettingsPageProps) {
  const [totalCount, setTotalCount] = useState(String(setting.totalCount));
  const [offlineThresholdSeconds, setOfflineThresholdSeconds] = useState(String(setting.offlineThresholdSeconds));
  const [pendingConfiguredIpcIds, setPendingConfiguredIpcIds] = useState<string[] | null>(null);
  const [selectedFileName, setSelectedFileName] = useState<string | null>(null);
  const [localError, setLocalError] = useState<string | null>(null);
  const [savedMessage, setSavedMessage] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setTotalCount(String(setting.totalCount));
    setOfflineThresholdSeconds(String(setting.offlineThresholdSeconds));
    setPendingConfiguredIpcIds(null);
    setSelectedFileName(null);
  }, [setting.totalCount, setting.offlineThresholdSeconds, setting.configuredIpcIds]);

  const displayedConfiguredIpcIds = pendingConfiguredIpcIds ?? setting.configuredIpcIds;

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.currentTarget;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    try {
      const content = await file.text();
      setPendingConfiguredIpcIds(parseIpcIdCsv(content));
      setSelectedFileName(file.name);
      setLocalError(null);
      setSavedMessage(null);
    } catch {
      setLocalError('로봇 ID 파일을 읽지 못했습니다.');
      setSavedMessage(null);
    } finally {
      input.value = '';
    }
  };

  const handleClearConfiguredIpcIds = () => {
    setPendingConfiguredIpcIds([]);
    setSelectedFileName(null);
    setLocalError(null);
    setSavedMessage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const parsedTotalCount = Number(totalCount);
    const parsedOfflineThresholdSeconds = Number(offlineThresholdSeconds);

    if (!Number.isInteger(parsedTotalCount) || parsedTotalCount < 0) {
      setLocalError('전체 로봇 대수는 0 이상의 정수로 입력해주세요.');
      setSavedMessage(null);
      return;
    }

    if (!Number.isInteger(parsedOfflineThresholdSeconds) || parsedOfflineThresholdSeconds < 1) {
      setLocalError('오프라인 기준 시간은 1초 이상의 정수로 입력해주세요.');
      setSavedMessage(null);
      return;
    }

    setLocalError(null);
    setSavedMessage(null);
    try {
      const request: UpdateIpcFleetSettingRequest = {
        totalCount: parsedTotalCount,
        offlineThresholdSeconds: parsedOfflineThresholdSeconds,
      };
      if (pendingConfiguredIpcIds !== null) {
        request.configuredIpcIds = pendingConfiguredIpcIds;
      }

      await onSave(request);
      setSavedMessage('설정을 저장했습니다.');
    } catch {
      setSavedMessage(null);
    }
  };

  return (
    <section className="settings-page panel" aria-labelledby="ipc-fleet-settings-title">
      <div className="settings-page__header">
        <div>
          <span>Settings</span>
          <h1 id="ipc-fleet-settings-title">로봇 대수 설정</h1>
          <p>단일 공장 기준으로 전체 로봇 대수와 오프라인 판정 시간을 관리합니다.</p>
        </div>
        <Settings size={32} aria-hidden="true" />
      </div>

      <form className="settings-form" onSubmit={handleSubmit}>
        <label>
          <span>전체 로봇 대수</span>
          <input
            type="number"
            inputMode="numeric"
            min={0}
            step={1}
            value={totalCount}
            disabled={isLoading || isSaving}
            onChange={(event) => setTotalCount(event.target.value)}
          />
        </label>
        <label>
          <span>오프라인 기준 시간</span>
          <input
            type="number"
            inputMode="numeric"
            min={1}
            step={1}
            value={offlineThresholdSeconds}
            disabled={isLoading || isSaving}
            onChange={(event) => setOfflineThresholdSeconds(event.target.value)}
          />
          <small>초 단위로 입력합니다. 기본값은 10초입니다.</small>
        </label>

        <div className="settings-file-field">
          <div>
            <span>로봇 ID 목록 파일</span>
            <small>CSV/TXT 파일의 콤마 구분 ID를 카드 표시 기준으로 저장합니다.</small>
          </div>
          <div className="settings-file-row">
            <label className="settings-file-button">
              <FileUp size={18} aria-hidden="true" />
              파일 선택
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv,.txt,text/csv,text/plain"
                disabled={isLoading || isSaving}
                onChange={handleFileChange}
              />
            </label>
            <button
              className="settings-secondary-button"
              type="button"
              disabled={isLoading || isSaving || (displayedConfiguredIpcIds.length === 0 && pendingConfiguredIpcIds === null)}
              onClick={handleClearConfiguredIpcIds}
            >
              <RotateCcw size={17} aria-hidden="true" />
              목록 초기화
            </button>
          </div>
          <div className="settings-file-summary">
            <span>{selectedFileName ? `선택 파일: ${selectedFileName}` : '선택된 새 파일 없음'}</span>
            <strong>{displayedConfiguredIpcIds.length}개 등록</strong>
          </div>
          {displayedConfiguredIpcIds.length > 0 ? (
            <ul className="settings-id-list" aria-label="등록된 로봇 ID">
              {displayedConfiguredIpcIds.map((ipcId) => (
                <li key={ipcId}>{ipcId}</li>
              ))}
            </ul>
          ) : (
            <p className="settings-empty-list">저장된 로봇 ID 목록이 없습니다.</p>
          )}
        </div>

        {localError || error ? (
          <p className="settings-alert" role="alert">
            {localError ?? error}
          </p>
        ) : null}

        {savedMessage ? (
          <p className="settings-success" role="status">
            {savedMessage}
          </p>
        ) : null}

        <button className="settings-submit-button" type="submit" disabled={isLoading || isSaving}>
          <Save size={18} aria-hidden="true" />
          {isSaving ? '저장 중' : '저장'}
        </button>
      </form>
    </section>
  );
}

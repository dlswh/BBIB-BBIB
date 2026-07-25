import { useCallback, useEffect, useState } from 'react';
import { getIpcFleetSetting, updateIpcFleetSetting } from '../api/ipcFleetSettings';
import type { IpcFleetSetting, UpdateIpcFleetSettingRequest } from '../types';

const defaultSetting: IpcFleetSetting = {
  totalCount: 0,
  offlineThresholdSeconds: 10,
  configuredIpcIds: [],
};

export function useIpcFleetSettings() {  // ✅ accessToken 파라미터 제거
  const [setting, setSetting] = useState<IpcFleetSetting>(defaultSetting);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    setIsLoading(true);
    getIpcFleetSetting()  // ✅ accessToken 제거
      .then((nextSetting) => {
        if (!ignore) {
          setSetting(nextSetting);
          setError(null);
        }
      })
      .catch((reason) => {
        if (!ignore) {
          setError(reason instanceof Error ? reason.message : '로봇 대수 설정을 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        if (!ignore) setIsLoading(false);
      });

    return () => { ignore = true; };
  }, []);  // ✅ accessToken 의존성 제거

  const saveSetting = useCallback(async (request: UpdateIpcFleetSettingRequest) => {
    setIsSaving(true);
    setError(null);

    try {
      const nextSetting = await updateIpcFleetSetting(request);  // ✅ accessToken 제거
      setSetting(nextSetting);
      return nextSetting;
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : '로봇 대수 설정을 저장하지 못했습니다.';
      setError(message);
      throw new Error(message);
    } finally {
      setIsSaving(false);
    }
  }, []);  // ✅ accessToken 의존성 제거

  return { setting, isLoading, isSaving, error, saveSetting };
}
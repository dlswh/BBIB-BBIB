import type { IpcFleetSetting, UpdateIpcFleetSettingRequest } from '../types';
import { fetchWithAuth } from './config';

export async function getIpcFleetSetting(): Promise<IpcFleetSetting> {
  const response = await fetchWithAuth('/api/settings/ipc-fleet', {
    headers: { Accept: 'application/json' },
  });

  if (!response.ok) throw new Error(await getErrorMessage(response, '로봇 대수 설정을 불러오지 못했습니다.'));
  return normalizeIpcFleetSetting(await response.json());
}

export async function updateIpcFleetSetting(request: UpdateIpcFleetSettingRequest): Promise<IpcFleetSetting> {
  const response = await fetchWithAuth('/api/admin/settings/ipc-fleet', {
    method: 'PUT',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (!response.ok) throw new Error(await getErrorMessage(response, '로봇 대수 설정을 저장하지 못했습니다.'));
  return normalizeIpcFleetSetting(await response.json());
}

function normalizeIpcFleetSetting(body: unknown): IpcFleetSetting {
  const setting = body as IpcFleetSetting;
  return {
    ...setting,
    configuredIpcIds: Array.isArray(setting.configuredIpcIds) ? setting.configuredIpcIds : [],
  };
}

async function getErrorMessage(response: Response, fallback: string) {
  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    const body = (await response.json().catch(() => null)) as { message?: string; error?: string } | null;
    return body?.message ?? body?.error ?? fallback;
  }
  const text = await response.text().catch(() => '');
  return text || fallback;
}
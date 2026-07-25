import type { UpdateUserNameRequest, UserProfile } from '../types';
import { fetchWithAuth } from './config';

export async function getMyProfile(): Promise<UserProfile> {
  const response = await fetchWithAuth('/api/users/me', {
    headers: { Accept: 'application/json' },
  });
  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '사용자 정보를 불러오지 못했습니다.'));
  }
  return response.json() as Promise<UserProfile>;
}

export async function updateMyName(request: UpdateUserNameRequest): Promise<UserProfile> {
  const response = await fetchWithAuth('/api/users/me/name', {
    method: 'PATCH',
    headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });
  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '이름을 수정하지 못했습니다.'));
  }
  return response.json() as Promise<UserProfile>;
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
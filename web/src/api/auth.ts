import type { LoginRequest, LoginResponse } from '../types';
import { buildApiUrl } from './config';

export async function loginAdmin(credentials: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(buildApiUrl('/api/auth/login'), {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '로그인에 실패했습니다.'));
  }

  return response.json() as Promise<LoginResponse>;
}

export async function logoutAdmin(accessToken?: string) {
  if (!accessToken) return;

  const response = await fetch(buildApiUrl('/api/auth/logout'), {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '로그아웃에 실패했습니다.'));
  }
}

export async function reissueAdminToken(refreshToken: string) {
  const response = await fetch(buildApiUrl('/api/auth/reissue'), {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response, '토큰 재발급에 실패했습니다.'));
  }

  return response.json() as Promise<Pick<LoginResponse, 'accessToken' | 'refreshToken'>>;
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

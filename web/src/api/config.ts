const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim().replace(/\/+$/, '') ?? '';

export const useMockDashboard = import.meta.env.VITE_USE_MOCK === 'true';

export function buildApiUrl(path: string) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;

  if (!configuredApiBaseUrl) {
    return normalizedPath;
  }

  if (configuredApiBaseUrl.endsWith('/api') && normalizedPath.startsWith('/api/')) {
    return `${configuredApiBaseUrl}${normalizedPath.slice('/api'.length)}`;
  }

  return `${configuredApiBaseUrl}${normalizedPath}`;
}

const AUTH_STORAGE_KEY = 'smartFactory.auth';

function getStoredTokens(): { accessToken: string; refreshToken: string } | null {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!stored) return null;
  try {
    return JSON.parse(stored) as { accessToken: string; refreshToken: string };
  } catch {
    return null;
  }
}

function updateStoredTokens(accessToken: string, refreshToken: string) {
  const stored = localStorage.getItem(AUTH_STORAGE_KEY);
  if (!stored) return;
  try {
    const parsed = JSON.parse(stored) as object;
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify({ ...parsed, accessToken, refreshToken }));
  } catch {
    // 무시
  }
}

// 다른 세션 RT 사용 등으로 재발급 실패 시 강제 로그아웃 트리거
export const authEvents = new EventTarget();

let reissuePromise: Promise<{ accessToken: string; refreshToken: string }> | null = null;

export async function fetchWithAuth(path: string, options: RequestInit = {}): Promise<Response> {
  const tokens = getStoredTokens();

  const response = await fetch(buildApiUrl(path), {
    ...options,
    headers: {
      ...options.headers,
      ...(tokens ? { Authorization: `Bearer ${tokens.accessToken}` } : {}),
    },
  });

  if (response.status !== 401) return response;

  const currentTokens = getStoredTokens();
  if (!currentTokens?.refreshToken) {
    authEvents.dispatchEvent(new Event('logout'));
    return response;
  }

  // 재발급 중이면 완료될 때까지 기다렸다가 재시도
  if (!reissuePromise) {
    reissuePromise = fetch(buildApiUrl('/api/auth/reissue'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ refreshToken: currentTokens.refreshToken }),
    })
      .then(async (res) => {
        if (!res.ok) {
          authEvents.dispatchEvent(new Event('logout'));
          throw new Error('reissue failed');
        }
        const data = await res.json() as { accessToken: string; refreshToken: string };
        updateStoredTokens(data.accessToken, data.refreshToken);
        return data;
      })
      .finally(() => {
        reissuePromise = null;
      });
  }

  try {
    const { accessToken } = await reissuePromise;
    return fetch(buildApiUrl(path), {
      ...options,
      headers: {
        ...options.headers,
        Authorization: `Bearer ${accessToken}`,
      },
    });
  } catch {
    return response;
  }
}

export function getStoredAccessToken(): string {
  const stored = localStorage.getItem('smartFactory.auth');
  if (!stored) return '';
  try {
    return (JSON.parse(stored) as { accessToken: string }).accessToken ?? '';
  } catch {
    return '';
  }
}
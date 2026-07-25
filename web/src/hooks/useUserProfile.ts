import { useEffect, useState } from 'react';
import { getMyProfile, updateMyName } from '../api/user';
import type { AuthSession, UserProfile } from '../types';

export function useUserProfile(session: AuthSession) {
  const [profile, setProfile] = useState<UserProfile>(() => toProfile(session));
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdating, setIsUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;

    setProfile(toProfile(session));
    setIsLoading(true);
    setError(null);

    getMyProfile()  // ✅ session.accessToken 제거
      .then((nextProfile) => {
        if (!ignore) setProfile(nextProfile);
      })
      .catch((profileError) => {
        if (!ignore) setError(profileError instanceof Error ? profileError.message : '사용자 정보를 불러오지 못했습니다.');
      })
      .finally(() => {
        if (!ignore) setIsLoading(false);
      });

    return () => { ignore = true; };
  }, [session]);

  const updateName = async (name: string) => {
    const trimmedName = name.trim();
    if (!trimmedName) throw new Error('이름을 입력해주세요.');

    setIsUpdating(true);
    setError(null);

    try {
      const nextProfile = await updateMyName({ name: trimmedName });  // ✅ session.accessToken 제거
      setProfile(nextProfile);
      return nextProfile;
    } catch (updateError) {
      const message = updateError instanceof Error ? updateError.message : '이름을 수정하지 못했습니다.';
      setError(message);
      throw new Error(message);
    } finally {
      setIsUpdating(false);
    }
  };

  return { profile, isLoading, isUpdating, error, updateName };
}

function toProfile(session: AuthSession): UserProfile {
  return {
    username: session.username,
    name: session.name,
    role: session.role,
    enabled: true,
  };
}
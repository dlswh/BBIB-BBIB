import { FormEvent, useEffect, useState } from 'react';
import { X } from 'lucide-react';
import type { UserProfile } from '../../types';

type NameEditPageProps = {
  profile: UserProfile;
  isUpdating: boolean;
  serverError: string | null;
  onSubmit: (name: string) => Promise<void>;
  onClose: () => void;
};

export function NameEditPage({ profile, isUpdating, serverError, onSubmit, onClose }: NameEditPageProps) {
  const [name, setName] = useState(profile.name);
  const [localError, setLocalError] = useState<string | null>(null);

  useEffect(() => {
    setName(profile.name);
  }, [profile.name]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmedName = name.trim();
    if (!trimmedName) {
      setLocalError('이름을 입력해주세요.');
      return;
    }

    setLocalError(null);
    await onSubmit(trimmedName);
  };

  return (
    <aside className="account-panel" aria-labelledby="name-edit-title">
      <div className="account-panel-header">
        <div>
          <span>Profile</span>
          <h2 id="name-edit-title">이름 수정</h2>
        </div>
        <button type="button" aria-label="이름 수정 닫기" onClick={onClose}>
          <X size={18} aria-hidden="true" />
        </button>
      </div>

      <form className="account-form" onSubmit={handleSubmit}>
        <label>
          <span>아이디</span>
          <input value={profile.username} disabled />
        </label>
        <label>
          <span>이름</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength={50}
            placeholder="이름 입력"
            autoFocus
          />
        </label>

        {localError || serverError ? (
          <p className="account-alert" role="alert">
            {localError ?? serverError}
          </p>
        ) : null}

        <button className="account-primary-button" type="submit" disabled={isUpdating}>
          {isUpdating ? '저장 중...' : '저장'}
        </button>
      </form>
    </aside>
  );
}

import { X } from 'lucide-react';
import type { UserProfile } from '../../types';

type MyPagePanelProps = {
  profile: UserProfile;
  isLoading: boolean;
  error: string | null;
  onEditName: () => void;
  onClose: () => void;
};

export function MyPagePanel({ profile, isLoading, error, onEditName, onClose }: MyPagePanelProps) {
  return (
    <aside className="account-panel" aria-labelledby="my-page-title">
      <div className="account-panel-header">
        <div>
          <span>Account</span>
          <h2 id="my-page-title">마이페이지</h2>
        </div>
        <button type="button" aria-label="마이페이지 닫기" onClick={onClose}>
          <X size={18} aria-hidden="true" />
        </button>
      </div>

      {error ? <p className="account-alert">{error}</p> : null}

      <dl className="account-profile-list" aria-busy={isLoading}>
        <div>
          <dt>아이디</dt>
          <dd>{profile.username}</dd>
        </div>
        <div>
          <dt>이름</dt>
          <dd>{profile.name}</dd>
        </div>
        <div>
          <dt>권한</dt>
          <dd>{profile.role}</dd>
        </div>
        <div>
          <dt>상태</dt>
          <dd>{profile.enabled === false ? '비활성' : '활성'}</dd>
        </div>
      </dl>

      <button className="account-primary-button" type="button" onClick={onEditName}>
        정보 수정
      </button>
    </aside>
  );
}

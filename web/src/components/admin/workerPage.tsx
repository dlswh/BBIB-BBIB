import { useState, useEffect } from 'react';
import { ArrowLeft, Plus, UserX, UserCheck } from 'lucide-react';
import { getWorkers, createWorker, disableWorker, enableWorker } from '../../api/admin';
import type { WorkerUser } from '../../types';

type WorkerPageProps = {
  onBack: () => void;
};

export function WorkerPage({ onBack }: WorkerPageProps) {
  const [workers, setWorkers] = useState<WorkerUser[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateForm, setShowCreateForm] = useState(false);
  const [form, setForm] = useState({ username: '', password: '', name: '' });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    loadWorkers();
  }, []);

  async function loadWorkers() {
    try {
      setIsLoading(true);
      const data = await getWorkers();
      setWorkers(data);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '불러오기 실패');
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreate() {
    if (!form.username || !form.password || !form.name) {
      setFormError('모든 항목을 입력해주세요.');
      return;
    }
    try {
      setIsSubmitting(true);
      setFormError(null);
      await createWorker(form.username, form.password, form.name);
      setForm({ username: '', password: '', name: '' });
      setShowCreateForm(false);
      await loadWorkers();
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : '생성 실패');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDisable(id: number, username: string) {
    if (!confirm(`${username} 계정을 비활성화할까요?`)) return;
    try {
      await disableWorker(id);
      await loadWorkers();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '비활성화 실패');
    }
  }

  async function handleEnable(id: number, username: string) {
  if (!confirm(`${username} 계정을 활성화할까요?`)) return;
  try {
    await enableWorker(id);
    await loadWorkers();
  } catch (e: unknown) {
    setError(e instanceof Error ? e.message : '활성화 실패');
  }
}

  return (
    <div className="worker-page">
      <div className="worker-page-header">
        <button className="worker-back-btn" type="button" onClick={onBack}>
          <ArrowLeft size={18} />
          대시보드로
        </button>
        <div>
          <span className="worker-page-label">관리자</span>
          <h1 className="worker-page-title">작업자 관리</h1>
        </div>
        <button
          className="account-primary-button worker-add-btn"
          type="button"
          onClick={() => setShowCreateForm((v) => !v)}
        >
          <Plus size={16} />
          새 작업자 추가
        </button>
      </div>

      {showCreateForm && (
        <div className="worker-create-form account-form">
          {formError && <p className="account-alert">{formError}</p>}
          <label>
            <span>아이디</span>
            <input
              type="text"
              value={form.username}
              onChange={(e) => setForm((f) => ({ ...f, username: e.target.value }))}
              placeholder="아이디 입력"
            />
          </label>
          <label>
            <span>비밀번호</span>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
              placeholder="비밀번호 입력"
            />
          </label>
          <label>
            <span>이름</span>
            <input
              type="text"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              placeholder="이름 입력"
            />
          </label>
          <button
            className="account-primary-button"
            type="button"
            onClick={handleCreate}
            disabled={isSubmitting}
          >
            {isSubmitting ? '생성 중...' : '계정 생성'}
          </button>
        </div>
      )}

      {error && <p className="account-alert">{error}</p>}

      {isLoading ? (
        <p className="worker-loading">불러오는 중...</p>
      ) : (
        <div className="worker-table-wrapper">
          <table className="worker-table">
            <thead>
              <tr>
                <th>아이디</th>
                <th>이름</th>
                <th>상태</th>
                <th>생성일</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {workers.map((worker) => (
                <tr key={worker.id} className={!worker.enabled ? 'row-disabled' : ''}>
                  <td>{worker.username}</td>
                  <td>{worker.name}</td>
                  <td>
                    <span className={`worker-status ${worker.enabled ? 'status-active' : 'status-inactive'}`}>
                      {worker.enabled ? '활성' : '비활성'}
                    </span>
                  </td>
                  <td>{new Date(worker.createdAt).toLocaleDateString('ko-KR')}</td>
                  <td>
                    {worker.enabled ? (
                      <button
                        type="button"
                        className="worker-disable-btn"
                        onClick={() => handleDisable(worker.id, worker.username)}
                        aria-label={`${worker.username} 비활성화`}
                      >
                        <UserX size={18} />
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="worker-enable-btn"
                        onClick={() => handleEnable(worker.id, worker.username)}
                        aria-label={`${worker.username} 활성화`}
                      >
                        <UserCheck size={18} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
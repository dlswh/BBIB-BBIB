import { FormEvent, useState } from 'react';
import { ArrowLeft, LockKeyhole, Moon, ShieldCheck, Sun } from 'lucide-react';
import type { LoginRequest, ThemeMode } from '../../types';

type AdminLoginPageProps = {
  themeMode: ThemeMode;
  isSubmitting: boolean;
  error: string | null;
  onToggleTheme: () => void;
  onBack: () => void;
  onLogin: (credentials: LoginRequest) => Promise<boolean>;
  onLoginSuccess: () => void;
};

export function AdminLoginPage({
  themeMode,
  isSubmitting,
  error,
  onToggleTheme,
  onBack,
  onLogin,
  onLoginSuccess,
}: AdminLoginPageProps) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const ThemeIcon = themeMode === 'dark' ? Sun : Moon;
  const nextThemeLabel = themeMode === 'dark' ? '화이트 테마로 전환' : '블랙 테마로 전환';

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const isLoggedIn = await onLogin({ username: username.trim(), password });

    if (isLoggedIn) {
      onLoginSuccess();
    }
  };

  return (
    <div className="auth-shell">
      <header className="auth-header">
        <button className="auth-back-button" type="button" onClick={onBack}>
          <ArrowLeft size={18} aria-hidden="true" />
          랜딩으로
        </button>
        <button className="landing-icon-button" type="button" aria-label={nextThemeLabel} onClick={onToggleTheme}>
          <ThemeIcon size={19} aria-hidden="true" />
        </button>
      </header>

      <main className="auth-main">
        <section className="auth-copy" aria-labelledby="auth-copy-title">
          <span className="landing-eyebrow">
            <ShieldCheck size={18} aria-hidden="true" />
            Control Center Login
          </span>
          <h1 id="auth-copy-title">
            통합 관제 센터에
            <br />
            로그인합니다.
          </h1>
          <p>
            계정 정보를 입력해 관제센터로 접속하세요.
          </p>
        </section>

        <form className="auth-panel" onSubmit={handleSubmit}>
          <div className="auth-panel-heading">
            <LockKeyhole size={25} aria-hidden="true" />
            <div>
              <h2>관리자 로그인</h2>
            </div>
          </div>

          <label className="auth-field">
            <span>아이디</span>
            <input
              autoComplete="username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              placeholder="아이디 입력"
              required
            />
          </label>

          <label className="auth-field">
            <span>비밀번호</span>
            <input
              autoComplete="current-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="비밀번호 입력"
              required
            />
          </label>

          {error ? (
            <p className="auth-error" role="alert">
              {error}
            </p>
          ) : null}

          <button className="auth-submit-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? '로그인 중...' : '관제센터 접속'}
          </button>
        </form>
      </main>
    </div>
  );
}

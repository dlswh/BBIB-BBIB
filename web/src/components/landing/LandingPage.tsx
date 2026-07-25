import { ArrowRight, Moon, Sun } from 'lucide-react';
import { BrandLogo } from '../common/BrandLogo';
import { LandingFeatureSection } from './LandingFeatureSection';
import { LandingHero } from './LandingHero';
import type { ThemeMode } from '../../types';

type LandingPageProps = {
  themeMode: ThemeMode;
  onToggleTheme: () => void;
  onLoginClick: () => void;
};

export function LandingPage({ themeMode, onToggleTheme, onLoginClick }: LandingPageProps) {
  const ThemeIcon = themeMode === 'dark' ? Sun : Moon;
  const nextThemeLabel = themeMode === 'dark' ? '화이트 테마로 전환' : '블랙 테마로 전환';

  return (
    <div className="marketing-shell">
      <header className="landing-nav">
        <div className="landing-brand">
          <BrandLogo variant={themeMode} className="brand-logo--landing" />
        </div>
        <div className="landing-nav-actions">
          <button className="landing-icon-button" type="button" aria-label={nextThemeLabel} onClick={onToggleTheme}>
            <ThemeIcon size={19} aria-hidden="true" />
          </button>
          <button className="landing-header-button" type="button" onClick={onLoginClick}>
            관제센터 열기
            <ArrowRight size={18} aria-hidden="true" />
          </button>
        </div>
      </header>

      <main>
        <LandingHero />
        <LandingFeatureSection />
      </main>
    </div>
  );
}

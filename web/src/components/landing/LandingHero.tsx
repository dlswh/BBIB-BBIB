import { ArrowDown } from 'lucide-react';

const splineRobotArmUrl = 'https://my.spline.design/interactiverobotarm-MfW3ewQhoGdAGRNSxxeQRyez/';

export function LandingHero() {
  const handleFeatureClick = () => {
    document.getElementById('landing-capabilities')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <section className="landing-hero" aria-labelledby="landing-title">
      <div className="landing-spline" aria-label="인터랙티브 로봇암 3D 장면">
        <iframe
          title="Interactive robot arm"
          src={splineRobotArmUrl}
          loading="lazy"
          allow="autoplay; fullscreen; xr-spatial-tracking"
        />
      </div>

      <div className="landing-hero-content">
        <span className="landing-eyebrow">Enhance &amp; Ensure</span>
        <h1 id="landing-title">
          <span>기존의 시스템을 보강하여</span>
          <span>지속적인 제어를 보장합니다</span>
        </h1>
        <p>
          bluetooth 예비통신망을 보강하여 wifi없이 기계 제어가 가능합니다.
        </p>
        <div className="landing-hero-actions">
          <button className="landing-primary-button" type="button" onClick={handleFeatureClick}>
            핵심 기능 보기
            <ArrowDown size={18} aria-hidden="true" />
          </button>
        </div>
      </div>
    </section>
  );
}

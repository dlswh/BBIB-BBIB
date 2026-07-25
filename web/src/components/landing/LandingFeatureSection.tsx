import dashboardImage from '../../assets/landing/feature-dashboard.png';
import networkImage from '../../assets/landing/feature-network.png';
import tpImage from '../../assets/landing/feature-tp.png';
import { LandingFeatureCard } from './LandingFeatureCard';

const features = [
  {
    title: '이중통신',
    label: 'Dual Communication',
    description: 'Wi-Fi 장애 시 Bluetooth 예비 통신망으로 제어 흐름을 이어갑니다.',
    image: networkImage,
    imageAlt: '공장 통신망과 로봇 제어 흐름 이미지',
  },
  {
    title: 'TP',
    label: 'Teaching Pendant',
    description: '현장 조작을 표준 제어 흐름에 연결해 장비 상태를 즉시 반영합니다.',
    image: tpImage,
    imageAlt: '현장 TP 장비로 로봇을 제어하는 이미지',
  },
  {
    title: '관제센터',
    label: 'Control Center',
    description: '접속 현황, KPI, 로그를 한 화면에서 확인하고 운영 상태를 판단합니다.',
    image: dashboardImage,
    imageAlt: '대시보드 화면으로 공장 운영을 관제하는 이미지',
  },
];

export function LandingFeatureSection() {
  return (
    <section className="landing-features" id="landing-capabilities" aria-labelledby="landing-features-title">
      <div className="landing-section-heading">
        <span>Core System</span>
        <h2 id="landing-features-title">제어를 지속시키는 핵심 기능</h2>
      </div>
      <div className="landing-feature-grid">
        {features.map((feature) => (
          <LandingFeatureCard
            key={feature.title}
            title={feature.title}
            label={feature.label}
            description={feature.description}
            image={feature.image}
            imageAlt={feature.imageAlt}
          />
        ))}
      </div>
    </section>
  );
}

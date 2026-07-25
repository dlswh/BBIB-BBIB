# Web Components Guide

`web/src/components`는 화면 책임별로 나누어 관리한다.

## landing

회사 랜딩페이지 전용 컴포넌트이다.

- `LandingPage`: 랜딩 전체 조립, 상단 내비게이션, 테마 전환, 로그인 진입
- `LandingHero`: Spline 3D 로봇암 hero와 메인 CTA
- `LandingFeatureSection`: 반도체 운송 로봇, Bluetooth 이중통신, 관제센터 대시보드 메시지

## auth

관제센터 진입 전 인증 화면이다.

- `AdminLoginPage`: 관리자 로그인 form과 오류 표시
- API 호출은 직접 하지 않고 `hooks/useAuthSession.ts`에서 받은 callback만 호출한다.

## account

로그인한 사용자 프로필과 계정 메뉴 영역이다.

- `ProfileMenu`: header 우측 프로필 버튼과 펼침 메뉴
- `MyPagePanel`: 현재 사용자 정보 요약
- `NameEditPage`: 현재 사용자 이름 수정 form
- 사용자 API 호출은 `src/api/user.ts`, 상태 관리는 `src/hooks/useUserProfile.ts`가 담당한다.

## dashboard

관제센터 대시보드 영역이다.

- `DashboardShell`: 기존 대시보드 화면 조립, header/sidebar, 데이터 hook 연결
- 개별 panel 컴포넌트는 장비 상태, 접속 현황, 로그 스트림, 분포, 파이프라인 상태만 담당한다.

## layout / common

- `layout`: 앱 공통 header/sidebar
- `common`: 여러 패널에서 재사용하는 작은 UI

새 API 호출은 컴포넌트에 직접 넣지 말고 `src/api`와 `src/hooks`에 분리한다.

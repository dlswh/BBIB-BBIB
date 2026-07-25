# Smart Factory Monitoring Web

React 19, TypeScript, Vite 기반 스마트팩토리 통합 관제 대시보드입니다.

## 실행

이 프로젝트의 패키지 매니저는 `web/package.json`의 `packageManager`에 맞춰 `pnpm@10.11.0`을 사용합니다.
로컬 실행과 빌드 검증은 npm이 아니라 pnpm 명령을 우선합니다.

PowerShell/cmd:

```powershell
corepack enable
pnpm.cmd install
pnpm.cmd dev
```

Bash/WSL/Git Bash:

```bash
corepack enable
pnpm install
pnpm dev
```

기본 개발 서버는 `http://127.0.0.1:5173` 또는 `http://localhost:5173`에서 확인합니다.

## 로컬 Web + EC2 서버 API 실행

일반적인 로컬 검증은 Web만 PC에서 실행하고, Spring Boot API와 Kafka/PostgreSQL/Redis 같은 나머지 서버 구성은 EC2 배포 서버를 사용합니다.

현재 `web/.env`는 로컬 Vite proxy를 통해 EC2 서버 백엔드를 바라봅니다.

```text
VITE_USE_MOCK=false
VITE_API_BASE_URL=
API_PROXY_TARGET=https://k14s207.p.ssafy.io
```

요청 흐름은 아래와 같습니다.

```text
local browser
  -> local Vite web: http://localhost:5173/api/...
  -> Vite dev proxy
  -> EC2 Spring API: https://k14s207.p.ssafy.io/api/...
  -> EC2 Kafka/PostgreSQL/Redis
```

실행 후 아래 주소로 확인합니다.

```text
http://localhost:5173
http://localhost:5173/?dev=ipc-state
```

EC2 Spring 상태는 아래처럼 먼저 확인할 수 있습니다.

```bash
curl https://k14s207.p.ssafy.io/api/actuator/health
```

로컬 Spring을 직접 연결해야 할 때만 `API_PROXY_TARGET=http://localhost:8080`으로 바꿉니다.
`VITE_API_BASE_URL`을 비워두면 브라우저는 `/api/...` 상대 경로로 호출하고, Vite가 `API_PROXY_TARGET`으로 프록시합니다.

배포 컨테이너에서는 Vite dev server proxy가 동작하지 않습니다. 대신 `infra/compose/.env`의 `FRONTEND_API_BASE_URL` 값이 Docker build arg로 들어가고, 현재 기본값은 아래 서버 URL입니다.

```text
FRONTEND_API_BASE_URL=https://k14s207.p.ssafy.io
```

따라서 배포된 프론트는 `https://k14s207.p.ssafy.io/api/...`로 백엔드와 통신합니다.

## 화면 흐름

```text
랜딩페이지 -> 관리자 로그인 -> 통합 관제 대시보드
```

- 첫 화면은 Spline 3D 로봇암을 사용하는 회사 랜딩페이지입니다.
- `관제센터 로그인`을 누르면 관리자 로그인 화면으로 이동합니다.
- 로그인 API는 `POST /api/auth/login`을 사용합니다.
- 백엔드 기본 관리자 계정은 현재 초기 데이터 기준 `admin / 1234`입니다.
- 로그인 성공 응답의 `role`이 `ADMIN`일 때만 대시보드로 진입합니다.

## 빌드 검증

PowerShell/cmd:

```powershell
pnpm.cmd build
```

Bash/WSL/Git Bash:

```bash
pnpm build
```

## 대시보드 권장 해상도

관제 대시보드는 많은 장비 상태, 접속 현황, 로그 스트림, 데이터 파이프라인 지표를 한 화면에서 비교하도록 설계되어 있습니다.

권장 환경:

```text
해상도: 1920 x 1080 이상
브라우저 배율: 100%
용도: 발표 시연, 관제센터 데스크톱 화면, 포트폴리오 캡처
```

보조 지원:

```text
1120px 이하: 패널이 세로 흐름으로 재배치됨
760px 이하: 모바일 폭에서도 가로 스크롤 없이 핵심 정보 확인 가능
```

단, 실제 운영 관제 화면과 발표 캡처는 `1920 x 1080` 이상을 기준으로 확인하는 것을 권장합니다.

## UX 확인 기준

- 상단 KPI, 기계 현재 상태, 실시간 접속 현황, 로그 스트림, 장비 상태 분포, 데이터 파이프라인 상태가 한 화면에서 자연스럽게 읽혀야 합니다.
- 긴 한국어 라벨과 수치가 카드 경계를 넘거나 서로 겹치지 않아야 합니다.
- 상태 색상은 의미가 명확해야 합니다: 정상/운행은 green 또는 blue, 경고는 yellow, 장애는 red 계열을 사용합니다.
- 키보드 포커스가 보이고, 주요 버튼에는 접근 가능한 이름이 있어야 합니다.
- 화면 수정 후 `pnpm.cmd build`와 브라우저 캡처 확인을 함께 수행합니다.

## CI/CD 확인

- `web/**` 변경 시 frontend Jenkins 배포 파이프라인이 실행되는지 확인합니다.

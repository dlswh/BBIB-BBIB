# 삡삡 (BBIPBBIP) - Backend

반도체·2차전지 물류자동화 장비 통합 관제 웹 서비스

## 프로젝트 소개

TP(Android) → IPC → Motor 제어 구조에서
- Wi-Fi / Bluetooth 이중 통신 지원
- IPC에서 EtherCAT으로 모터 제어
- 웹 기반 실시간 통합 관제 대시보드 제공

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| DB | PostgreSQL |
| Cache | Redis |
| Message Broker | Kafka |
| Build | Gradle |
| Auth | Spring Security, JWT |

## 주요 기능

### 실시간 모니터링
- Kafka 기반 IPC 로그 수집
- 모터 상태 및 위치 실시간 조회
- 통합 관제 대시보드

### 인증 및 계정 관리
- JWT 기반 인증 (Access Token / Refresh Token)
- Redis 기반 동시 로그인 차단
- RBAC (ADMIN / WORKER 역할 분리)
- ADMIN 전용 계정 관리 백오피스
- WebSocket 기반 실시간 접속 현황

## 프로젝트 구조

```
com.example.backend
├── domain
│   ├── auth        # 로그인/로그아웃/토큰 재발급
│   └── user        # 계정 관리
├── global
│   ├── config      # 설정 파일
│   ├── jwt         # JWT 관련
│   ├── security    # Spring Security 관련
│   ├── exception   # 예외 처리
│   └── response    # 공통 응답 형식
└── BackendApplication.java
```

## 시스템 아키텍처

```
[TP Android]
↕ Wi-Fi TCP / Bluetooth
[IPC]  →  EtherCAT  →  [Motor]
↓ Kafka
[Backend Server]
↕ WebSocket
[Web 관제 대시보드]
```

 
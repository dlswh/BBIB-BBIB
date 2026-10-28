# 삡삡 (BBIB-BBIB)

산업용 물류자동화장비를 Wi-Fi/Bluetooth 이중 통신으로 제어하는 TP(Teach Pendant) 앱과 관제 웹서비스

- 기간: 2026.04.06 ~ 05.21 (7주)
- 인원: 6명
- 역할: Android 앱, Web 백엔드(인증·세션)

<img width="426" height="240" alt="BBIB-BBIB_example_video" src="https://github.com/user-attachments/assets/c66b5d15-a63e-466c-9c6e-bb6b1a91fd66" />

## 구조

TP 앱에서 IPC로 명령을 보내 모터를 제어합니다.

```
Android TP 앱 ──(Wi-Fi TCP / Bluetooth)──▶ IPC(Windows) ──(EtherCAT)──▶ 모터
```

## 주요 기능

- Wi-Fi(TCP)와 Bluetooth 이중 통신, Heartbeat 기반 연결 끊김 감지
- 절대/상대 이동, STOP 등 모터 제어 명령 전송
- 로그 기간별·키워드 검색
- 로그인, 오프라인 모드
- 관제 웹: 접속 세션 조회, 로그인/로그아웃 이벤트 실시간 반영

## 기술 스택

| 구분 | 기술 |
|---|---|
| Android | Kotlin, Jetpack Compose, Room, Retrofit, Hilt |
| Backend | Spring Boot, JWT, Redis, WebSocket, PostgreSQL |
| Web | React |
| IPC / 모터 | C#, WMX3, EtherCAT |
| Infra | Docker |

## 담당 범위

**Android**
- `TcpChannel` 구현 (연결, 전송, 수신, 해제), 1초 간격 Heartbeat와 3초 타임아웃으로 연결 끊김 감지
- Wi-Fi/Bluetooth 통합 연결 상태 모델 설계, `StateFlow`로 상태 노출
- 로그 검색, 로그인·인증, 오프라인 모드 분기

**Backend**
- JWT 인증, Access/Refresh Token 발급, Refresh Token Rotation
- 중복 로그인 강제 로그아웃, 로그아웃 토큰 블랙리스트
- 회원 생성·soft-delete, 접속 세션 조회, WebSocket 세션 이벤트 브로드캐스트

> 원 프로젝트에서 커밋 히스토리 없이 옮겨 온 레포입니다.

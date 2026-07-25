# TP Android App

## 📌 Overview

TP는 IPC와의 통신을 통해 장비를 제어하고 상태를 모니터링하는 Android 애플리케이션입니다.
Wi-Fi(TCP) 및 Bluetooth 기반 연결을 지원하며, 사용자 입력에 따라 제어 명령을 전송하고 결과를 실시간으로 반영합니다.

---

## 🚀 Features

* Wi-Fi (IP / Port) 기반 TCP 연결
* Bluetooth 페어링 및 연결
* 제어 명령 전송 (예: 이동, 회전 등)
* 실시간 상태 모니터링
* 연결 재시도 및 오류 상태 처리

---

## 🏗️ Project Structure

```
android/app/src/main/java/com/s14p31s207/android
├─ app/navigation
├─ feature
│  ├─ connection/{ui, vm}
│  ├─ control/{ui, vm}
│  └─ monitor/{ui, vm}
├─ data
│  ├─ comm/{api, tcp, bluetooth}
│  ├─ repository
│  └─ di
├─ domain
│  ├─ model
│  └─ repository
└─ ui
```

### 📂 Description

* `feature`: 화면 단위 UI 및 ViewModel
* `data/comm`: 통신 모듈 (TCP, Bluetooth)
* `repository`: 데이터 흐름 및 통신 연결 관리
* `domain/model`: 공통 모델 및 DTO
* `ui`: 공통 테마 및 UI 요소

---

## 🧠 Architecture

```
UI → ViewModel → Repository → Communication Layer → IPC
```

### 구성 요소 설명

* **UI**: 사용자 입력 및 상태 표시
* **ViewModel**: UI 상태 관리 및 이벤트 처리
* **Repository**: 비즈니스 로직 및 데이터 흐름 제어
* **Communication Layer**: TCP / Bluetooth 기반 송수신 처리
* **IPC**: 실제 장비 또는 외부 제어 시스템

---

## 🔌 Communication Protocol

### 📤 Request Example

```json
{
  "cmd": "MOVE_ABSOLUTE_WHEEL",
  "id": "req-001",
  "data": {
    "position": 0.0,
    "speed": 3.0
  }
}
```

### 📌 Message Rules

* `cmd`: 메시지 종류 (MOVE_ABSOLUTE_WHEEL, MOVE_RELATIVE_WHEEL, STOP_WHEEL 등)
* `id`: 요청-응답 매칭을 위한 ID
* `data`: 메시지 데이터 (type에 따라 구조 변경)

---

## ⚙️ How to Run

1. Android Studio에서 프로젝트 실행
2. Gradle Sync 완료
3. 에뮬레이터 또는 실제 디바이스 연결
4. Connection 화면  
   4-1. wifi 선택 시 -> IP / Port 입력  
   4-2. bluetooth 선택 시 -> 페어링
5. Control 화면에서 wifi/bluetooth 및 IPC 선택
6. Control 화면에서 명령 전송

---

## 🔁 Data Flow

```
[UI]
  ↓
ViewModel
  ↓
DTO 생성
  ↓
JSON 변환
  ↓
Socket 전송
  ↓
IPC 처리
  ↓
응답 수신
  ↓
DTO 변환
  ↓
Repository
  ↓
ViewModel
  ↓
UI 업데이트
```

---

## 📎 TODO

* [ ] 공통 DTO 및 데이터 모델 설계
* [ ] TCP / Bluetooth 통신 모듈 구현
* [ ] 연결 기능 구현 (Wi-Fi, Bluetooth)
* [ ] 명령 전송 및 응답 처리 로직 구현
* [ ] 상태 모니터링 기능 구현
* [ ] 연결 상태 및 에러 처리 로직 구현
* [ ] 재시도 및 안정성 개선
* [ ] 로그 및 디버깅 시스템 구축
* [ ] UI 연동 및 사용자 경험 개선

---
 
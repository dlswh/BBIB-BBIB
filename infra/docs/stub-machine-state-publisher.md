## Stub Machine State Publisher

이 문서는 관제 웹의 기계 현재 상태 화면을 검증하기 위한 stub machine state publisher 실행 방법을 정리합니다.

이 컨테이너는 실제 IPC 대신 Kafka `ipc.motor.state` topic으로 `STATE` 메시지를 주기적으로 발행합니다.

## Current Scenario

기본 발행 대상:

```text
v-ipc-1
v-ipc-2
v-ipc-3
v-ipc-5
```

`v-ipc-4`는 메시지를 발행하지 않습니다. 관제 웹에서 `OFFLINE` 상태 검증용으로 사용합니다.

기본 상태 주기:

```text
MOVING 5s -> READY 2s -> JOGGING 4s -> READY 2s -> repeat
```

기본 시작 상태:

```text
v-ipc-1: MOVING
v-ipc-2: READY
v-ipc-3: JOGGING
v-ipc-5: READY
```

## Deploy After Dev Merge

브랜치가 `dev`에 머지된 뒤 EC2 또는 WSL 실행 환경에서 repo를 최신화합니다.

EC2 예시:

```bash
cd ~/src/S14P31S207
git fetch origin
git switch dev
git pull --ff-only origin dev
```

WSL 예시:

```bash
cd ~/home/ssafy/S14P31S207
git fetch origin
git switch dev
git pull --ff-only origin dev
```

root 계정에서 WSL repo를 사용하는 경우 실제 경로가 다를 수 있습니다.

```bash
cd /root/home/ssafy/S14P31S207
```

## Start Or Restart

기존 컨테이너를 내리고 새 이미지로 재빌드합니다.

```bash
docker compose -p s14p31s207-sse-test \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.sse-test.yml \
  down

sh infra/scripts/up-sse-test.sh
```

`up-sse-test.sh`는 내부적으로 아래 작업을 수행합니다.

```text
1. infra/compose/.env 없으면 .env.example에서 생성
2. Kafka topic 생성 확인
3. fake IPC state publisher 이미지 빌드
4. s207-fake-ipc-state-pub 컨테이너 실행
```

## Verify

컨테이너 상태:

```bash
docker ps --filter name=s207-fake-ipc-state-pub
```

publisher 로그:

```bash
docker logs -f s207-fake-ipc-state-pub
```

정상 시작 로그에는 아래 값이 포함되어야 합니다.

```text
intervalMs=1000
ipcIds=v-ipc-1,v-ipc-2,v-ipc-3,v-ipc-5
stateSequence=MOVING,MOVING,MOVING,MOVING,MOVING,READY,READY,JOGGING,JOGGING,JOGGING,JOGGING,READY,READY
stateOffsets=0,5,7,11
```

아래 값이 보이면 이전 이미지 또는 이전 설정이 실행 중인 것입니다.

```text
intervalMs=3000
ipcIds=v-ipc-1,v-ipc-2,v-ipc-3
E_STOPPED
ERROR
v-ipc-4
```

Kafka topic 메시지 확인:

```bash
docker run --rm -it apache/kafka:3.9.1 \
  /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server k14s207.p.ssafy.io:9092 \
  --topic ipc.motor.state \
  --property print.key=true \
  --property key.separator=" => "
```

## Override

필요하면 `infra/compose/.env` 또는 shell 환경변수로 기본 시나리오를 바꿀 수 있습니다.

```dotenv
FAKE_IPC_IDS=v-ipc-1,v-ipc-2,v-ipc-3,v-ipc-5
FAKE_IPC_INTERVAL_MS=1000
FAKE_IPC_KAFKA_BOOTSTRAP_SERVERS=k14s207.p.ssafy.io:9092
FAKE_IPC_KAFKA_TOPIC=ipc.motor.state
FAKE_IPC_STATE_SEQUENCE=MOVING,MOVING,MOVING,MOVING,MOVING,READY,READY,JOGGING,JOGGING,JOGGING,JOGGING,READY,READY
FAKE_IPC_STATE_OFFSETS=0,5,7,11
```

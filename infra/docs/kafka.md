## Kafka

이 문서는 Kafka single-node KRaft 구성과 검증 절차를 정리합니다.

Kafka는 IPC, backend, dashboard 확장 사이에서 이벤트 스트리밍을 담당할 후보입니다.

## Architecture

현재 구성은 single-node KRaft입니다.

```text
kafka-data-init -> kafka -> kafka-ui
```

Listener 기준:

| Listener | Address | Usage |
| :-- | :-- | :-- |
| `PLAINTEXT_HOST` | `k14s207.p.ssafy.io:9092` | External IPC producer and local backend validation |
| `PLAINTEXT` | `kafka:19092` | Docker network 내부, Kafka UI와 backend 연결 |
| `CONTROLLER` | `kafka:29093` | KRaft controller 내부 통신 |

Kafka는 공식 이미지가 `appuser(uid=1000)`로 실행됩니다. Docker named volume은 처음 생성될 때 root 소유가 될 수 있으므로, `kafka-data-init` 서비스가 `s207-kafka-data` volume 권한을 `1000:1000`으로 맞춘 뒤 Kafka를 실행합니다.

## Environment

```dotenv
KAFKA_CLUSTER_ID=4L6g3nShT-eMCtK--X86sw
KAFKA_BIND=0.0.0.0
KAFKA_EXTERNAL_HOST=k14s207.p.ssafy.io
KAFKA_PORT=9092
KAFKA_UI_PORT=8085
```

`KAFKA_CLUSTER_ID`는 `s207-kafka-data` volume이 처음 생성될 때 사용됩니다. 이미 Kafka volume이 있는 상태에서 이 값을 바꾸면 cluster id mismatch가 날 수 있습니다.

## Startup

실행:

```bash
sh infra/scripts/up-kafka.sh
```

직접 실행:

```bash
docker compose -p s14p31s207-kafka \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.kafka.yml \
  up -d
```

상태 확인:

```bash
docker compose -p s14p31s207-kafka \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.kafka.yml \
  ps

docker ps -a --filter name=s207-kafka-data-init
docker logs s207-kafka --tail=100
docker logs s207-kafka-ui --tail=100
```

`s207-kafka-data-init`이 `Exited (0)`, `s207-kafka`가 `healthy`, `s207-kafka-ui`가 `Up`이면 정상입니다.

## Topic Verification

topic 생성:

```bash
docker exec -it s207-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic machine-state-events --partitions 3 --replication-factor 1
docker exec -it s207-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic command-result-events --partitions 3 --replication-factor 1
docker exec -it s207-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic alarm-events --partitions 3 --replication-factor 1
docker exec -it s207-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic edge-connection-events --partitions 3 --replication-factor 1
```

topic 목록 확인:

```bash
docker exec -it s207-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

produce:

```bash
echo '{"machineId":"machine-001","state":"IDLE"}' | docker exec -i s207-kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic machine-state-events
```

consume:

```bash
docker exec -it s207-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic machine-state-events --from-beginning --max-messages 1
```

아래 메시지가 보이면 정상입니다.

```json
{"machineId":"machine-001","state":"IDLE"}
```

## External Kafka Verification

EC2 Kafka is expected to be reachable from local/WSL producer and backend containers at:

```text
k14s207.p.ssafy.io:9092
```

Verify from local WSL:

```bash
docker run --rm apache/kafka:3.9.1 \
  /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server k14s207.p.ssafy.io:9092 \
  --list
```

Run the local backend against the external Kafka broker:

```bash
sh infra/scripts/up-backend-external-kafka.sh
```

## Kafka UI

Windows에서 SSH tunnel을 열고 아래 주소로 접근합니다.

```text
http://localhost:18085
```

확인할 항목:

- Cluster 이름이 `s207-local`인지 확인
- `machine-state-events` topic이 보이는지 확인
- produced message가 topic message viewer에서 보이는지 확인

## Reset

Kafka volume까지 초기화해야 할 때만 사용합니다. 기존 topic과 message가 삭제됩니다.

```bash
docker compose -p s14p31s207-kafka \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.kafka.yml \
  down

docker volume rm s207-kafka-data
sh infra/scripts/up-kafka.sh
```

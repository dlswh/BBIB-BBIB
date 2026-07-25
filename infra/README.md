## Infra Guide

이 디렉터리는 Jenkins, Docker Compose 기반 개발/검증 인프라, 모니터링, 스토리지 설정을 관리합니다.

EC2에서는 Jenkins가 `127.0.0.1:8080`을 사용합니다. 추가 컨테이너도 기본적으로 `127.0.0.1`에만 바인딩하고, 외부 공개는 native Nginx reverse proxy를 통해 필요한 서비스만 엽니다.

## Documents

세부 실행 가이드는 1-depth 문서로만 나눕니다.

| Document | Scope |
| :-- | :-- |
| [applications.md](docs/applications.md) | Frontend, Backend, Nginx, Jenkins job |
| [local-dev-infra-setup.md](docs/local-dev-infra-setup.md) | 로컬 개발 인프라 설정과 Frontend/Spring 실행 |
| [data-services.md](docs/data-services.md) | MinIO, PostgreSQL, Redis |
| [stub-machine-state-publisher.md](docs/stub-machine-state-publisher.md) | Stub machine state publisher |
| [monitoring.md](docs/monitoring.md) | Node Exporter, Prometheus, Grafana |
| [kafka.md](docs/kafka.md) | Kafka, Kafka UI, topic 검증 |

## Port Plan

| Service | Host bind | Container port | Purpose | Public exposure |
| :-- | :-- | :-- | :-- | :-- |
| Jenkins | `127.0.0.1:8080` | `8080` | CI/CD controller | Nginx `/jenkins/` only |
| Frontend Web | `127.0.0.1:3001` | `80` | React/Vite static web | Nginx `/` only |
| Backend API | `127.0.0.1:8000` | `8080` | Spring Boot API | Nginx `/api/` only |
| PostgreSQL | `127.0.0.1:5432` | `5432` | Application database | No |
| Redis | `127.0.0.1:6379` | `6379` | Cache, online TTL | No |
| Kafka | `${KAFKA_BIND:-0.0.0.0}:9092` | `9092` | Event streaming broker | External IPC/test producer access via `k14s207.p.ssafy.io:9092` |
| Kafka UI | `127.0.0.1:8085` | `8080` | Kafka topic/message viewer | Optional admin only |
| Node Exporter | `127.0.0.1:9100` | `9100` | EC2 host metrics exporter | No |
| Prometheus | `127.0.0.1:9090` | `9090` | Metrics collection | Optional admin only |
| Grafana | `127.0.0.1:3000` | `3000` | Metrics dashboard | Optional admin only |
| MinIO API | `127.0.0.1:9000` | `9000` | S3-compatible object storage | No |
| MinIO Console | `127.0.0.1:9001` | `9001` | MinIO admin console | Optional admin only |

## Compose Policy

인프라 컨테이너는 한 번에 전부 띄우지 않고, 서비스 단위 compose 파일로 나눠서 하나씩 실행하고 검증합니다.

| Compose file | Service | Project name |
| :-- | :-- | :-- |
| `infra/compose/docker-compose.storage.yml` | MinIO | `s14p31s207-storage` |
| `infra/compose/docker-compose.database.yml` | PostgreSQL | `s14p31s207-database` |
| `infra/compose/docker-compose.cache.yml` | Redis | `s14p31s207-cache` |
| `infra/compose/docker-compose.node-exporter.yml` | Node Exporter | `s14p31s207-node-exporter` |
| `infra/compose/docker-compose.prometheus.yml` | Prometheus | `s14p31s207-prometheus` |
| `infra/compose/docker-compose.grafana.yml` | Grafana | `s14p31s207-grafana` |
| `infra/compose/docker-compose.kafka.yml` | Kafka, Kafka UI | `s14p31s207-kafka` |
| `infra/compose/docker-compose.frontend.yml` | Frontend Web | `s14p31s207-frontend` |
| `infra/compose/docker-compose.backend.yml` | Backend API | `s14p31s207-backend` |
| `infra/compose/docker-compose.backend.external-kafka.yml` | Backend API external Kafka override | `s14p31s207-backend` |

원칙은 아래와 같습니다.

- compose 파일 하나는 검증 단위 하나만 담당합니다.
- 각 compose project는 `-p`로 명시해서 orphan container 경고를 피합니다.
- 각 서비스는 자기 named volume을 사용합니다.
- 외부 공개는 하지 않고 `127.0.0.1`에만 bind합니다.
- PostgreSQL, Redis, Kafka, MinIO, Backend API는 `s207-app` 공용 Docker network에 붙입니다.
- 동시에 host bind는 유지하므로 Windows local에서는 SSH tunnel로 `localhost:<tunnel-port>` 접근이 가능합니다.
- Backend container는 내부 서비스 접근 시 host port가 아니라 Docker DNS를 사용합니다. 예: `s207-postgres:5432`, `s207-redis:6379`, `kafka:19092`, `s207-minio:9000`.
- Backend가 서버 Kafka를 직접 읽어야 하는 로컬 검증에서는 `infra/scripts/up-backend-external-kafka.sh`를 사용합니다. 이 스크립트는 같은 backend container를 띄우되 Kafka bootstrap만 `k14s207.p.ssafy.io:9092`로 바꿉니다.
- `infra/scripts/up-*.sh`와 `infra/scripts/up-*.ps1`은 `s207-app` network를 자동 생성합니다. 직접 `docker compose`를 실행한다면 먼저 `docker network create s207-app`을 한 번 실행합니다.

## Local Web With EC2 Server Stack

프론트엔드 화면만 로컬에서 수정하고, Spring Boot API와 Kafka/PostgreSQL/Redis는 EC2 배포 서버를 사용하는 흐름을 기본 로컬 검증 방식으로 둡니다.

```text
local browser
  -> local Vite Web: http://localhost:5173/api/...
  -> Vite dev proxy
  -> EC2 Spring API: https://k14s207.p.ssafy.io/api/...
  -> EC2 Kafka/PostgreSQL/Redis
```

이 방식에서는 로컬 PC에서 backend, Kafka, PostgreSQL, Redis 컨테이너를 띄울 필요가 없습니다. `web/.env`만 아래처럼 설정합니다.

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=
API_PROXY_TARGET=https://k14s207.p.ssafy.io
```

로컬 Spring을 직접 디버깅해야 하는 경우에만 `infra/scripts/up-backend-external-kafka.sh`를 사용하고, Web의 `API_PROXY_TARGET`을 로컬 Spring 주소로 바꿉니다.

## EC2 Full Startup Order

EC2에서 전체 컨테이너를 다시 띄울 때는 repo 최신화 후 아래 순서로 실행합니다. 핵심은 backend보다 PostgreSQL, Redis, Kafka, MinIO를 먼저 띄우는 것입니다.

```bash
cd ~/src/S14P31S207
git pull

sh infra/scripts/up-storage.sh
sh infra/scripts/up-database.sh
sh infra/scripts/up-cache.sh
sh infra/scripts/up-node-exporter.sh
sh infra/scripts/up-prometheus.sh
sh infra/scripts/up-grafana.sh
sh infra/scripts/up-kafka.sh
sh infra/scripts/up-backend.sh
```

Frontend까지 수동으로 다시 띄워야 한다면 아래 명령을 추가로 실행합니다. 일반적으로는 Jenkins `frontend-dev-deploy` job이 이 역할을 담당합니다.

```bash
docker compose -p s14p31s207-frontend \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.frontend.yml \
  up -d --build
```

실행 후 전체 상태는 아래처럼 확인합니다.

```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
docker network inspect s207-app --format '{{range .Containers}}{{.Name}}{{"\n"}}{{end}}' | sort
curl -fsS http://127.0.0.1:8000/api/actuator/health
```

## SSH Tunnel

EC2의 인프라 포트는 기본적으로 `127.0.0.1`에만 바인딩합니다. Windows PC에서 관리 화면에 접근하려면 SSH tunnel을 열고, 브라우저 또는 DB client에서 `localhost`로 접속합니다.

Windows `cmd.exe` 기준 전체 터널링 명령은 아래와 같습니다.

```cmd
ssh -i "C:\Users\SSAFY\Documents\K14S207T.pem" -L 18080:127.0.0.1:8080 -L 13001:127.0.0.1:3001 -L 18000:127.0.0.1:8000 -L 15432:127.0.0.1:5432 -L 16379:127.0.0.1:6379 -L 19092:127.0.0.1:9092 -L 18085:127.0.0.1:8085 -L 19100:127.0.0.1:9100 -L 19090:127.0.0.1:9090 -L 13000:127.0.0.1:3000 -L 19000:127.0.0.1:9000 -L 19001:127.0.0.1:9001 ubuntu@k14s207.p.ssafy.io
```

터널을 연 뒤 로컬 PC에서 아래 주소로 접속합니다.

| Service | Local URL |
| :-- | :-- |
| Jenkins | `http://localhost:18080/jenkins/` |
| Frontend Web | `http://localhost:13001` 또는 HTTPS `https://k14s207.p.ssafy.io` |
| Backend API | `http://localhost:18000/api/actuator/health` 또는 HTTPS `https://k14s207.p.ssafy.io/api/actuator/health` |
| PostgreSQL | `localhost:15432` |
| Redis | `localhost:16379` |
| Kafka | Direct external producer/bootstrap: `k14s207.p.ssafy.io:9092`; SSH tunnel fallback: `localhost:19092` |
| Kafka UI | `http://localhost:18085` |
| Node Exporter | `http://localhost:19100/metrics` |
| Prometheus | `http://localhost:19090` |
| Grafana | `http://localhost:13000` |
| MinIO API | `http://localhost:19000` |
| MinIO Console | `http://localhost:19001` |

## Data Services

이 문서는 애플리케이션이 사용하는 데이터/스토리지 계층을 정리합니다.

대상은 아래 세 개입니다.

- MinIO: S3-compatible object storage
- PostgreSQL: application database
- Redis: cache, online TTL, fast read 후보

모든 서비스는 외부 공개 없이 `127.0.0.1`에만 바인딩합니다. Windows PC에서는 SSH tunnel을 통해 접근합니다.

## MinIO

MinIO는 Jenkins artifact, 검증 리포트, 부하테스트 결과를 저장하기 위한 S3-compatible storage입니다.

환경 변수:

```dotenv
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001
MINIO_ROOT_USER=s207-minio-admin
MINIO_ROOT_PASSWORD=change-this-minio-password
```

실행:

```bash
sh infra/scripts/up-storage.sh
```

직접 실행:

```bash
docker compose -p s14p31s207-storage \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.storage.yml \
  up -d
```

검증:

```bash
docker compose -p s14p31s207-storage \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.storage.yml \
  ps

curl -fsS http://127.0.0.1:9000/minio/health/live
```

권장 bucket:

| Bucket | Usage |
| :-- | :-- |
| `s207-artifacts` | Jenkins build artifact 후보 |
| `s207-reports` | 테스트/검증 리포트 |
| `s207-loadtest` | k6, Grafana screenshot, 부하테스트 결과 |

MinIO bucket은 compose가 자동 생성하지 않습니다. Console에서 필요할 때 직접 생성합니다.

## PostgreSQL

PostgreSQL은 애플리케이션 기본 데이터베이스입니다. 실제 데이터는 Docker named volume `s207-postgres-data`에 저장합니다.

환경 변수:

```dotenv
POSTGRES_DB=s207_app
POSTGRES_USER=s207_app
POSTGRES_PASSWORD=change-this-postgres-password
```

실행:

```bash
sh infra/scripts/up-database.sh
```

직접 실행:

```bash
docker compose -p s14p31s207-database \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.database.yml \
  up -d
```

검증:

```bash
docker compose -p s14p31s207-database \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.database.yml \
  ps

docker exec -it s207-postgres pg_isready -U s207_app -d s207_app
docker exec -it s207-postgres psql -U s207_app -d s207_app
```

Windows DB client 접속 값:

| Field | Value |
| :-- | :-- |
| Host | `localhost` |
| Port | `15432` |
| Database | `s207_app` |
| Username | `s207_app` |
| Password | `infra/compose/.env`의 `POSTGRES_PASSWORD` |

초기화 SQL:

```text
infra/postgres/init/01-init.sql
```

`s207-postgres-data` volume이 이미 있으면 init SQL은 다시 실행되지 않습니다.

## Redis

Redis는 캐시, 장비 online/offline TTL, dashboard fast read 용도로 사용할 후보입니다. 현재는 단일 비밀번호와 localhost bind로 보호합니다.

환경 변수:

```dotenv
REDIS_PASSWORD=change-this-redis-password
```

실행:

```bash
sh infra/scripts/up-cache.sh
```

직접 실행:

```bash
docker compose -p s14p31s207-cache \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.cache.yml \
  up -d
```

검증:

```bash
docker compose -p s14p31s207-cache \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.cache.yml \
  ps

export REDIS_PASSWORD=$(grep '^REDIS_PASSWORD=' infra/compose/.env | cut -d '=' -f2-)
docker exec -it s207-redis redis-cli ping
docker exec -it s207-redis redis-cli -a "$REDIS_PASSWORD" ping
```

비밀번호 없이 접근하면 `NOAUTH Authentication required.`가 나와야 정상입니다. 비밀번호를 사용하면 `PONG`이 나와야 합니다.

간단한 읽기/쓰기 검증:

```bash
docker exec -it s207-redis redis-cli -a "$REDIS_PASSWORD" set s207:ping ok
docker exec -it s207-redis redis-cli -a "$REDIS_PASSWORD" get s207:ping
```

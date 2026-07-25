## Local Development Infra Setup

이 문서는 로컬 개발을 위해 WSL/Docker 기반 인프라를 준비하고, React/Vite 프론트엔드와 Spring Boot API를 함께 실행하는 방법을 정리합니다.

목표 흐름은 아래와 같습니다.

```text
Browser
  -> http://localhost:5173/api/...
  -> Vite dev proxy
  -> http://localhost:8080/api/...
  -> Local Spring Boot
  -> WSL Docker PostgreSQL / Redis / Kafka
```

## Prerequisites

WSL 또는 Docker Desktop에 기반 인프라가 먼저 떠 있어야 합니다.

필요 컨테이너:

```text
s207-postgres   127.0.0.1:5432 -> 5432
s207-redis      127.0.0.1:6379 -> 6379
s207-kafka      0.0.0.0:9092   -> 9092
s207-kafka-ui   127.0.0.1:8085 -> 8080
```

WSL에서 띄우는 경우:

```bash
cd ~/home/ssafy/S14P31S207
sh infra/scripts/up-database.sh
sh infra/scripts/up-cache.sh
sh infra/scripts/up-kafka.sh
docker ps
```

Windows PowerShell에서 포트 접근 확인:

```powershell
Test-NetConnection localhost -Port 5432
Test-NetConnection localhost -Port 6379
Test-NetConnection localhost -Port 9092
Test-NetConnection localhost -Port 8085
```

모두 `TcpTestSucceeded: True`가 나와야 합니다.

## Env 기준

로컬 Spring 직접 실행은 `infra/compose/.env`를 기준으로 합니다.

```text
infra/compose/.env
```

이 파일은 Git에 올리지 않는 로컬 파일입니다. 없으면 `infra/compose/.env.example`에서 복사합니다.

중요한 값:

```dotenv
POSTGRES_DB=...
POSTGRES_USER=...
POSTGRES_PASSWORD=...
REDIS_PASSWORD=...
KAFKA_PORT=9092
JWT_SECRET=...
```

주의:

```text
Spring을 Windows repo에서 실행하면 Windows repo의 infra/compose/.env를 읽습니다.
인프라 컨테이너를 WSL repo에서 띄웠다면 WSL repo의 infra/compose/.env와 값이 맞아야 합니다.
특히 PostgreSQL user/password/db가 다르면 Spring DB 연결이 실패합니다.
```

## Run Local Spring

Windows PowerShell:

```powershell
.\infra\scripts\run-backend-local.ps1
```

이 스크립트는 `infra/compose/.env`를 읽고 Spring 표준 환경 변수로 변환합니다.

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/${POSTGRES_DB}
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_SECRET=${JWT_SECRET}
```

Gradle daemon이 예전 환경 변수를 재사용하지 않도록 `bootRun --no-daemon`으로 실행합니다.

Spring 확인:

```powershell
curl.exe http://localhost:8080/api/actuator/health
```

Swagger:

```text
http://localhost:8080/api/swagger-ui/index.html
```

## Run Local Frontend Against Local Spring

`web/.env`를 로컬 Spring용으로 둡니다.

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=
API_PROXY_TARGET=http://localhost:8080
```

프론트 실행:

```powershell
cd web
npm.cmd install
npm.cmd run dev
```

브라우저:

```text
http://localhost:5173
```

프론트 코드는 `/api/auth/login`처럼 상대 경로로 호출합니다. 브라우저 Network 탭에는 아래처럼 보입니다.

```text
http://localhost:5173/api/auth/login
```

하지만 실제 요청은 Vite proxy가 로컬 Spring으로 넘깁니다.

```text
http://localhost:8080/api/auth/login
```

`API_PROXY_TARGET`에는 `/api`를 붙이지 않습니다. Spring Boot가 이미 `server.servlet.context-path=/api`로 실행되기 때문입니다.

## One Shot Scripts

아래 스크립트는 `web/.env`를 로컬 Spring 테스트용으로 바꾼 뒤, Spring Boot와 Vite dev server를 함께 실행합니다.

Windows `cmd.exe`:

```cmd
infra\scripts\run-local-frontend-backend.cmd
```

이 스크립트는 Spring Boot와 Vite를 각각 새 `cmd` 창으로 띄웁니다.

Linux/WSL:

```bash
sh infra/scripts/run-local-frontend-backend.sh
```

이 스크립트는 현재 터미널에서 Spring Boot와 Vite를 background process로 띄우고, `Ctrl+C` 입력 시 함께 종료합니다.

## Switch Back To Server Spring

로컬 프론트에서 배포 서버 Spring을 바라보려면 `web/.env`를 아래처럼 둡니다.

```dotenv
VITE_USE_MOCK=false
VITE_API_BASE_URL=
API_PROXY_TARGET=https://k14s207.p.ssafy.io
```

이 경우 브라우저에는 여전히 `http://localhost:5173/api/...`로 보이지만, Vite proxy가 서버로 넘깁니다.

## Deployment Behavior

배포된 프론트 컨테이너는 Vite dev server를 사용하지 않습니다. 따라서 `API_PROXY_TARGET`도 사용하지 않습니다.

배포 빌드는 `infra/compose/.env`의 값을 Docker build arg로 넣습니다.

```dotenv
FRONTEND_API_BASE_URL=https://k14s207.p.ssafy.io
```

배포 후 요청 흐름:

```text
Browser
  -> https://k14s207.p.ssafy.io/api/...
  -> native Nginx
  -> 127.0.0.1:8000/api/...
  -> Server Spring Boot
```

즉 로컬 테스트와 배포의 차이는 아래와 같습니다.

| Environment | Frontend URL | API target |
| :-- | :-- | :-- |
| Local frontend + local Spring | `http://localhost:5173` | `API_PROXY_TARGET=http://localhost:8080` |
| Local frontend + server Spring | `http://localhost:5173` | `API_PROXY_TARGET=https://k14s207.p.ssafy.io` |
| Deployed frontend | `https://k14s207.p.ssafy.io` | `FRONTEND_API_BASE_URL=https://k14s207.p.ssafy.io` |

## Troubleshooting

| Symptom | Check |
| :-- | :-- |
| `Connection refused` to DB/Redis/Kafka | WSL Docker containers are running and Windows can reach `localhost:5432`, `6379`, `9092` |
| Spring DB login failure | Windows `infra/compose/.env` values match the WSL compose env used to create PostgreSQL |
| Browser request shows `localhost:5173` | Normal. Vite proxy receives the request first, then forwards to Spring |
| Local Spring API still returns server data | Check `web/.env` has `API_PROXY_TARGET=http://localhost:8080` and restart `npm.cmd run dev` |
| `PATCH /api/users/me/name` fails before controller | Confirm current backend branch includes `PATCH` in CORS allowed methods |
| Swagger works but frontend login fails | Confirm frontend uses `/api/auth/login`, not `/auth/login`, and proxy target does not include `/api` |

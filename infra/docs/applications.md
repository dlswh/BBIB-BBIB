## Applications

이 문서는 EC2에서 실행되는 애플리케이션 배포 대상을 정리합니다.

대상은 아래 두 개입니다.

- Frontend Web: React/Vite build 결과물을 Nginx container로 서빙
- Backend API: Spring Boot jar를 Docker image로 빌드해 API 서버로 실행

## Frontend Web

프론트엔드는 Vite build 결과물인 `web/dist`를 Nginx container에 복사해서 서빙합니다. EC2의 native Nginx는 HTTPS 진입점만 담당하고, 프론트 컨테이너는 `127.0.0.1:3001`에만 바인딩합니다.

```text
Browser -> native Nginx HTTPS -> 127.0.0.1:3001 -> s207-frontend
```

환경 변수:

```dotenv
FRONTEND_PORT=3001
FRONTEND_API_BASE_URL=https://k14s207.p.ssafy.io
FRONTEND_USE_MOCK=true
FRONTEND_IMAGE_TAG=latest
```

수동 실행:

```bash
docker compose -p s14p31s207-frontend \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.frontend.yml \
  up -d --build
```

검증:

```bash
docker compose -p s14p31s207-frontend \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.frontend.yml \
  ps

curl -fsS http://127.0.0.1:3001/health
curl -fsSI http://127.0.0.1:3001/
docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' s207-frontend
```

native Nginx에는 아래 snippet을 include합니다.

```text
infra/nginx/frontend
```

적용:

```bash
sudo cp infra/nginx/frontend /etc/nginx/sites-available/frontend
sudo nginx -t
sudo systemctl reload nginx
```

## Backend API

백엔드는 Spring Boot jar를 Docker image로 빌드한 뒤 `127.0.0.1:8000`에서 container 내부 `8080`으로 바인딩합니다. EC2의 native Nginx는 HTTPS 진입점만 담당하고, `/api/` 요청을 백엔드 컨테이너로 넘깁니다.

```text
Browser -> native Nginx HTTPS -> /api/ -> 127.0.0.1:8000 -> s207-backend:8080
```

Spring Boot는 `server.servlet.context-path=/api`로 실행됩니다. 따라서 Nginx rewrite 없이도 내부/외부 health 경로가 모두 `/api/actuator/health`로 일치합니다.

백엔드와 주요 인프라 컨테이너는 `s207-app` 공용 Docker network에 연결합니다.

```text
s207-backend -> s207-postgres:5432
s207-backend -> s207-redis:6379
s207-backend -> kafka:19092
s207-backend -> s207-minio:9000
```

환경 변수:

```dotenv
BACKEND_PORT=8000
BACKEND_IMAGE_TAG=latest
BACKEND_SPRING_PROFILES_ACTIVE=dev
BACKEND_BASIC_AUTH_USER=s207-admin
BACKEND_BASIC_AUTH_PASSWORD=change-this-backend-password
BACKEND_JAVA_OPTS=
JWT_SECRET=change-this-jwt-secret
```

백엔드가 참조하는 인프라 값:

```dotenv
POSTGRES_DB=s207_app
POSTGRES_USER=s207_app
POSTGRES_PASSWORD=change-this-postgres-password
REDIS_PASSWORD=change-this-redis-password
KAFKA_PORT=9092
MINIO_ROOT_USER=s207-minio-admin
MINIO_ROOT_PASSWORD=change-this-minio-password
```

실행:

```bash
sh infra/scripts/up-backend.sh
```

로컬에서 Spring Boot를 컨테이너가 아니라 `bootRun`으로 직접 실행할 때도 `infra/compose/.env`를 기준으로 환경 변수를 주입합니다.

Windows:

```powershell
.\infra\scripts\run-backend-local.ps1
```

Linux/EC2 shell:

```bash
sh infra/scripts/run-backend-local.sh
```

이 스크립트는 `infra/compose/.env`의 DB, Redis, Kafka, JWT 값을 읽고 Spring 표준 환경 변수인 `SPRING_DATASOURCE_URL`, `SPRING_DATA_REDIS_HOST`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`로 변환합니다. 로컬 직접 실행 기준이므로 DB/Redis/Kafka host는 `localhost`를 사용합니다. Gradle daemon이 이전 환경 변수를 재사용하지 않도록 `--no-daemon`으로 실행합니다.

직접 실행:

```bash
docker compose -p s14p31s207-backend \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.backend.yml \
  up -d --build
```

검증:

```bash
docker compose -p s14p31s207-backend \
  --env-file infra/compose/.env \
  -f infra/compose/docker-compose.backend.yml \
  ps

curl -fsS http://127.0.0.1:8000/api/actuator/health
curl -fsSI http://127.0.0.1:8000/api/swagger-ui.html
curl -fsS http://127.0.0.1:8000/api/v3/api-docs
docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' s207-backend
```

native Nginx에는 아래 snippet을 include합니다.

```text
infra/nginx/backend
```

적용:

```bash
sudo cp infra/nginx/backend /etc/nginx/sites-available/backend
sudo nginx -t
sudo systemctl reload nginx
```

외부 HTTPS 검증:

```bash
curl -fsS https://k14s207.p.ssafy.io/api/actuator/health
curl -fsSI https://k14s207.p.ssafy.io/api/swagger-ui.html
curl -fsS https://k14s207.p.ssafy.io/api/v3/api-docs
```

## Jenkins Jobs

Frontend 배포 job:

```text
Item name: frontend-dev-deploy
Definition: Pipeline script from SCM
Repository URL: https://lab.ssafy.com/s14-final/S14P31S207.git
Credentials: gitlab-read-token
Branch Specifier: */dev
Script Path: infra/jenkins/pipelines/frontend-dev-deploy.Jenkinsfile
```

Backend 배포 job:

```text
Item name: backend-dev-deploy
Definition: Pipeline script from SCM
Repository URL: https://lab.ssafy.com/s14-final/S14P31S207.git
Credentials: gitlab-read-token
Branch Specifier: */dev
Script Path: infra/jenkins/pipelines/backend-dev-deploy.Jenkinsfile
```

Jenkins용 native Nginx snippet:

```text
infra/nginx/jenkins
```

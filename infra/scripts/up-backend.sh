#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INFRA_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_DIR="$INFRA_ROOT/compose"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"
BACKEND_COMPOSE="$COMPOSE_DIR/docker-compose.backend.yml"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
fi

for name in \
  BACKEND_BASIC_AUTH_USER \
  BACKEND_BASIC_AUTH_PASSWORD \
  JWT_SECRET \
  POSTGRES_DB \
  POSTGRES_USER \
  POSTGRES_PASSWORD \
  REDIS_PASSWORD \
  MINIO_ROOT_USER \
  MINIO_ROOT_PASSWORD; do
  if ! grep -q "^$name=" "$ENV_FILE"; then
    echo "Missing $name in infra/compose/.env. Add it from infra/compose/.env.example before starting Backend." >&2
    exit 1
  fi
  if grep -q "^$name=$" "$ENV_FILE"; then
    echo "$name is empty in infra/compose/.env. Set a non-empty value before starting Backend." >&2
    exit 1
  fi
done

docker network inspect s207-app >/dev/null 2>&1 || docker network create s207-app >/dev/null

docker compose -p s14p31s207-backend --env-file "$ENV_FILE" -f "$BACKEND_COMPOSE" up -d --build
docker compose -p s14p31s207-backend --env-file "$ENV_FILE" -f "$BACKEND_COMPOSE" ps

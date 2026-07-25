#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
COMPOSE_DIR="$REPO_ROOT/infra/compose"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"
PREFERRED_KAFKA_BOOTSTRAP_SERVERS="${BACKEND_KAFKA_BOOTSTRAP_SERVERS:-}"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
fi

set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a

if [ -n "$PREFERRED_KAFKA_BOOTSTRAP_SERVERS" ]; then
  BACKEND_KAFKA_BOOTSTRAP_SERVERS="$PREFERRED_KAFKA_BOOTSTRAP_SERVERS"
fi

for name in JWT_SECRET POSTGRES_DB POSTGRES_USER POSTGRES_PASSWORD REDIS_PASSWORD; do
  eval "value=\${$name:-}"
  if [ -z "$value" ]; then
    echo "Missing or empty $name in infra/compose/.env. Add it before starting Backend locally." >&2
    exit 1
  fi
done

export SPRING_PROFILES_ACTIVE="${BACKEND_SPRING_PROFILES_ACTIVE:-dev}"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/${POSTGRES_DB}"
export SPRING_DATASOURCE_USERNAME="$POSTGRES_USER"
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export SPRING_DATA_REDIS_HOST="localhost"
export SPRING_DATA_REDIS_PORT="${REDIS_PORT:-6379}"
export SPRING_DATA_REDIS_PASSWORD="$REDIS_PASSWORD"
export SPRING_KAFKA_BOOTSTRAP_SERVERS="${BACKEND_KAFKA_BOOTSTRAP_SERVERS:-localhost:${KAFKA_PORT:-9092}}"

cd "$REPO_ROOT/backend"
./gradlew bootRun --no-daemon

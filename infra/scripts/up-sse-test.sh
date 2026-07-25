#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INFRA_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_DIR="$INFRA_ROOT/compose"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"
SSE_TEST_COMPOSE="$COMPOSE_DIR/docker-compose.sse-test.yml"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
fi

export FAKE_IPC_KAFKA_BOOTSTRAP_SERVERS="${FAKE_IPC_KAFKA_BOOTSTRAP_SERVERS:-k14s207.p.ssafy.io:9092}"

docker compose -p s14p31s207-sse-test --env-file "$ENV_FILE" -f "$SSE_TEST_COMPOSE" up -d --build
docker compose -p s14p31s207-sse-test --env-file "$ENV_FILE" -f "$SSE_TEST_COMPOSE" ps

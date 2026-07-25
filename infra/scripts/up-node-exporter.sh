#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INFRA_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_DIR="$INFRA_ROOT/compose"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"
NODE_EXPORTER_COMPOSE="$COMPOSE_DIR/docker-compose.node-exporter.yml"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
fi

docker compose -p s14p31s207-node-exporter --env-file "$ENV_FILE" -f "$NODE_EXPORTER_COMPOSE" up -d
docker compose -p s14p31s207-node-exporter --env-file "$ENV_FILE" -f "$NODE_EXPORTER_COMPOSE" ps

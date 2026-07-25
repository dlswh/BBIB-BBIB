#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
INFRA_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_DIR="$INFRA_ROOT/compose"
ENV_FILE="$COMPOSE_DIR/.env"
ENV_EXAMPLE="$COMPOSE_DIR/.env.example"
GRAFANA_COMPOSE="$COMPOSE_DIR/docker-compose.grafana.yml"

if [ ! -f "$ENV_FILE" ]; then
  cp "$ENV_EXAMPLE" "$ENV_FILE"
  echo "Created infra/compose/.env from .env.example. Change credentials before using this beyond local testing."
fi

for name in GRAFANA_ADMIN_USER GRAFANA_ADMIN_PASSWORD; do
  if ! grep -q "^$name=" "$ENV_FILE"; then
    echo "Missing $name in infra/compose/.env. Add it from infra/compose/.env.example before starting Grafana." >&2
    exit 1
  fi
  if grep -q "^$name=$" "$ENV_FILE"; then
    echo "$name is empty in infra/compose/.env. Set a non-empty value before starting Grafana." >&2
    exit 1
  fi
done

docker compose -p s14p31s207-grafana --env-file "$ENV_FILE" -f "$GRAFANA_COMPOSE" up -d
docker compose -p s14p31s207-grafana --env-file "$ENV_FILE" -f "$GRAFANA_COMPOSE" ps

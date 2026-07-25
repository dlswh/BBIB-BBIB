#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)

cd "$REPO_ROOT"

if [ ! -f "infra/compose/.env" ]; then
  cp "infra/compose/.env.example" "infra/compose/.env"
  echo "Created infra/compose/.env from .env.example. Check credentials before running Spring."
fi

cat > "web/.env" <<'EOF'
VITE_USE_MOCK=false
VITE_API_BASE_URL=
API_PROXY_TARGET=http://localhost:8080
EOF

cleanup() {
  if [ -n "${BACKEND_PID:-}" ]; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi
  if [ -n "${FRONTEND_PID:-}" ]; then
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
}

trap cleanup INT TERM EXIT

echo "Starting local Spring Boot on http://localhost:8080 ..."
sh infra/scripts/run-backend-local.sh &
BACKEND_PID=$!

echo "Starting local Vite frontend on http://localhost:5173 ..."
(
  cd web
  npm install
  npm run dev -- --host 0.0.0.0
) &
FRONTEND_PID=$!

echo "Open http://localhost:5173 after both servers finish starting."
echo "Browser requests to /api/... will be proxied to http://localhost:8080/api/..."

wait "$BACKEND_PID" "$FRONTEND_PID"

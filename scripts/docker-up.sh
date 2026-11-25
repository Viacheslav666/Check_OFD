#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "Нужен Docker Compose: установите плагин (docker compose) или пакет docker-compose." >&2
  exit 1
fi

"${COMPOSE[@]}" up -d --build "$@"
echo
echo "API:  http://127.0.0.1:${OFD_HOST_PORT:-18443}/api/health"
echo "Стоп: ${COMPOSE[*]} down"

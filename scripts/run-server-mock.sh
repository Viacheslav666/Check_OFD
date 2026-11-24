#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export OFD_MODE=mock
export OFD_INN=7707083893
export SERVER_PORT="${SERVER_PORT:-18443}"
export DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/ofd_marking}"
export DB_USER="${DB_USER:-ofd}"
export DB_PASSWORD="${DB_PASSWORD:-ofd}"
mvn -pl server -am -DskipTests package
exec java -jar server/target/ofd-marking-server-1.0.0-SNAPSHOT.jar

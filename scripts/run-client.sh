#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
export OFD_CLIENT_SERVER_URL="${OFD_CLIENT_SERVER_URL:-http://127.0.0.1:18443}"
mvn -pl client -am -DskipTests package
mvn -pl client org.openjfx:javafx-maven-plugin:0.0.8:run

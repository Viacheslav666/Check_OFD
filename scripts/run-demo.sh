#!/usr/bin/env bash
# Демо без Docker: центр (H2) + 2 mock-агента на этой машине.
# Центр: http://127.0.0.1:18443  агенты: 127.0.0.1:18501, 127.0.0.1:18502
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
mvn -B -q -pl agent,server -am -DskipTests package
AGENT_JAR=$(ls agent/target/ofd-agent-*-exec.jar | head -1)
SERVER_JAR=$(ls server/target/ofd-marking-server-*.jar | grep -v original | head -1)
DEMO=${DEMO_DIR:-/tmp/ofd-demo}
mkdir -p "$DEMO"
start_agent() { # name port inn token
  AGENT_NAME=$1 AGENT_PORT=$2 OFD_INN=$3 AGENT_TOKEN=$4 OFD_MODE=mock AGENT_ALLOWED_IPS=127.0.0.1,::1 \
  AGENT_DATA_DIR="$DEMO/$1" nohup java -Xmx256m -jar "$AGENT_JAR" > "$DEMO/$1.out" 2>&1 &
  echo $! > "$DEMO/$1.pid"
}
start_agent demo-samara 18501 635001808204 demo-token-agent1-0123456789
start_agent demo-tolyatti 18502 632100000017 demo-token-agent2-0123456789
OFD_DATA_DIR="$DEMO/central" SERVER_PORT=18443 \
CENTRAL_BOOTSTRAP_AGENTS="demo-samara|127.0.0.1|18501|demo-token-agent1-0123456789;demo-tolyatti|127.0.0.1|18502|demo-token-agent2-0123456789" \
nohup java -Xmx512m -jar "$SERVER_JAR" --spring.profiles.active=desktop > "$DEMO/central.out" 2>&1 &
echo $! > "$DEMO/central.pid"
echo "Запущено. Логи: $DEMO. Остановка: kill \$(cat $DEMO/*.pid)"

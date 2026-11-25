#!/usr/bin/env bash
# Сборка ofd-agent-windows.zip на Linux/macOS (для Windows-RDP, без Docker):
# ofd-agent.jar + Eclipse Temurin JRE 21 для Windows x64 (проверка SHA256) + WinSW + скрипты.
# Использование: scripts/package-agent-windows.sh [выходной_каталог]   (по умолчанию dist/)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT_DIR="${1:-$ROOT/dist}"
WINSW_URL="https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe"
WINSW_SHA256="05b82d46ad331cc16bdc00de5c6332c1ef818df8ceefcd49c726553209b3a0da"
API="https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse"

cd "$ROOT"
[ "${SKIP_BUILD:-0}" = "1" ] || mvn -B -q -pl agent -am -DskipTests package
JAR="$(ls agent/target/ofd-agent-*-exec.jar | head -1)"
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
PKG="$WORK/ofd-agent"; mkdir -p "$PKG"

cp "$JAR" "$PKG/ofd-agent.jar"
cp agent/src/main/winsw/ofd-agent.xml deploy/agent/agent.yml.example deploy/agent/README.txt "$PKG/"
for s in Install-Agent.ps1 Update-Agent.ps1 Set-AgentAllowedIps.ps1 Uninstall-Agent.ps1 AgentCommon.ps1; do
  cp "scripts/windows/$s" "$PKG/"
done

read -r JRE_NAME JRE_LINK JRE_SHA < <(curl -fsSL "$API" | python3 -c '
import json,sys; p=json.load(sys.stdin)[0]["binary"]["package"]; print(p["name"], p["link"], p["checksum"])')
echo "Temurin JRE: $JRE_NAME"
curl -fsSL -o "$WORK/$JRE_NAME" "$JRE_LINK"
echo "$JRE_SHA  $WORK/$JRE_NAME" | sha256sum -c -
unzip -q "$WORK/$JRE_NAME" -d "$WORK/jre-unpack"
mv "$WORK/jre-unpack"/*/ "$PKG/jre"

curl -fsSL -o "$PKG/WinSW-x64.exe" "$WINSW_URL"
echo "$WINSW_SHA256  $PKG/WinSW-x64.exe" | sha256sum -c -

mkdir -p "$OUT_DIR"
ZIP="$OUT_DIR/ofd-agent-windows.zip"
rm -f "$ZIP"
python3 - "$PKG" "$ZIP" <<'PY'
import os, sys, zipfile
src, dst = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(dst, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for base, _, files in os.walk(src):
        for f in files:
            full = os.path.join(base, f)
            z.write(full, os.path.relpath(full, src))
PY
echo "Built $ZIP ($(du -h "$ZIP" | cut -f1))"

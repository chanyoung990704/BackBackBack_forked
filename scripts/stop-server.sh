#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"

if command -v systemctl >/dev/null 2>&1; then
  if systemctl list-unit-files --type=service | awk '{print $1}' | grep -qx "${APP_NAME}.service"; then
    systemctl stop "$APP_NAME"
    echo "[INFO] 서버 종료 완료 (systemd: $APP_NAME)"
    exit 0
  fi
fi

APP_DIR="${APP_DIR:-/opt/project}"
PID_FILE="${PID_FILE:-$APP_DIR/app.pid}"
JAR_PATH="${JAR_PATH:-$APP_DIR/app.jar}"

PID=""
if [ -f "$PID_FILE" ]; then
  PID="$(cat "$PID_FILE")"
fi

if [ -z "$PID" ]; then
  PID="$(pgrep -f "java.*${JAR_PATH}" || true)"
fi

if [ -z "$PID" ]; then
  PID="$(pgrep -f "project-0.0.1-SNAPSHOT.jar" || true)"
fi

if [ -z "$PID" ]; then
  echo "[INFO] 실행 중인 프로세스를 찾지 못했습니다."
  exit 0
fi

if kill -0 "$PID" 2>/dev/null; then
  kill "$PID"
  for _ in {1..20}; do
    if kill -0 "$PID" 2>/dev/null; then
      sleep 1
    else
      break
    fi
  done
  if kill -0 "$PID" 2>/dev/null; then
    kill -9 "$PID"
  fi
fi

rm -f "$PID_FILE"
echo "[INFO] 서버 종료 완료 (legacy)"

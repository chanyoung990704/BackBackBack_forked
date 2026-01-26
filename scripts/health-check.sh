#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ec2-user/app/BackBackBack}"
PID_FILE="${PID_FILE:-$APP_DIR/app.pid}"

if [ -f "$APP_DIR/.env" ]; then
  set -a
  # shellcheck disable=SC1091
  . "$APP_DIR/.env"
  set +a
fi

HEALTHCHECK_URL="${HEALTHCHECK_URL:-}"

if [ -n "$HEALTHCHECK_URL" ]; then
  if ! curl -fsS "$HEALTHCHECK_URL" >/dev/null; then
    echo "[ERROR] 헬스체크 실패: $HEALTHCHECK_URL" >&2
    exit 1
  fi
  echo "[INFO] 헬스체크 성공: $HEALTHCHECK_URL"
  exit 0
fi

if [ ! -f "$PID_FILE" ]; then
  echo "[ERROR] PID 파일이 없습니다." >&2
  exit 1
fi

PID="$(cat "$PID_FILE")"
if ! kill -0 "$PID" 2>/dev/null; then
  echo "[ERROR] 프로세스가 실행 중이 아닙니다. (pid: $PID)" >&2
  exit 1
fi

echo "[INFO] 서버 상태 정상 (pid: $PID)"

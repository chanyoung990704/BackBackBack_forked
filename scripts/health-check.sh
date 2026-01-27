#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"
ENV_FILE="${ENV_FILE:-/etc/${APP_NAME}/${APP_NAME}.env}"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
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

if command -v systemctl >/dev/null 2>&1; then
  if systemctl is-active --quiet "$APP_NAME"; then
    echo "[INFO] 서버 상태 정상 (systemd: $APP_NAME)"
    exit 0
  fi
  echo "[ERROR] systemd 서비스가 비활성 상태입니다: $APP_NAME" >&2
  exit 1
fi

echo "[ERROR] systemctl을 찾지 못했습니다." >&2
exit 1

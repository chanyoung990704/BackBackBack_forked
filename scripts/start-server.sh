#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"
START_WAIT_SECONDS="${START_WAIT_SECONDS:-30}"

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload
  systemctl restart "$APP_NAME"

  for ((i=1; i<=START_WAIT_SECONDS; i++)); do
    state="$(systemctl is-active "$APP_NAME" || true)"
    if [ "$state" = "active" ]; then
      echo "[INFO] 서버 시작 완료 (systemd: $APP_NAME)"
      exit 0
    fi
    if [ "$state" = "failed" ] || [ "$state" = "inactive" ]; then
      echo "[ERROR] 서비스 시작 실패 (systemd: $APP_NAME, state=$state)" >&2
      systemctl --no-pager --full status "$APP_NAME" || true
      journalctl -u "$APP_NAME" -n 120 --no-pager || true
      exit 1
    fi
    sleep 1
  done

  echo "[WARN] 서비스가 아직 완전히 활성화되지 않았습니다. ValidateService 단계에서 추가 확인합니다. (systemd: $APP_NAME)"
  exit 0
fi

echo "[ERROR] systemctl을 찾지 못했습니다." >&2
exit 1

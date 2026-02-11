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

HEALTHCHECK_PATH="${HEALTHCHECK_PATH:-/actuator/health}"
HEALTHCHECK_MAX_RETRIES="${HEALTHCHECK_MAX_RETRIES:-30}"
HEALTHCHECK_INTERVAL_SECONDS="${HEALTHCHECK_INTERVAL_SECONDS:-2}"
HEALTHCHECK_INITIAL_DELAY_SECONDS="${HEALTHCHECK_INITIAL_DELAY_SECONDS:-5}"

HEALTHCHECK_URL="${HEALTHCHECK_URL:-}"
if [ -z "$HEALTHCHECK_URL" ]; then
  HEALTH_PORT="${MANAGEMENT_SERVER_PORT:-${SERVER_PORT:-8080}}"
  HEALTHCHECK_URL="http://localhost:${HEALTH_PORT}${HEALTHCHECK_PATH}"
fi

print_service_diagnostics() {
  if ! command -v systemctl >/dev/null 2>&1; then
    return
  fi
  if ! systemctl list-unit-files --type=service | awk '{print $1}' | grep -qx "${APP_NAME}.service"; then
    return
  fi

  echo "[INFO] systemd 상태 출력: ${APP_NAME}" >&2
  systemctl --no-pager --full status "$APP_NAME" || true
  echo "[INFO] systemd 최근 로그 출력: ${APP_NAME}" >&2
  journalctl -u "$APP_NAME" -n 120 --no-pager || true
}

if [ "$HEALTHCHECK_INITIAL_DELAY_SECONDS" -gt 0 ]; then
  echo "[INFO] 헬스체크 전 초기 대기: ${HEALTHCHECK_INITIAL_DELAY_SECONDS}초"
  sleep "$HEALTHCHECK_INITIAL_DELAY_SECONDS"
fi

for ((attempt=1; attempt<=HEALTHCHECK_MAX_RETRIES; attempt++)); do
  if curl --connect-timeout 2 --max-time 5 -fsS "$HEALTHCHECK_URL" >/dev/null; then
    echo "[INFO] 헬스체크 성공: $HEALTHCHECK_URL (시도 ${attempt}/${HEALTHCHECK_MAX_RETRIES})"
    exit 0
  fi

  echo "[WARN] 헬스체크 실패: $HEALTHCHECK_URL (시도 ${attempt}/${HEALTHCHECK_MAX_RETRIES})" >&2

  if command -v systemctl >/dev/null 2>&1; then
    if systemctl list-unit-files --type=service | awk '{print $1}' | grep -qx "${APP_NAME}.service"; then
      service_state="$(systemctl is-active "$APP_NAME" || true)"
      if [ "$service_state" = "failed" ] || [ "$service_state" = "inactive" ]; then
        echo "[ERROR] 서비스 상태 비정상: ${APP_NAME} (${service_state})" >&2
        print_service_diagnostics
        exit 1
      fi
    fi
  fi

  if [ "$attempt" -lt "$HEALTHCHECK_MAX_RETRIES" ]; then
    sleep "$HEALTHCHECK_INTERVAL_SECONDS"
  fi
done

echo "[ERROR] 헬스체크 실패: $HEALTHCHECK_URL (최대 재시도 초과)" >&2
print_service_diagnostics
exit 1

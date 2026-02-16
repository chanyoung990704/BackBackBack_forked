#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"
ENV_FILE="${ENV_FILE:-/etc/${APP_NAME}/${APP_NAME}.env}"
APP_DIR="${APP_DIR:-/opt/project}"
LIB_FILE="${APP_DIR}/scripts/lib/deploy-runtime.sh"

if [ -f "$ENV_FILE" ]; then
  set -a
  # shellcheck disable=SC1090
  . "$ENV_FILE"
  set +a
fi

if [ -f "$LIB_FILE" ]; then
  # shellcheck disable=SC1090
  . "$LIB_FILE"
  DEPLOY_RUNTIME_RESOLVED="$(resolve_deploy_runtime)"
else
  DEPLOY_RUNTIME_RESOLVED="${DEPLOY_RUNTIME:-systemd}"
fi

HEALTHCHECK_PATH="${HEALTHCHECK_PATH:-/actuator/health}"
HEALTHCHECK_MAX_RETRIES="${HEALTHCHECK_MAX_RETRIES:-90}" # 30 -> 90회 (약 3분으로 연장)
HEALTHCHECK_INTERVAL_SECONDS="${HEALTHCHECK_INTERVAL_SECONDS:-2}"
HEALTHCHECK_INITIAL_DELAY_SECONDS="${HEALTHCHECK_INITIAL_DELAY_SECONDS:-10}"

HEALTHCHECK_URL="${HEALTHCHECK_URL:-}"
if [ -z "$HEALTHCHECK_URL" ]; then
  HEALTH_PORT="${MANAGEMENT_SERVER_PORT:-${SERVER_PORT:-8080}}"
  HEALTHCHECK_URL="http://localhost:${HEALTH_PORT}${HEALTHCHECK_PATH}"
fi

print_service_diagnostics() {
  if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ]; then
    if [ -f "$LIB_FILE" ]; then
      echo "[INFO] docker compose 상태 출력" >&2
      run_compose_command ps || true
      echo "[INFO] docker compose 최근 로그 출력 (app)" >&2
      run_compose_command logs --tail=120 app || true
    fi
    return
  fi

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

check_docker_dependency_state() {
  if [ ! -f "$LIB_FILE" ]; then
    return 0
  fi

  local kafka_enabled_raw="${APP_AI_JOB_KAFKA_ENABLED:-true}"
  local kafka_enabled
  kafka_enabled="$(printf '%s' "$kafka_enabled_raw" | tr '[:upper:]' '[:lower:]')"

  local required_services=("redis")
  if [ "$kafka_enabled" = "true" ]; then
    required_services+=("kafka")
  fi

  local defined_services service_status
  defined_services="$(run_compose_command config --services 2>/dev/null || true)"

  for service in "${required_services[@]}"; do
    if ! grep -qx "$service" <<< "$defined_services"; then
      echo "[ERROR] docker compose에 필수 서비스(${service})가 정의되어 있지 않습니다." >&2
      echo "[ERROR] 배포 산출물의 docker-compose.app.yml 버전을 확인하세요." >&2
      print_service_diagnostics
      exit 1
    fi

    service_status="$(run_compose_command ps "$service" 2>/dev/null | tr -d '\n' || true)"
    if [ -z "$service_status" ]; then
      echo "[ERROR] docker ${service} 컨테이너 상태를 조회하지 못했습니다." >&2
      print_service_diagnostics
      exit 1
    fi
    if [[ "$service_status" == *"Exit"* ]] || [[ "$service_status" == *"exited"* ]] || [[ "$service_status" == *"dead"* ]]; then
      echo "[ERROR] docker ${service} 컨테이너 상태 비정상" >&2
      print_service_diagnostics
      exit 1
    fi
  done
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

  if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ]; then
    check_docker_dependency_state
    if [ -f "$LIB_FILE" ]; then
      app_container_state="$(run_compose_command ps app 2>/dev/null | tr -d '\n' || true)"
      if [[ "$app_container_state" == *"Exit"* ]] || [[ "$app_container_state" == *"exited"* ]] || [[ "$app_container_state" == *"dead"* ]]; then
        echo "[ERROR] docker app 컨테이너 상태 비정상" >&2
        print_service_diagnostics
        exit 1
      fi
    fi
  else
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
  fi

  if [ "$attempt" -lt "$HEALTHCHECK_MAX_RETRIES" ]; then
    sleep "$HEALTHCHECK_INTERVAL_SECONDS"
  fi
done

echo "[ERROR] 헬스체크 실패: $HEALTHCHECK_URL (최대 재시도 초과)" >&2
print_service_diagnostics
exit 1

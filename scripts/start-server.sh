#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"
START_WAIT_SECONDS="${START_WAIT_SECONDS:-30}"
APP_DIR="${APP_DIR:-/opt/project}"
LIB_FILE="${APP_DIR}/scripts/lib/deploy-runtime.sh"
ENV_FILE="${ENV_FILE:-/etc/${APP_NAME}/${APP_NAME}.env}"
IMAGE_ENV_FILE="${IMAGE_ENV_FILE:-${APP_DIR}/image-uri.env}"

if [ -f "$LIB_FILE" ]; then
  # shellcheck disable=SC1090
  . "$LIB_FILE"
  # 런타임 결정 전에 환경 변수 파일을 먼저 로드하여 DEPLOY_RUNTIME=docker 설정을 읽어옵니다.
  load_env_file_if_exists "$ENV_FILE"
  DEPLOY_RUNTIME_RESOLVED="$(resolve_deploy_runtime)"
else
  DEPLOY_RUNTIME_RESOLVED="${DEPLOY_RUNTIME:-systemd}"
fi

if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ] && [ ! -f "$LIB_FILE" ]; then
  echo "[ERROR] docker 런타임 공통 스크립트를 찾지 못했습니다: $LIB_FILE" >&2
  exit 1
fi

if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ]; then
  load_env_file_if_exists "$IMAGE_ENV_FILE"

  if [ -z "${APP_IMAGE:-}" ]; then
    echo "[ERROR] APP_IMAGE 값이 비어 있어 docker 배포를 진행할 수 없습니다." >&2
    exit 1
  fi

  docker_login_to_ecr_if_needed
  run_compose_command pull app

  # 전체 down/up을 피하고 변경된 서비스만 갱신해 다운타임과 부수 영향을 줄입니다.
  run_compose_command up -d --remove-orphans
  echo "[INFO] 서버 시작 완료 (docker compose)"
  exit 0
fi

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

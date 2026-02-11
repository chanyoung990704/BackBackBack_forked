#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "[ERROR] root 권한으로 실행해야 합니다." >&2
  exit 1
fi

APP_NAME="${APP_NAME:-backbackback}"
APP_DIR="${APP_DIR:-/opt/project}"
LOG_DIR="${LOG_DIR:-$APP_DIR/logs}"

ENV_DIR="/etc/${APP_NAME}"
ENV_FILE="${ENV_DIR}/${APP_NAME}.env"
ENV_SOURCE="${APP_DIR}/scripts/systemd/${APP_NAME}.env.example"

mkdir -p "$LOG_DIR"
mkdir -p "$ENV_DIR"

if [ ! -f "$ENV_FILE" ]; then
  if [ -f "$ENV_SOURCE" ]; then
    cp "$ENV_SOURCE" "$ENV_FILE"
    echo "[WARN] 환경 변수 파일을 생성했습니다. 값 확인 후 수정하세요: $ENV_FILE" >&2
  else
    echo "[WARN] 환경 변수 파일을 찾지 못했습니다: $ENV_FILE" >&2
  fi
fi

if [ -f "$ENV_FILE" ]; then
  chown root:root "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi

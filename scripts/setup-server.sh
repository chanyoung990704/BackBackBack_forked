#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/project}"
SCRIPTS_DIR="${SCRIPTS_DIR:-$APP_DIR/scripts}"
APP_NAME="${APP_NAME:-backbackback}"
LIB_FILE="${SCRIPTS_DIR}/lib/deploy-runtime.sh"

if [ -d "$SCRIPTS_DIR" ]; then
  find "$SCRIPTS_DIR" -type f -name '*.sh' -exec chmod +x {} + || true
fi

if [ -f "$LIB_FILE" ]; then
  # 배포 런타임 공통 로직을 로드합니다.
  # shellcheck disable=SC1090
  . "$LIB_FILE"
  DEPLOY_RUNTIME_RESOLVED="$(resolve_deploy_runtime)"
  PM="$(resolve_package_manager)"
else
  DEPLOY_RUNTIME_RESOLVED="${DEPLOY_RUNTIME:-systemd}"
  if command -v dnf >/dev/null 2>&1; then
    PM="dnf"
  else
    PM="yum"
  fi
fi

if ! command -v curl >/dev/null 2>&1; then
  "$PM" install -y curl
fi

if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ]; then
  if ! command -v docker >/dev/null 2>&1; then
    if [ "$PM" = "dnf" ]; then
      "$PM" install -y docker
    else
      amazon-linux-extras install docker -y || "$PM" install -y docker
    fi
  fi

  if ! docker compose version >/dev/null 2>&1; then
    if [ "$PM" = "dnf" ]; then
      "$PM" install -y docker-compose-plugin || true
    else
      "$PM" install -y docker-compose-plugin || true
    fi
  fi

  if ! command -v aws >/dev/null 2>&1; then
    "$PM" install -y awscli || true
  fi

  systemctl enable --now docker
  usermod -aG docker ec2-user || true
  echo "[INFO] Docker 런타임 준비 완료"
else
  if ! command -v java >/dev/null 2>&1; then
    "$PM" install -y java-21-amazon-corretto-headless || "$PM" install -y java-21-amazon-corretto
  fi
  echo "[INFO] systemd(JAR) 런타임 준비 완료"
fi

mkdir -p "$APP_DIR"
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/backup"
mkdir -p "/etc/${APP_NAME}"
chown -R ec2-user:ec2-user "$APP_DIR"

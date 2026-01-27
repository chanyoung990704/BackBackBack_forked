#!/usr/bin/env bash
set -euo pipefail

APP_NAME="${APP_NAME:-backbackback}"
APP_DIR="${APP_DIR:-/home/ec2-user/app/BackBackBack}"

if command -v docker >/dev/null 2>&1; then
  if [ -f "$APP_DIR/docker-compose.yml" ] || compgen -G "$APP_DIR/docker-compose*.yml" >/dev/null; then
    if command -v systemctl >/dev/null 2>&1 && ! systemctl is-active --quiet docker; then
      systemctl start docker
    fi

    if docker compose version >/dev/null 2>&1; then
      DOCKER_COMPOSE="docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
      DOCKER_COMPOSE="docker-compose"
    else
      DOCKER_COMPOSE=""
    fi

    if [ -n "$DOCKER_COMPOSE" ]; then
      (cd "$APP_DIR" && $DOCKER_COMPOSE up -d)
    else
      echo "[WARN] docker compose 명령을 찾지 못했습니다. 의존 컨테이너 실행을 건너뜁니다." >&2
    fi
  fi
else
  echo "[WARN] docker 명령을 찾지 못했습니다. 의존 컨테이너 실행을 건너뜁니다." >&2
fi

if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload
  systemctl restart "$APP_NAME"
  echo "[INFO] 서버 시작 완료 (systemd: $APP_NAME)"
  exit 0
fi

echo "[ERROR] systemctl을 찾지 못했습니다." >&2
exit 1

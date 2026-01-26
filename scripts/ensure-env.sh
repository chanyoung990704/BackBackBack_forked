#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ec2-user/app/BackBackBack}"
LOG_DIR="${LOG_DIR:-$APP_DIR/logs}"

mkdir -p "$LOG_DIR"

if [ ! -f "$APP_DIR/.env" ]; then
  if [ -f "$APP_DIR/.env.example" ]; then
    cp "$APP_DIR/.env.example" "$APP_DIR/.env"
    echo "[WARN] .env.example을 .env로 복사했습니다. 필요한 값으로 수정하세요." >&2
  else
    echo "[WARN] .env 파일이 없습니다. dev 프로파일에 필요한 환경 변수를 확인하세요." >&2
  fi
fi

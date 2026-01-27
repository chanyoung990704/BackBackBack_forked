#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/home/ec2-user/app/BackBackBack}"
SCRIPTS_DIR="${SCRIPTS_DIR:-$APP_DIR/scripts}"

if [ -d "$SCRIPTS_DIR" ]; then
  chmod +x "$SCRIPTS_DIR"/*.sh || true
fi

if command -v dnf >/dev/null 2>&1; then
  PM="dnf"
else
  PM="yum"
fi

if ! command -v docker >/dev/null 2>&1; then
  "$PM" install -y docker
  systemctl enable --now docker
fi

if ! docker compose version >/dev/null 2>&1 && ! command -v docker-compose >/dev/null 2>&1; then
  if "$PM" install -y docker-compose-plugin >/dev/null 2>&1; then
    :
  else
    ARCH="$(uname -m)"
    case "$ARCH" in
      x86_64) BIN_ARCH="x86_64" ;;
      aarch64) BIN_ARCH="aarch64" ;;
      *) echo "[ERROR] 지원하지 않는 아키텍처입니다: $ARCH" >&2; exit 1 ;;
    esac
    PLUGIN_DIR="/usr/local/lib/docker/cli-plugins"
    mkdir -p "$PLUGIN_DIR"
    curl -fsSL "https://github.com/docker/compose/releases/download/v2.29.2/docker-compose-linux-$BIN_ARCH" \
      -o "$PLUGIN_DIR/docker-compose"
    chmod +x "$PLUGIN_DIR/docker-compose"
  fi
fi

if ! command -v java >/dev/null 2>&1; then
  "$PM" install -y java-21-amazon-corretto-headless || "$PM" install -y java-21-amazon-corretto
fi

usermod -aG docker ec2-user || true
mkdir -p "$APP_DIR"
chown -R ec2-user:ec2-user "$APP_DIR"
mkdir -p /opt/app
chown ec2-user:ec2-user /opt/app

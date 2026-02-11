#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/project}"
SCRIPTS_DIR="${SCRIPTS_DIR:-$APP_DIR/scripts}"

if [ -d "$SCRIPTS_DIR" ]; then
  chmod +x "$SCRIPTS_DIR"/*.sh || true
fi

if command -v dnf >/dev/null 2>&1; then
  PM="dnf"
else
  PM="yum"
fi

if ! command -v java >/dev/null 2>&1; then
  "$PM" install -y java-21-amazon-corretto-headless || "$PM" install -y java-21-amazon-corretto
fi

if ! command -v curl >/dev/null 2>&1; then
  "$PM" install -y curl
fi

mkdir -p "$APP_DIR"
mkdir -p "$APP_DIR/logs"
mkdir -p "$APP_DIR/backup"
chown -R ec2-user:ec2-user "$APP_DIR"

#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "[ERROR] root 권한으로 실행해야 합니다." >&2
  exit 1
fi

APP_DIR="${APP_DIR:-/opt/project}"
TARGET_DIR="${TARGET_DIR:-/opt/project}"
TARGET_JAR="${TARGET_JAR:-$TARGET_DIR/app.jar}"
BACKUP_DIR="${BACKUP_DIR:-$TARGET_DIR/backup}"

JAR_DIR="${APP_DIR}/build/libs"
if [ ! -d "$JAR_DIR" ]; then
  echo "[ERROR] JAR 디렉터리를 찾지 못했습니다: $JAR_DIR" >&2
  exit 1
fi

shopt -s nullglob
JARS=("$JAR_DIR"/*.jar)
shopt -u nullglob

if [ ${#JARS[@]} -eq 0 ]; then
  echo "[ERROR] JAR 파일을 찾지 못했습니다: $JAR_DIR" >&2
  exit 1
fi

JAR_PATH=""
for CANDIDATE in "${JARS[@]}"; do
  if [[ "$CANDIDATE" != *-plain.jar ]]; then
    JAR_PATH="$CANDIDATE"
    break
  fi
done

if [ -z "$JAR_PATH" ]; then
  JAR_PATH="${JARS[0]}"
fi

mkdir -p "$TARGET_DIR"
mkdir -p "$BACKUP_DIR"

if [ -f "$TARGET_JAR" ]; then
  cp "$TARGET_JAR" "$BACKUP_DIR/app-$(date +%Y%m%d%H%M%S).jar"
fi

cp "$JAR_PATH" "$TARGET_JAR"
chown ec2-user:ec2-user "$TARGET_JAR"
chmod 644 "$TARGET_JAR"

echo "[INFO] JAR 설치 완료: $TARGET_JAR"

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
APP_NAME="${APP_NAME:-backbackback}"
ENV_FILE="${ENV_FILE:-/etc/${APP_NAME}/${APP_NAME}.env}"
LIB_FILE="${APP_DIR}/scripts/lib/deploy-runtime.sh"

if [ -f "$LIB_FILE" ]; then
  # shellcheck disable=SC1090
  . "$LIB_FILE"
  DEPLOY_RUNTIME_RESOLVED="$(resolve_deploy_runtime)"
else
  DEPLOY_RUNTIME_RESOLVED="${DEPLOY_RUNTIME:-systemd}"
fi

if [ "$DEPLOY_RUNTIME_RESOLVED" = "docker" ]; then
  IMAGE_ENV_SOURCE="${APP_DIR}/image-uri.env"
  IMAGE_ENV_TARGET="${TARGET_DIR}/image-uri.env"
  mkdir -p "$TARGET_DIR"
  mkdir -p "$BACKUP_DIR"

  if [ ! -f "$IMAGE_ENV_SOURCE" ]; then
    echo "[ERROR] 이미지 정보 파일을 찾지 못했습니다: $IMAGE_ENV_SOURCE" >&2
    exit 1
  fi

  if [ -f "$IMAGE_ENV_TARGET" ]; then
    cp "$IMAGE_ENV_TARGET" "$BACKUP_DIR/image-uri-$(date +%Y%m%d%H%M%S).env"
  fi

  cp "$IMAGE_ENV_SOURCE" "$IMAGE_ENV_TARGET"
  chown root:root "$IMAGE_ENV_TARGET"
  chmod 600 "$IMAGE_ENV_TARGET"

  load_env_file_if_exists "$ENV_FILE"
  load_env_file_if_exists "$IMAGE_ENV_TARGET"
  if [ -z "${APP_IMAGE:-}" ]; then
    echo "[ERROR] APP_IMAGE 값이 비어 있습니다: $IMAGE_ENV_TARGET" >&2
    exit 1
  fi

  echo "[INFO] Docker 이미지 정보 설치 완료: $APP_IMAGE"
  exit 0
fi

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

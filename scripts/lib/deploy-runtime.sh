#!/usr/bin/env bash

resolve_deploy_runtime() {
  local runtime="${DEPLOY_RUNTIME:-systemd}"
  runtime="$(printf '%s' "$runtime" | tr '[:upper:]' '[:lower:]')"

  case "$runtime" in
    docker|systemd)
      printf '%s\n' "$runtime"
      ;;
    *)
      echo "[WARN] 지원하지 않는 DEPLOY_RUNTIME(${runtime})입니다. systemd로 기본 처리합니다." >&2
      printf 'systemd\n'
      ;;
  esac
}

resolve_package_manager() {
  if command -v dnf >/dev/null 2>&1; then
    printf 'dnf\n'
    return 0
  fi
  printf 'yum\n'
}

extract_ecr_region() {
  local registry="${1:-}"
  if [[ "$registry" =~ ^[0-9]+\.dkr\.ecr\.([a-z0-9-]+)\.amazonaws\.com$ ]]; then
    printf '%s\n' "${BASH_REMATCH[1]}"
    return 0
  fi
  return 1
}

load_env_file_if_exists() {
  local env_file="$1"
  if [ -f "$env_file" ]; then
    set -a
    # shellcheck disable=SC1090
    . "$env_file"
    set +a
  fi
}

set_compose_command() {
  COMPOSE_CMD=()

  if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
    COMPOSE_CMD=("docker" "compose")
    return 0
  fi

  if command -v docker-compose >/dev/null 2>&1; then
    COMPOSE_CMD=("docker-compose")
    return 0
  fi

  return 1
}

run_compose_command() {
  local app_name="${APP_NAME:-backbackback}"
  local app_dir="${APP_DIR:-/opt/project}"
  local compose_file="${COMPOSE_FILE_PATH:-${app_dir}/docker-compose.app.yml}"
  local env_file="${ENV_FILE:-/etc/${app_name}/${app_name}.env}"
  local image_env_file="${IMAGE_ENV_FILE:-${app_dir}/image-uri.env}"
  local cmd=()

  # 도커 배포 시에는 쉘 환경 변수로 로드하지 않고 (오염 방지)
  # docker compose의 --env-file 옵션만 사용하여 보안을 유지합니다.

  if [ ! -f "$compose_file" ]; then
    echo "[ERROR] compose 파일을 찾지 못했습니다: $compose_file" >&2
    return 1
  fi

  if ! set_compose_command; then
    echo "[ERROR] docker compose 또는 docker-compose 명령을 찾지 못했습니다." >&2
    return 1
  fi

  cmd=("${COMPOSE_CMD[@]}")
  if [ -f "$env_file" ]; then
    cmd+=("--env-file" "$env_file")
  fi
  # APP_IMAGE 같은 이미지 치환 변수는 배포 산출물(image-uri.env)에서 보완합니다.
  if [ -f "$image_env_file" ]; then
    cmd+=("--env-file" "$image_env_file")
  fi
  cmd+=("-f" "$compose_file")
  cmd+=("$@")

  "${cmd[@]}"
}

docker_login_to_ecr_if_needed() {
  local app_image="${APP_IMAGE:-}"
  local registry=""
  local region="${AWS_REGION:-}"
  local inferred_region=""

  if [ -z "$app_image" ]; then
    return 0
  fi

  registry="${app_image%%/*}"
  if ! inferred_region="$(extract_ecr_region "$registry")"; then
    return 0
  fi

  if [ -z "$region" ]; then
    region="$inferred_region"
  fi

  if ! command -v aws >/dev/null 2>&1; then
    echo "[ERROR] aws cli를 찾지 못했습니다. ECR 로그인에 aws cli가 필요합니다." >&2
    return 1
  fi

  if ! command -v docker >/dev/null 2>&1; then
    echo "[ERROR] docker를 찾지 못했습니다." >&2
    return 1
  fi

  aws ecr get-login-password --region "$region" \
    | docker login --username AWS --password-stdin "$registry" >/dev/null
  echo "[INFO] ECR 로그인 완료: $registry"
}

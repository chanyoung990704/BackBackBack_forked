#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
. "${SCRIPT_DIR}/../lib/deploy-runtime.sh"

assert_eq() {
  local expected="$1"
  local actual="$2"
  local message="$3"

  if [ "$expected" != "$actual" ]; then
    echo "[FAIL] ${message} | expected=${expected}, actual=${actual}" >&2
    exit 1
  fi
}

test_resolve_deploy_runtime_default() {
  unset DEPLOY_RUNTIME
  assert_eq "systemd" "$(resolve_deploy_runtime)" "DEPLOY_RUNTIME 기본값"
}

test_resolve_deploy_runtime_docker() {
  DEPLOY_RUNTIME="DoCkEr"
  assert_eq "docker" "$(resolve_deploy_runtime)" "DEPLOY_RUNTIME docker 변환"
}

test_resolve_deploy_runtime_invalid() {
  DEPLOY_RUNTIME="legacy"
  assert_eq "systemd" "$(resolve_deploy_runtime)" "DEPLOY_RUNTIME 유효성 검증"
}

test_extract_ecr_region() {
  assert_eq "ap-northeast-2" "$(extract_ecr_region "160885260227.dkr.ecr.ap-northeast-2.amazonaws.com")" "ECR 리전 파싱"
}

test_extract_ecr_region_invalid() {
  if extract_ecr_region "example.com" >/dev/null 2>&1; then
    echo "[FAIL] 잘못된 registry가 파싱되었습니다." >&2
    exit 1
  fi
}

main() {
  test_resolve_deploy_runtime_default
  test_resolve_deploy_runtime_docker
  test_resolve_deploy_runtime_invalid
  test_extract_ecr_region
  test_extract_ecr_region_invalid
  echo "[PASS] deploy-runtime helper tests"
}

main "$@"

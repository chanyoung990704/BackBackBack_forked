#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <base_run_id>"
  echo "Example: $0 20260211-vt-scope"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE_RUN_ID="$1"

run_case() {
  local case_name="$1"
  local run_id="${BASE_RUN_ID}-${case_name}"
  shift

  echo "[matrix] case=${case_name} run_id=${run_id}"
  PATH="${HOME}/tools/apache-jmeter-5.6.3/bin:${PATH}" \
    "${ROOT_DIR}/perf/scripts/run-before.sh" "${run_id}"
  PATH="${HOME}/tools/apache-jmeter-5.6.3/bin:${PATH}" "$@" \
    "${ROOT_DIR}/perf/scripts/run-after.sh" "${run_id}"
  "${ROOT_DIR}/perf/scripts/compare.sh" "${run_id}"
  echo "[matrix] completed case=${case_name}"
}

run_case "all-on" \
  env \
  SPRING_THREADS_VIRTUAL_ENABLED=true \
  APP_VIRTUAL_THREAD_ENABLED=true \
  APP_VIRTUAL_THREAD_INSIGHT_ENABLED=true \
  APP_VIRTUAL_THREAD_EMAIL_ENABLED=true

run_case "insight-only" \
  env \
  SPRING_THREADS_VIRTUAL_ENABLED=true \
  APP_VIRTUAL_THREAD_ENABLED=false \
  APP_VIRTUAL_THREAD_INSIGHT_ENABLED=true \
  APP_VIRTUAL_THREAD_EMAIL_ENABLED=false

run_case "email-only" \
  env \
  SPRING_THREADS_VIRTUAL_ENABLED=true \
  APP_VIRTUAL_THREAD_ENABLED=false \
  APP_VIRTUAL_THREAD_INSIGHT_ENABLED=false \
  APP_VIRTUAL_THREAD_EMAIL_ENABLED=true

run_case "global-only" \
  env \
  SPRING_THREADS_VIRTUAL_ENABLED=true \
  APP_VIRTUAL_THREAD_ENABLED=true \
  APP_VIRTUAL_THREAD_INSIGHT_ENABLED=false \
  APP_VIRTUAL_THREAD_EMAIL_ENABLED=false

echo "[matrix] all cases completed under perf/results/${BASE_RUN_ID}-*"

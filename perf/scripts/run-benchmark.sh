#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <before|after> [run_id]"
  exit 1
fi

MODE="$1"
RUN_ID="${2:-$(date +%Y%m%d-%H%M%S)}"

if [[ "$MODE" != "before" && "$MODE" != "after" ]]; then
  echo "MODE must be before or after"
  exit 1
fi

if ! command -v jmeter >/dev/null 2>&1; then
  echo "jmeter command not found"
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RESULT_DIR="${ROOT_DIR}/perf/results/${RUN_ID}/${MODE}"
mkdir -p "${RESULT_DIR}"
HTML_REPORT_DIR="${RESULT_DIR}/html-report"
RESULT_JTL="${RESULT_DIR}/results.jtl"
RUNTIME_METRICS_CSV="${RESULT_DIR}/runtime-metrics.csv"
RUNTIME_METRICS_MD="${RESULT_DIR}/runtime-metrics-summary.md"
RUNTIME_METRICS_TSV="${RESULT_DIR}/runtime-metrics-summary.tsv"

if [[ "${MODE}" == "before" ]]; then
  VT_ENABLED=false
else
  VT_ENABLED=true
fi

# after 실험에서 필요하면 기능별 플래그를 개별 오버라이드한다.
VT_GLOBAL="${APP_VIRTUAL_THREAD_ENABLED:-${VT_ENABLED}}"
VT_INSIGHT="${APP_VIRTUAL_THREAD_INSIGHT_ENABLED:-${VT_ENABLED}}"
VT_EMAIL="${APP_VIRTUAL_THREAD_EMAIL_ENABLED:-${VT_ENABLED}}"
VT_SPRING_THREADS="${SPRING_THREADS_VIRTUAL_ENABLED:-${VT_ENABLED}}"

pushd "${ROOT_DIR}" >/dev/null

echo "[${MODE}] starting app with perf profile..."
SPRING_PROFILES_ACTIVE=perf \
SPRING_THREADS_VIRTUAL_ENABLED="${VT_SPRING_THREADS}" \
APP_VIRTUAL_THREAD_ENABLED="${VT_GLOBAL}" \
APP_VIRTUAL_THREAD_INSIGHT_ENABLED="${VT_INSIGHT}" \
APP_VIRTUAL_THREAD_EMAIL_ENABLED="${VT_EMAIL}" \
./gradlew bootRun --no-daemon >"${RESULT_DIR}/server.log" 2>&1 &
SERVER_PID=$!

cleanup() {
  if [[ -n "${METRICS_PID:-}" ]] && ps -p "${METRICS_PID}" >/dev/null 2>&1; then
    kill "${METRICS_PID}" >/dev/null 2>&1 || true
    wait "${METRICS_PID}" >/dev/null 2>&1 || true
  fi
  if ps -p "${SERVER_PID}" >/dev/null 2>&1; then
    kill "${SERVER_PID}" >/dev/null 2>&1 || true
    wait "${SERVER_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

echo "[${MODE}] waiting for server..."
READY=false
for _ in {1..180}; do
  # bootRun 프로세스가 먼저 종료되면 즉시 실패로 처리한다.
  if ! ps -p "${SERVER_PID}" >/dev/null 2>&1; then
    echo "server process exited before ready"
    tail -n 80 "${RESULT_DIR}/server.log" || true
    exit 1
  fi

  # 내가 띄운 애플리케이션이 실제로 기동됐는지 로그 마커로 확인한다.
  if grep -q "Started ProjectApplication" "${RESULT_DIR}/server.log" \
    && grep -q "Tomcat started on port 8080" "${RESULT_DIR}/server.log" \
    && curl -sf "http://localhost:8080/api/perf/benchmark/fixture" >/dev/null; then
    READY=true
    break
  fi
  sleep 1
done

if [[ "${READY}" != "true" ]]; then
  echo "server not ready in time"
  exit 1
fi

# 워밍업 직후에도 살아있는지 한 번 더 검증한다.
sleep 2
if ! ps -p "${SERVER_PID}" >/dev/null 2>&1; then
  echo "server process exited right after ready"
  tail -n 80 "${RESULT_DIR}/server.log" || true
  exit 1
fi
if ! curl -sf "http://localhost:8080/api/perf/benchmark/fixture" >/dev/null; then
  echo "server is not reachable right before jmeter"
  exit 1
fi

echo "[${MODE}] running jmeter..."
# 같은 RUN_ID 재실행 시 JMeter HTML 리포트 폴더 충돌을 방지한다.
rm -rf "${HTML_REPORT_DIR}"
rm -f "${RESULT_JTL}"
rm -f "${RUNTIME_METRICS_CSV}" "${RUNTIME_METRICS_MD}" "${RUNTIME_METRICS_TSV}"
"${ROOT_DIR}/perf/scripts/collect-runtime-metrics.sh" "${RUNTIME_METRICS_CSV}" 1 >"${RESULT_DIR}/runtime-metrics.log" 2>&1 &
METRICS_PID=$!
jmeter -n \
  -t "${ROOT_DIR}/perf/jmeter/phase1-virtual-thread-benchmark.jmx" \
  -q "${ROOT_DIR}/perf/jmeter/props/common.properties" \
  -Jcompany.id=1 \
  -Jcompany.stock.code=900001 \
  -l "${RESULT_JTL}" \
  -e -o "${HTML_REPORT_DIR}" \
  >"${RESULT_DIR}/jmeter.log" 2>&1
if [[ -n "${METRICS_PID:-}" ]] && ps -p "${METRICS_PID}" >/dev/null 2>&1; then
  kill "${METRICS_PID}" >/dev/null 2>&1 || true
  wait "${METRICS_PID}" >/dev/null 2>&1 || true
fi
"${ROOT_DIR}/perf/scripts/summarize-runtime-metrics.sh" \
  "${RUNTIME_METRICS_CSV}" \
  "${RUNTIME_METRICS_MD}" \
  "${RUNTIME_METRICS_TSV}"

cat > "${RESULT_DIR}/run-meta.env" <<EOF
RUN_ID=${RUN_ID}
MODE=${MODE}
SPRING_PROFILES_ACTIVE=perf
SPRING_THREADS_VIRTUAL_ENABLED=${VT_SPRING_THREADS}
APP_VIRTUAL_THREAD_ENABLED=${VT_GLOBAL}
APP_VIRTUAL_THREAD_INSIGHT_ENABLED=${VT_INSIGHT}
APP_VIRTUAL_THREAD_EMAIL_ENABLED=${VT_EMAIL}
RUNTIME_METRICS_CSV=${RUNTIME_METRICS_CSV}
RUNTIME_METRICS_SUMMARY_MD=${RUNTIME_METRICS_MD}
EOF

echo "[${MODE}] completed: ${RESULT_DIR}"
popd >/dev/null

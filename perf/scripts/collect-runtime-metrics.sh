#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <output_csv> [interval_sec]"
  exit 1
fi

OUTPUT_CSV="$1"
INTERVAL_SEC="${2:-1}"
BASE_URL="${ACTUATOR_BASE_URL:-http://localhost:8080/actuator}"

metric_json() {
  local metric_name="$1"
  local query="${2:-}"
  curl -sf "${BASE_URL}/metrics/${metric_name}${query}" || true
}

extract_stat_value() {
  local json="$1"
  local stat="$2"
  local value
  value="$(
    printf '%s' "${json}" \
      | tr -d '\n' \
      | grep -o "\"statistic\":\"${stat}\",\"value\":[-0-9.eE+]*" \
      | head -1 \
      | cut -d':' -f3
  )"
  if [[ -z "${value}" ]]; then
    echo "NaN"
  else
    echo "${value}"
  fi
}

extract_first_value() {
  local json="$1"
  local value
  value="$(
    printf '%s' "${json}" \
      | tr -d '\n' \
      | grep -o '"value":[-0-9.eE+]*' \
      | head -1 \
      | cut -d':' -f2
  )"
  if [[ -z "${value}" ]]; then
    echo "NaN"
  else
    echo "${value}"
  fi
}

echo "ts,process_cpu_usage,system_cpu_usage,jvm_threads_live,hikari_active,hikari_pending,tomcat_threads_busy,gc_pause_count,gc_pause_total_time" > "${OUTPUT_CSV}"

while true; do
  ts="$(date +%s)"

  process_cpu_json="$(metric_json "process.cpu.usage")"
  system_cpu_json="$(metric_json "system.cpu.usage")"
  threads_live_json="$(metric_json "jvm.threads.live")"
  hikari_active_json="$(metric_json "hikaricp.connections.active")"
  hikari_pending_json="$(metric_json "hikaricp.connections.pending")"
  tomcat_busy_json="$(metric_json "tomcat.threads.busy")"
  gc_pause_json="$(metric_json "jvm.gc.pause")"

  process_cpu="$(extract_first_value "${process_cpu_json}")"
  system_cpu="$(extract_first_value "${system_cpu_json}")"
  threads_live="$(extract_first_value "${threads_live_json}")"
  hikari_active="$(extract_first_value "${hikari_active_json}")"
  hikari_pending="$(extract_first_value "${hikari_pending_json}")"
  tomcat_busy="$(extract_first_value "${tomcat_busy_json}")"
  gc_pause_count="$(extract_stat_value "${gc_pause_json}" "COUNT")"
  gc_pause_total_time="$(extract_stat_value "${gc_pause_json}" "TOTAL_TIME")"

  echo "${ts},${process_cpu},${system_cpu},${threads_live},${hikari_active},${hikari_pending},${tomcat_busy},${gc_pause_count},${gc_pause_total_time}" >> "${OUTPUT_CSV}"
  sleep "${INTERVAL_SEC}"
done

#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 3 ]]; then
  echo "Usage: $0 <input_csv> <output_md> <output_tsv>"
  exit 1
fi

INPUT_CSV="$1"
OUTPUT_MD="$2"
OUTPUT_TSV="$3"

awk -F',' '
  NR == 1 {
    for (i = 2; i <= NF; i++) {
      cols[i] = $i
      sum[i] = 0
      cnt[i] = 0
      max[i] = ""
    }
    next
  }
  {
    samples++
    for (i = 2; i <= NF; i++) {
      if ($i != "NaN" && $i != "") {
        v = $i + 0
        sum[i] += v
        cnt[i]++
        if (max[i] == "" || v > max[i]) {
          max[i] = v
        }
      }
    }
  }
  END {
    print "# Runtime Metrics Summary"
    print ""
    print "- samples: " samples
    print ""
    print "| metric | avg | max |"
    print "|---|---:|---:|"
    for (i = 2; i <= length(cols) + 1; i++) {
      if (cnt[i] > 0) {
        avg = sum[i] / cnt[i]
        printf "| %s | %.6f | %.6f |\n", cols[i], avg, max[i]
      } else {
        printf "| %s | NaN | NaN |\n", cols[i]
      }
    }
  }
' "${INPUT_CSV}" > "${OUTPUT_MD}"

awk -F',' '
  NR == 1 {
    for (i = 2; i <= NF; i++) {
      cols[i] = $i
      sum[i] = 0
      cnt[i] = 0
      max[i] = ""
    }
    next
  }
  {
    for (i = 2; i <= NF; i++) {
      if ($i != "NaN" && $i != "") {
        v = $i + 0
        sum[i] += v
        cnt[i]++
        if (max[i] == "" || v > max[i]) {
          max[i] = v
        }
      }
    }
  }
  END {
    for (i = 2; i <= length(cols) + 1; i++) {
      if (cnt[i] > 0) {
        avg = sum[i] / cnt[i]
        printf "%s\t%.6f\t%.6f\n", cols[i], avg, max[i]
      } else {
        printf "%s\tNaN\tNaN\n", cols[i]
      }
    }
  }
' "${INPUT_CSV}" > "${OUTPUT_TSV}"

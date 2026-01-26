#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "[ERROR] root 권한으로 실행해야 합니다." >&2
  exit 1
fi

if command -v dnf >/dev/null 2>&1; then
  PM="dnf"
else
  PM="yum"
fi

if ! command -v curl >/dev/null 2>&1; then
  "$PM" install -y curl
fi

if ! command -v ruby >/dev/null 2>&1; then
  "$PM" install -y ruby
fi

REGION="${AWS_REGION:-}"
if [ -z "$REGION" ]; then
  TOKEN="$(curl -sX PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 60" || true)"
  REGION="$(curl -sH "X-aws-ec2-metadata-token: $TOKEN" \
    http://169.254.169.254/latest/dynamic/instance-identity/document \
    | grep -oE '\"region\"\\s*:\\s*\"[^\"]+\"' \
    | cut -d '\"' -f4 || true)"
fi

if [ -z "$REGION" ]; then
  echo "[ERROR] AWS_REGION을 찾지 못했습니다. AWS_REGION을 지정하세요." >&2
  exit 1
fi

CODEDEPLOY_URL="https://aws-codedeploy-${REGION}.s3.${REGION}.amazonaws.com/latest/install"
curl -fsSL "$CODEDEPLOY_URL" -o /tmp/codedeploy-install
chmod +x /tmp/codedeploy-install
/tmp/codedeploy-install auto
systemctl enable --now codedeploy-agent

echo "[INFO] CodeDeploy Agent 설치 완료"

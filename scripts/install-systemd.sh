#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  echo "[ERROR] root 권한으로 실행해야 합니다." >&2
  exit 1
fi

APP_NAME="${APP_NAME:-backbackback}"
APP_DIR="${APP_DIR:-/opt/project}"

SYSTEMD_DIR="/etc/systemd/system"
ENV_DIR="/etc/${APP_NAME}"
SERVICE_FILE="${SYSTEMD_DIR}/${APP_NAME}.service"
ENV_FILE="${ENV_DIR}/${APP_NAME}.env"

SERVICE_SOURCE="${APP_DIR}/scripts/systemd/${APP_NAME}.service"
ENV_SOURCE="${APP_DIR}/scripts/systemd/${APP_NAME}.env.example"

mkdir -p "$ENV_DIR"

if [ ! -f "$ENV_FILE" ]; then
  if [ -f "$ENV_SOURCE" ]; then
    cp "$ENV_SOURCE" "$ENV_FILE"
  else
    cat > "$ENV_FILE" <<'EOF'
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms128m -Xmx256m
EOF
  fi
  echo "[WARN] 환경 변수 파일을 생성했습니다. 값 확인 후 수정하세요: $ENV_FILE" >&2
fi

chown root:root "$ENV_FILE"
chmod 600 "$ENV_FILE"

if [ -f "$SERVICE_SOURCE" ]; then
  cp "$SERVICE_SOURCE" "$SERVICE_FILE"
else
  cat > "$SERVICE_FILE" <<'EOF'
[Unit]
Description=BackBackBack Spring Boot Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/project
EnvironmentFile=/etc/backbackback/backbackback.env
ExecStart=/usr/bin/java $JAVA_OPTS -jar /opt/project/app.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF
fi

chown root:root "$SERVICE_FILE"
chmod 644 "$SERVICE_FILE"

systemctl daemon-reload
systemctl enable "$APP_NAME"
echo "[INFO] systemd 서비스 설치 완료: $SERVICE_FILE"

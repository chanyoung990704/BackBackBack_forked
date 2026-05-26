#!/bin/bash
# Shell script for setting up the Python Mock AI Worker virtual environment and dependencies.
# 이 스크립트는 가상환경(venv)을 구성하고 필수 라이브러리를 자동으로 설치합니다.

set -e

# ANSI escape codes for beautiful styling
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

echo -e "${CYAN}==========================================================${NC}"
echo -e "${CYAN}         Mock AI Worker Python Virtual Environment Setup  ${NC}"
echo -e "${CYAN}==========================================================${NC}"

# 1. Python 설치 여부 확인
if ! command -v python3 &> /dev/null; then
    echo -e "${YELLOW}python3 not found. Trying python...${NC}"
    if ! command -v python &> /dev/null; then
        echo -e "${YELLOW}Python is not installed. Please install Python 3.10+ first.${NC}"
        exit 1
    else
        PYTHON_CMD="python"
    fi
else
    PYTHON_CMD="python3"
fi

python_version=$($PYTHON_CMD --version)
echo -e "Detected Python: ${GREEN}${python_version}${NC}"

# 2. 가상환경 생성 (venv)
if [ ! -d "venv" ]; then
    echo -e "Creating virtual environment 'venv'..."
    $PYTHON_CMD -m venv venv
    echo -e "${GREEN}Virtual environment created successfully.${NC}"
else
    echo -e "Virtual environment 'venv' already exists. Skipping creation."
fi

# 3. Pip 업그레이드 및 requirements.txt 종속성 설치
echo -e "Activating virtual environment & installing dependencies..."
source venv/bin/activate

echo -e "Upgrading pip..."
pip install --upgrade pip

echo -e "Installing requirements from requirements.txt..."
pip install -r requirements.txt

echo -e "${GREEN}==========================================================${NC}"
echo -e "${GREEN}             Setup Completed Successfully!                ${NC}"
echo -e "${GREEN}==========================================================${NC}"
echo ""
echo -e "To run the Mock AI Worker:"
echo -e "  1. ${GRAY}(Optional) Start your local Kafka server on localhost:9092${NC}"
echo -e "  2. Run the application:"
echo -e "     ${YELLOW}source venv/bin/activate && python main.py${NC}"
echo ""
echo -e "The worker will automatically start a background thread to consume"
echo -e "requests from topic 'ai-job-request' and push responses to 'ai-job-response'."
echo -e "The health check endpoint is available at: ${CYAN}http://localhost:8082/health${NC}"
echo -e "${GREEN}==========================================================${NC}"

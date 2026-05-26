# PowerShell script for setting up the Python Mock AI Worker virtual environment and dependencies.
# 이 스크립트는 가상환경(venv)을 구성하고 필수 라이브러리를 자동으로 설치합니다.

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "         Mock AI Worker Python Virtual Environment Setup  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Python 설치 여부 확인
try {
    $pythonVersion = python --version
    Write-Host "Detected Python: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Error "Python is not installed or not added to your PATH environment. Please install Python 3.10+ first."
    Exit 1
}

# 2. 가상환경 생성 (venv)
if (-not (Test-Path "venv")) {
    Write-Host "Creating virtual environment 'venv'..." -ForegroundColor Yellow
    python -m venv venv
    Write-Host "Virtual environment created successfully." -ForegroundColor Green
} else {
    Write-Host "Virtual environment 'venv' already exists. Skipping creation." -ForegroundColor Yellow
}

# 3. Pip 업그레이드 및 requirements.txt 종속성 설치
Write-Host "Activating virtual environment & installing dependencies..." -ForegroundColor Yellow

# Windows PowerShell 보안 정책 우회하여 활성화 스크립트 실행 또는 직접 가상환경 내 pip 실행
$pipPath = "venv\Scripts\pip.exe"
$pythonExec = "venv\Scripts\python.exe"

if (-not (Test-Path $pipPath)) {
    Write-Error "Failed to locate virtual environment pip. Check if venv creation was successful."
    Exit 1
}

Write-Host "Upgrading pip..." -ForegroundColor Yellow
& $pythonExec -m pip install --upgrade pip

Write-Host "Installing requirements from requirements.txt..." -ForegroundColor Yellow
& $pipPath install -r requirements.txt

Write-Host "==========================================================" -ForegroundColor Green
Write-Host "             Setup Completed Successfully!                " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
Write-Host "To run the Mock AI Worker:" -ForegroundColor Cyan
Write-Host "  1. (Optional) Start your local Kafka server on localhost:9092" -ForegroundColor Gray
Write-Host "  2. Run the application using the virtual environment python:" -ForegroundColor Gray
Write-Host "     & venv\Scripts\python.exe main.py" -ForegroundColor Yellow
Write-Host ""
Write-Host "The worker will automatically start a background thread to consume" -ForegroundColor White
Write-Host "requests from topic 'ai-job-request' and push responses to 'ai-job-response'." -ForegroundColor White
Write-Host "The health check endpoint is available at: http://localhost:8082/health" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green

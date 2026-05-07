$ErrorActionPreference = 'Stop'
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Initializing ML Backends...             " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$VENV_DIR = "venv"
$PYTHON_CMD = "python"

# 1. Setup Virtual Environment
if (-Not (Test-Path "$VENV_DIR\Scripts\python.exe")) {
    Write-Host "[1/4] Creating Python virtual environment..." -ForegroundColor Yellow
    & $PYTHON_CMD -m venv $VENV_DIR
} else {
    Write-Host "[1/4] Virtual environment already exists." -ForegroundColor Green
}

# 2. Install Requirements
Write-Host "[2/4] Checking and installing requirements (this may take a moment on first run)..." -ForegroundColor Yellow
$PIP = ".\$VENV_DIR\Scripts\pip.exe"
& $PIP install -q --no-cache-dir --default-timeout=100 -r arduino_bot\requirements.txt
& $PIP install -q --no-cache-dir --default-timeout=100 -r pdf_bot\requirements.txt
Write-Host "Requirements check complete." -ForegroundColor Green

# Function to check if a port is in use
function Test-Port {
    param([int]$Port)
    $connection = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return $null -ne $connection
}

# 3. Start Arduino Bot
Write-Host "[3/4] Checking Arduino Bot (Port 8000)..." -ForegroundColor Yellow
if (Test-Port 8000) {
    Write-Host "Arduino Bot is already running on port 8000." -ForegroundColor Green
} else {
    Write-Host "Starting Arduino Bot in a new window..." -ForegroundColor Cyan
    Start-Process -FilePath ".\$VENV_DIR\Scripts\python.exe" -ArgumentList "app.py" -WorkingDirectory ".\arduino_bot" -WindowStyle Normal
}

# 4. Start PDF Bot
Write-Host "[4/4] Checking PDF Bot (Port 5000)..." -ForegroundColor Yellow
if (Test-Port 5000) {
    Write-Host "PDF Bot is already running on port 5000." -ForegroundColor Green
} else {
    Write-Host "Starting PDF Bot in a new window..." -ForegroundColor Cyan
    Start-Process -FilePath ".\$VENV_DIR\Scripts\python.exe" -ArgumentList "app.py" -WorkingDirectory ".\pdf_bot" -WindowStyle Normal
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  ML Backends Initialization Complete!    " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

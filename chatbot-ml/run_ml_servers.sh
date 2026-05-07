#!/bin/bash
echo "=========================================="
echo "  Initializing ML Backends...             "
echo "=========================================="

VENV_DIR="venv"
PYTHON_CMD="python3"

if [ ! -f "$VENV_DIR/bin/python" ]; then
    echo "[1/4] Creating Python virtual environment..."
    $PYTHON_CMD -m venv $VENV_DIR
else
    echo "[1/4] Virtual environment already exists."
fi

echo "[2/4] Checking and installing requirements..."
PIP="./$VENV_DIR/bin/pip"
$PIP install -q -r arduino_bot/requirements.txt
$PIP install -q -r pdf_bot/requirements.txt
echo "Requirements check complete."

if ! lsof -Pi :8000 -sTCP:LISTEN -t >/dev/null ; then
    echo "[3/4] Starting Arduino Bot..."
    nohup ./$VENV_DIR/bin/python arduino_bot/app.py > arduino_bot.log 2>&1 &
else
    echo "[3/4] Arduino Bot already running on port 8000."
fi

if ! lsof -Pi :5000 -sTCP:LISTEN -t >/dev/null ; then
    echo "[4/4] Starting PDF Bot..."
    nohup ./$VENV_DIR/bin/python pdf_bot/app.py > pdf_bot.log 2>&1 &
else
    echo "[4/4] PDF Bot already running on port 5000."
fi

echo "=========================================="
echo "  ML Backends Initialization Complete!    "
echo "=========================================="

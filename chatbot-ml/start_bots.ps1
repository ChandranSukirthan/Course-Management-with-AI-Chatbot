Write-Host "Starting Duinophile ML Backends..." -ForegroundColor Cyan

# Start Arduino Bot
Write-Host "Starting Arduino Bot (Port 8000)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "app.py" -WorkingDirectory ".\arduino_bot" -WindowStyle Normal

# Start PDF Bot
Write-Host "Starting PDF Bot (Port 5000)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "app.py" -WorkingDirectory ".\pdf_bot" -WindowStyle Normal

Write-Host "Both ML models are starting in separate windows!" -ForegroundColor Yellow

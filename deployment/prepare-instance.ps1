# WordAI ARM Instance Preparation Script
# Run this from PowerShell BEFORE uploading files

$IP = "130.162.184.150"
$KEY = "C:\Users\johnm\.ssh\arm-wordai.key"

Write-Host "=== Preparing OCI ARM Instance for WordAI ===" -ForegroundColor Green
Write-Host ""

# Create directories on remote server
Write-Host "Step 1: Creating application directories..." -ForegroundColor Yellow
ssh -i $KEY opc@$IP "mkdir -p ~/wordai-app ~/wordai-app/logs ~/wordai-data"

Write-Host "Directories created" -ForegroundColor Green
Write-Host ""

# Verify directories
Write-Host "Step 2: Verifying directory structure..." -ForegroundColor Yellow
ssh -i $KEY opc@$IP "ls -la ~ | grep wordai"

Write-Host ""
Write-Host "=== Instance Prepared Successfully ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  1. Upload JAR file" -ForegroundColor White
Write-Host "  2. Upload service file" -ForegroundColor White
Write-Host "  3. Upload configuration" -ForegroundColor White
Write-Host "  4. Run deployment script" -ForegroundColor White
Write-Host ""

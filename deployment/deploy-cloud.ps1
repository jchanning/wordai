# Deploy WordAI to Oracle Cloud
# Usage: .\deployment\deploy-cloud.ps1
# Requires: deployment-config.ps1 with IP and KEY variables

# Load configuration
$configFile = "$PSScriptRoot\deployment-config.ps1"
if (-not (Test-Path $configFile)) {
    Write-Host "ERROR: deployment-config.ps1 not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Create deployment-config.ps1 with:" -ForegroundColor Yellow
    Write-Host '  $IP = "YOUR_SERVER_IP"' -ForegroundColor White
    Write-Host '  $KEY = "PATH_TO_SSH_KEY"' -ForegroundColor White
    Write-Host ""
    Write-Host "Example: deployment-config.ps1.example" -ForegroundColor Cyan
    exit 1
}

. $configFile

Write-Host "=== Deploying WordAI to Oracle Cloud ===" -ForegroundColor Green
Write-Host "Target: $IP" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build for cloud
Write-Host "Step 1: Building application for cloud (Java 17)..." -ForegroundColor Yellow
& "$PSScriptRoot\build-cloud.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting deployment." -ForegroundColor Red
    exit 1
}

# Step 2: Upload JAR
Write-Host ""
Write-Host "Step 2: Uploading JAR to server..." -ForegroundColor Yellow
scp -i $KEY target\wordai-1.5.0.jar opc@${IP}:~/wordai-app/wordai.jar

# Step 3: Upload configuration
Write-Host ""
Write-Host "Step 3: Uploading configuration files..." -ForegroundColor Yellow
scp -i $KEY deployment\wordai.properties opc@${IP}:~/wordai-app/
scp -i $KEY deployment\setup-dictionaries.sh opc@${IP}:~/wordai-app/
scp -i $KEY deployment\wordai.service opc@${IP}:~/

# Step 4: Extract dictionaries and restart
Write-Host ""
Write-Host "Step 4: Setting up dictionaries and restarting service..." -ForegroundColor Yellow
ssh -i $KEY opc@$IP "cd ~/wordai-app; chmod +x setup-dictionaries.sh; ./setup-dictionaries.sh; if [ -f ~/wordai.service ]; then sudo cp ~/wordai.service /etc/systemd/system/wordai.service; sudo systemctl daemon-reload; fi; sudo systemctl restart wordai; sleep 3; sudo systemctl status wordai --no-pager"

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Application URL: http://${IP}:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  View logs:    ssh -i $KEY opc@$IP 'sudo journalctl -u wordai -f'" -ForegroundColor White
Write-Host "  Check status: ssh -i $KEY opc@$IP 'sudo systemctl status wordai'" -ForegroundColor White
Write-Host "  Restart:      ssh -i $KEY opc@$IP 'sudo systemctl restart wordai'" -ForegroundColor White
Write-Host ""

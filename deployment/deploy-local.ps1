# Deploy WordAI to Local Linux Server (ZorinOS / AI-Server)
# Usage: .\deployment\deploy-local.ps1
# Requires: deployment-config-local.ps1 with HOST, USER, KEY variables
#
# First-time setup: run deployment\setup-local.sh on the server first.
#   scp -i $KEY deployment\setup-local.sh ${USER}@${HOST}:~/
#   ssh -i $KEY ${USER}@${HOST} "chmod +x ~/setup-local.sh && ~/setup-local.sh"

# Load configuration
$configFile = "$PSScriptRoot\deployment-config-local.ps1"
if (-not (Test-Path $configFile)) {
    Write-Host "ERROR: deployment-config-local.ps1 not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Create deployment-config-local.ps1 with:" -ForegroundColor Yellow
    Write-Host '  $HOST = "AI-Server"'                            -ForegroundColor White
    Write-Host '  $USER = "jchanning"'                            -ForegroundColor White
    Write-Host '  $KEY  = "C:\Users\johnm\.ssh\zorin-wordai.key"' -ForegroundColor White
    Write-Host ""
    Write-Host "See deployment-config-local.ps1.example for a template." -ForegroundColor Cyan
    exit 1
}

. $configFile

Write-Host "=== Deploying WordAI to Local Linux Server ===" -ForegroundColor Green
Write-Host "Target: $USER@$HOST" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build (reuse the cloud build — same JAR runs on any Linux/JVM 17+)
Write-Host "Step 1: Building application..." -ForegroundColor Yellow
& "$PSScriptRoot\build-cloud.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting deployment." -ForegroundColor Red
    exit 1
}

# Step 2: Upload JAR
Write-Host ""
Write-Host "Step 2: Uploading JAR to server..." -ForegroundColor Yellow
$jarFile = Get-ChildItem target/wordai-*.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jarFile) {
    Write-Host "ERROR: No JAR file found in target directory!" -ForegroundColor Red
    exit 1
}
Write-Host "Using: $($jarFile.Name)" -ForegroundColor Cyan
scp -i $KEY $jarFile.FullName "${USER}@${HOST}:~/wordai-app/wordai.jar"

# Step 3: Upload configuration files
Write-Host ""
Write-Host "Step 3: Uploading configuration files..." -ForegroundColor Yellow
scp -i $KEY deployment\wordai-local.properties "${USER}@${HOST}:~/wordai-app/wordai.properties"
scp -i $KEY deployment\setup-dictionaries.sh   "${USER}@${HOST}:~/wordai-app/"
scp -i $KEY deployment\wordai-local.service    "${USER}@${HOST}:~/"

# Step 4: Install service (if new) and restart
Write-Host ""
Write-Host "Step 4: Installing service and restarting..." -ForegroundColor Yellow
ssh -i $KEY "${USER}@${HOST}" @"
    set -e
    cd ~/wordai-app
    chmod +x setup-dictionaries.sh
    ./setup-dictionaries.sh
    if [ -f ~/wordai-local.service ]; then
        sudo cp ~/wordai-local.service /etc/systemd/system/wordai.service
        sudo systemctl daemon-reload
        sudo systemctl enable wordai
    fi
    sudo systemctl restart wordai
    sleep 3
    sudo systemctl status wordai --no-pager
"@

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Application URL: http://${HOST}:8080"          -ForegroundColor Cyan
Write-Host "H2 Console:      http://${HOST}:8080/h2-console" -ForegroundColor Cyan
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  View logs:    ssh -i $KEY ${USER}@$HOST 'sudo journalctl -u wordai -f'"       -ForegroundColor White
Write-Host "  Check status: ssh -i $KEY ${USER}@$HOST 'sudo systemctl status wordai'"       -ForegroundColor White
Write-Host "  Restart:      ssh -i $KEY ${USER}@$HOST 'sudo systemctl restart wordai'"      -ForegroundColor White
Write-Host "  Stop:         ssh -i $KEY ${USER}@$HOST 'sudo systemctl stop wordai'"         -ForegroundColor White
Write-Host ""

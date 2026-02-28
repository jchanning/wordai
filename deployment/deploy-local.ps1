# Deploy WordAI to Local Linux Server (ZorinOS / AI-Server)
# Usage: .\deployment\deploy-local.ps1
# Requires: deployment-config-local.ps1 with IP, USER, KEY variables
#
# First-time setup: run deployment\setup-local.sh on the server first.
#   scp -i $KEY deployment\setup-local.sh ${USER}@${IP}:~/
#   ssh -i $KEY ${USER}@${IP} "chmod +x ~/setup-local.sh && ~/setup-local.sh"

# Load configuration
$configFile = "$PSScriptRoot\deployment-config-local.ps1"
if (-not (Test-Path $configFile)) {
    Write-Host "ERROR: deployment-config-local.ps1 not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Create deployment-config-local.ps1 with:" -ForegroundColor Yellow
    Write-Host '  $IP = "AI-Server"'                            -ForegroundColor White
    Write-Host '  $USER = "jchanning"'                            -ForegroundColor White
    Write-Host '  $KEY  = "C:\Users\johnm\.ssh\AI-Server.key"' -ForegroundColor White
    Write-Host ""
    Write-Host "See deployment-config-local.ps1.example for a template." -ForegroundColor Cyan
    exit 1
}

. $configFile

Write-Host "=== Deploying WordAI to Local Linux Server ===" -ForegroundColor Green
Write-Host "Target: $USER@$IP" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build (reuse the cloud build — same JAR runs on any Linux/JVM 17+)
Write-Host "Step 1: Building application..." -ForegroundColor Yellow
& "$PSScriptRoot\build-cloud.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed! Aborting deployment." -ForegroundColor Red
    exit 1
}

# Step 2: Ensure application directories exist on server
Write-Host ""
Write-Host "Step 2: Preparing server directories..." -ForegroundColor Yellow
ssh -i $KEY "${USER}@${IP}" "mkdir -p ~/wordai-app/logs ~/wordai-data/analysis"

# Step 3: Upload JAR
Write-Host ""
Write-Host "Step 3: Uploading JAR to server..." -ForegroundColor Yellow
$jarFile = Get-ChildItem target/wordai-*.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jarFile) {
    Write-Host "ERROR: No JAR file found in target directory!" -ForegroundColor Red
    exit 1
}
Write-Host "Using: $($jarFile.Name)" -ForegroundColor Cyan
scp -i $KEY $jarFile.FullName "${USER}@${IP}:~/wordai-app/wordai.jar"

# Step 4: Upload configuration files
Write-Host ""
Write-Host "Step 4: Uploading configuration files..." -ForegroundColor Yellow
scp -i $KEY deployment\wordai-local.properties "${USER}@${IP}:~/wordai-app/wordai.properties"
scp -i $KEY deployment\wordai-local.service    "${USER}@${IP}:~/wordai-app/"

# Step 5: Install service (if new) and restart
Write-Host ""
Write-Host "Step 5: Installing service and restarting..." -ForegroundColor Yellow
ssh -i $KEY "${USER}@${IP}" "mkdir -p ~/wordai-app/logs ~/wordai-app/dictionaries ~/wordai-data/analysis; cd ~/wordai-app; jar xf wordai.jar BOOT-INF/classes/dictionaries/ 2>/dev/null; mv BOOT-INF/classes/dictionaries/*.txt dictionaries/ 2>/dev/null; rm -rf BOOT-INF 2>/dev/null; echo 'Dictionaries:' && ls dictionaries/; if [ -f ~/wordai-app/wordai-local.service ]; then sudo cp ~/wordai-app/wordai-local.service /etc/systemd/system/wordai.service; sudo systemctl daemon-reload; sudo systemctl enable wordai; fi; sudo systemctl restart wordai; sleep 5; sudo systemctl status wordai --no-pager"

Write-Host ""
Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host ""
Write-Host "Application URL: http://${IP}:8080"          -ForegroundColor Cyan
Write-Host "H2 Console:      http://${IP}:8080/h2-console" -ForegroundColor Cyan
Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  View logs:    ssh -i $KEY ${USER}@$IP 'sudo journalctl -u wordai -f'"       -ForegroundColor White
Write-Host "  Check status: ssh -i $KEY ${USER}@$IP 'sudo systemctl status wordai'"       -ForegroundColor White
Write-Host "  Restart:      ssh -i $KEY ${USER}@$IP 'sudo systemctl restart wordai'"      -ForegroundColor White
Write-Host "  Stop:         ssh -i $KEY ${USER}@$IP 'sudo systemctl stop wordai'"         -ForegroundColor White
Write-Host ""

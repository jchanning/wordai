# Backward-compatible wrapper.
# Canonical script: .\deployment\deploy.ps1

Write-Host "deploy-cloud.ps1 is deprecated. Running deploy.ps1..." -ForegroundColor Yellow
& "$PSScriptRoot\deploy.ps1"

# Build WordAI for Cloud Deployment (Java 17)
# This script builds the application with Java 17 compatibility for Oracle Cloud

Write-Host "=== Building WordAI for Cloud Deployment ===" -ForegroundColor Green
Write-Host ""

# Clean and build with cloud profile
Write-Host "Building with cloud profile (Java 17)..." -ForegroundColor Yellow
mvn clean package -DskipTests -Pcloud

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAR location: target\wordai-1.8.0.jar" -ForegroundColor Cyan
    Write-Host "Target Java version: 17" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "  1. Run .\deployment\deploy-cloud.ps1 to deploy to Oracle Cloud" -ForegroundColor White
    Write-Host "  2. Or manually upload JAR and run setup on server" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "Build failed!" -ForegroundColor Red
    exit 1
}

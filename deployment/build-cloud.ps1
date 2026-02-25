# Build WordAI for Cloud Deployment (Java 17)
# This script builds the application with Java 17 compatibility for Oracle Cloud

Write-Host "=== Building WordAI for Cloud Deployment ===" -ForegroundColor Green
Write-Host ""

# Read version from pom.xml and inject into index.html
$pomVersion = (Select-Xml -Path "$PSScriptRoot\..\pom.xml" -XPath "//*[local-name()='project']/*[local-name()='version']").Node.InnerText
Write-Host "Injecting version $pomVersion into index.html..." -ForegroundColor Yellow
$indexPath = "$PSScriptRoot\..\src\main\resources\static\index.html"
(Get-Content $indexPath -Raw) -replace '© 2026 FistralTech Ltd \| v[\d.]+', "© 2026 FistralTech Ltd | v$pomVersion" `
                             -replace '<p class="about-version">v[\d.]+</p>', "<p class=`"about-version`">v$pomVersion</p>" `
| Set-Content $indexPath -NoNewline

# Clean and build with cloud profile
Write-Host "Building with cloud profile (Java 17)..." -ForegroundColor Yellow
mvn clean package -DskipTests -Pcloud

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Build successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "JAR location: target\wordai-$pomVersion.jar" -ForegroundColor Cyan
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

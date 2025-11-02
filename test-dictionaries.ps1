# Test the dictionaries API endpoint
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/wordai/dictionaries"
$json = $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5
Write-Host $json

@echo off
echo Testing WordAI Web API...
echo.

echo 1. Testing Health Endpoint:
powershell -Command "try { $response = Invoke-RestMethod -Uri 'http://localhost:8080/api/wordai/health' -Method GET; Write-Host 'SUCCESS: Server is running' -ForegroundColor Green; $response | ConvertTo-Json } catch { Write-Host 'ERROR: Could not reach server' -ForegroundColor Red }"
echo.

echo 2. Creating a New Game:
powershell -Command "try { $response = Invoke-RestMethod -Uri 'http://localhost:8080/api/wordai/games' -Method POST -ContentType 'application/json' -Body '{}'; Write-Host 'SUCCESS: Game created' -ForegroundColor Green; $global:gameId = $response.gameId; $response | ConvertTo-Json; echo $response.gameId > game_id.tmp } catch { Write-Host 'ERROR: Could not create game' -ForegroundColor Red }"
echo.

echo 3. Making a Test Guess (HOUSE):
for /f %%i in (game_id.tmp) do set GAME_ID=%%i
powershell -Command "$gameId = Get-Content 'game_id.tmp' -Raw; $gameId = $gameId.Trim(); try { $response = Invoke-RestMethod -Uri \"http://localhost:8080/api/wordai/games/$gameId/guess\" -Method POST -ContentType 'application/json' -Body '{\"word\":\"HOUSE\"}'; Write-Host 'SUCCESS: Guess made' -ForegroundColor Green; $response | ConvertTo-Json } catch { Write-Host 'ERROR: Could not make guess' -ForegroundColor Red }"

del game_id.tmp 2>nul
echo.
echo Testing complete!
pause
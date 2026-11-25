@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not defined JAVA_HOME (
  where java >nul 2>&1 || (
    echo [ERROR] Install JDK 21+ ^(Eclipse Temurin^) and add java to PATH.
    pause
    exit /b 1
  )
)

set "OFD_HOME=%~dp0"
if exist "%OFD_HOME%ofd-env.bat" call "%OFD_HOME%ofd-env.bat"

set "OFD_DATA_DIR=%LOCALAPPDATA%\OFD-Marking\data"
if not exist "%OFD_DATA_DIR%" mkdir "%OFD_DATA_DIR%"

if not defined OFD_MODE set "OFD_MODE=mock"
if not defined SERVER_PORT set "SERVER_PORT=18443"
if not defined OFD_CLIENT_SERVER_URL set "OFD_CLIENT_SERVER_URL=http://127.0.0.1:%SERVER_PORT%"

echo ============================================
echo  OFD Marking — desktop ^(no Docker^)
echo  Data: %OFD_DATA_DIR%
echo  API:  %OFD_CLIENT_SERVER_URL%
echo  Mode: %OFD_MODE%
echo ============================================

REM Stop previous server if still running on the port
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%SERVER_PORT% " ^| findstr LISTENING') do (
  echo Stopping old process PID %%p on port %SERVER_PORT%...
  taskkill /F /PID %%p >nul 2>&1
)

start "OFD-Marking-Server" /MIN cmd /c ^
  "set OFD_DATA_DIR=%OFD_DATA_DIR%&& set OFD_MODE=%OFD_MODE%&& set OFD_API_KEY=%OFD_API_KEY%&& set OFD_INN=%OFD_INN%&& set SERVER_PORT=%SERVER_PORT%&& java -Xms256m -Xmx768m -jar ""%OFD_HOME%ofd-marking-server.jar"" --spring.profiles.active=desktop"

echo Waiting for API...
set /a tries=0
:wait_health
set /a tries+=1
powershell -NoProfile -Command "try { $r=Invoke-RestMethod -Uri 'http://127.0.0.1:%SERVER_PORT%/api/health' -TimeoutSec 2; if ($r.status -eq 'UP') { exit 0 } else { exit 1 } } catch { exit 1 }"
if %ERRORLEVEL%==0 goto start_client
if %tries% GEQ 40 (
  echo [ERROR] Server did not become healthy. See log: %OFD_DATA_DIR%\ofd-marking-server.log
  pause
  exit /b 1
)
timeout /t 2 /nobreak >nul
goto wait_health

:start_client
echo API is UP. Starting client...
if exist "%OFD_HOME%client\run-client.bat" (
  call "%OFD_HOME%client\run-client.bat"
) else (
  echo [ERROR] client\run-client.bat not found
  pause
  exit /b 1
)

echo Client closed. Server keeps running in background.
echo To stop server: Stop-OFD.bat
endlocal

@echo off
setlocal
if not defined SERVER_PORT set "SERVER_PORT=18443"
echo Stopping OFD Marking server on port %SERVER_PORT%...
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":%SERVER_PORT% " ^| findstr LISTENING') do (
  taskkill /F /PID %%p >nul 2>&1 && echo Stopped PID %%p
)
echo Done.
endlocal

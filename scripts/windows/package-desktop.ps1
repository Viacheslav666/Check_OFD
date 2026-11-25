# Builds ofd-marking-desktop-windows.zip — server JAR + JavaFX client + Start/Stop bat (no Docker).
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

Write-Host "Building server + client..."
mvn -B -pl server,client -am -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }

# Client bits into dist/client-win
& (Join-Path $PSScriptRoot "package-client.ps1")

$out = Join-Path $Root "dist\desktop"
Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path (Join-Path $out "client") | Out-Null

$serverJar = Get-ChildItem "server\target\ofd-marking-server-*.jar" |
  Where-Object { $_.Name -notmatch "original" } |
  Select-Object -First 1
if (-not $serverJar) { throw "Server jar missing" }
Copy-Item $serverJar.FullName (Join-Path $out "ofd-marking-server.jar")

# Expand client zip contents into desktop/client
$clientZip = Join-Path $Root "dist\ofd-marking-client-windows.zip"
Expand-Archive -Path $clientZip -DestinationPath (Join-Path $out "client") -Force

Copy-Item (Join-Path $PSScriptRoot "Start-OFD.bat") $out
Copy-Item (Join-Path $PSScriptRoot "Stop-OFD.bat") $out
Copy-Item (Join-Path $PSScriptRoot "central-env.example.bat") $out
Copy-Item (Join-Path $PSScriptRoot "Install-Desktop.ps1") $out -ErrorAction SilentlyContinue

@"
OFD Marking — центральное приложение (Windows, без Docker)
==========================================================
Требования: JDK 21+ (Temurin) в PATH. Docker НЕ нужен.

1. Распакуйте zip в папку, например C:\OFD-Marking
2. Запуск: Start-OFD.bat  (поднимает центральный сервер на H2 + окно клиента)
3. Во вкладке «Агенты» добавьте агентов (IP, порт, токен) — агенты ставятся на RDP клиентов
   из пакета ofd-agent-windows.zip
4. Стоп сервера: Stop-OFD.bat

Данные/логи: %LOCALAPPDATA%\OFD-Marking\data
Ключ ОФД в центре не нужен — он хранится на агентах.
"@ | Set-Content -Encoding UTF8 (Join-Path $out "README.txt")

$zip = Join-Path $Root "dist\ofd-marking-desktop-windows.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $out "*") -DestinationPath $zip
Write-Host "Built $zip"

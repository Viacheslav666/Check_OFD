# Builds a portable Windows client zip (JavaFX module-path + app jar + deps).
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

mvn -B -pl client -am -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }

$dist = Join-Path $Root "dist\client-win"
$lib = Join-Path $dist "lib"
$fx = Join-Path $dist "javafx"
Remove-Item -Recurse -Force $dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $lib | Out-Null
New-Item -ItemType Directory -Path $fx | Out-Null

# Prefer non-shaded jar for JavaFX module-path launch
$orig = Get-ChildItem "client\target\original-ofd-marking-client-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
$jar = if ($orig) { $orig } else {
  Get-ChildItem "client\target\ofd-marking-client-*-SNAPSHOT.jar" |
    Where-Object { $_.Name -notmatch "original" } |
    Select-Object -First 1
}
if (-not $jar) { throw "Client jar not found" }
Copy-Item $jar.FullName (Join-Path $dist "ofd-marking-client.jar")

# All runtime deps into lib/
mvn -B -pl client org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies `
  "-DoutputDirectory=$lib" `
  "-DincludeScope=runtime"
if ($LASTEXITCODE -ne 0) { throw "dependency copy failed" }

# Move JavaFX artifacts to javafx/ for --module-path
Get-ChildItem $lib -Filter "javafx-*.jar" | ForEach-Object {
  Move-Item $_.FullName $fx -Force
}

@'
@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1 || (
  echo Install JDK 21+ ^(Temurin^) and ensure java is on PATH.
  exit /b 1
)
if not defined OFD_CLIENT_SERVER_URL set OFD_CLIENT_SERVER_URL=http://127.0.0.1:18443
java --module-path "javafx" --add-modules javafx.controls,javafx.fxml,javafx.swing -cp "ofd-marking-client.jar;lib\*" ru.ofd.marking.client.MarkingClientApp
'@ | Set-Content -Encoding ASCII (Join-Path $dist "run-client.bat")

@'
OFD Marking Client (Windows)
============================
1. Start local API on THIS PC:  docker compose up -d
2. Double-click run-client.bat  (http://127.0.0.1:18443)
3. JDK 21+ on PATH required

OFD.ru requests are sent by the local Docker server, so the public
source IP is this machine / site — install one stack per RDP host.
'@ | Set-Content -Encoding UTF8 (Join-Path $dist "README.txt")

New-Item -ItemType Directory -Path (Join-Path $Root "dist") -Force | Out-Null
$zip = Join-Path $Root "dist\ofd-marking-client-windows.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $dist "*") -DestinationPath $zip
Write-Host "Built $zip"

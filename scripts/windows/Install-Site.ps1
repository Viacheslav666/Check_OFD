<#
.SYNOPSIS
  First-time install on a Windows RDP host (one site = one public IP for OFD).

.DESCRIPTION
  - Requires Docker Desktop
  - Pulls API image from GHCR + Postgres via docker compose
  - Downloads latest Windows client zip from GitHub Releases
  - Client talks to http://127.0.0.1:18443 so OFD egress = THIS PC
#>
param(
  [string]$Repo = "Viacheslav666/Check_OFD",
  [string]$InstallDir = "$env:USERPROFILE\OFD-Marking",
  [ValidateSet("mock", "live")]
  [string]$Mode = "mock",
  [string]$ApiKey = "",
  [string]$Inn = "7707083893",
  [int]$HostPort = 18443
)

$ErrorActionPreference = "Stop"

function Assert-Docker {
  docker version | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Docker Desktop is required and must be running." }
  docker compose version | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Docker Compose plugin is required (docker compose)." }
}

Assert-Docker
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
Set-Location $InstallDir

$composeUrl = "https://raw.githubusercontent.com/$Repo/main/docker-compose.yml"
$envExampleUrl = "https://raw.githubusercontent.com/$Repo/main/.env.example"
Invoke-WebRequest -Uri $composeUrl -OutFile "docker-compose.yml"
Invoke-WebRequest -Uri $envExampleUrl -OutFile ".env.example" -ErrorAction SilentlyContinue

@"
OFD_HOST_PORT=$HostPort
OFD_MODE=$Mode
OFD_API_KEY=$ApiKey
OFD_INN=$Inn
OFD_IMAGE=ghcr.io/viacheslav666/check_ofd:latest
"@ | Set-Content -Encoding ASCII ".env"

Write-Host "Pulling and starting API + Postgres..."
docker compose pull
docker compose up -d

# Wait for health
$ok = $false
for ($i = 0; $i -lt 30; $i++) {
  try {
    $r = Invoke-RestMethod -Uri "http://127.0.0.1:$HostPort/api/health" -TimeoutSec 3
    if ($r.status -eq "UP") { $ok = $true; break }
  } catch { Start-Sleep -Seconds 3 }
}
if (-not $ok) { Write-Warning "API health not UP yet — check: docker compose logs -f server" }
else { Write-Host "API OK: http://127.0.0.1:$HostPort/api/health" }

# Latest client release asset
$rel = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ "User-Agent" = "ofd-marking-install" }
$asset = $rel.assets | Where-Object { $_.name -eq "ofd-marking-client-windows.zip" } | Select-Object -First 1
if (-not $asset) { throw "Client zip not found in latest release. Wait for CD workflow to finish." }

$zip = Join-Path $InstallDir "ofd-marking-client-windows.zip"
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip
$clientDir = Join-Path $InstallDir "client"
Remove-Item -Recurse -Force $clientDir -ErrorAction SilentlyContinue
Expand-Archive -Path $zip -DestinationPath $clientDir -Force

Write-Host ""
Write-Host "Installed to $InstallDir"
Write-Host "  Server: docker compose ps"
Write-Host "  Client: $clientDir\run-client.bat"
Write-Host "Update later: .\Update-Site.ps1"
Write-Host ""
Write-Host "IMPORTANT: OFD.ru sees the public IP of THIS RDP machine."

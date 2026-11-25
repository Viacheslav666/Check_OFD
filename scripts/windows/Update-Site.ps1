<#
.SYNOPSIS
  Pull latest server image + client release onto this RDP host.
#>
param(
  [string]$Repo = "Viacheslav666/Check_OFD",
  [string]$InstallDir = "$env:USERPROFILE\OFD-Marking"
)

$ErrorActionPreference = "Stop"
if (-not (Test-Path (Join-Path $InstallDir "docker-compose.yml"))) {
  throw "Not installed. Run Install-Site.ps1 first (expected $InstallDir)."
}
Set-Location $InstallDir

Write-Host "Updating API image..."
docker compose pull
docker compose up -d

$rel = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ "User-Agent" = "ofd-marking-update" }
$asset = $rel.assets | Where-Object { $_.name -eq "ofd-marking-client-windows.zip" } | Select-Object -First 1
if ($asset) {
  $zip = Join-Path $InstallDir "ofd-marking-client-windows.zip"
  Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip
  $clientDir = Join-Path $InstallDir "client"
  Remove-Item -Recurse -Force $clientDir -ErrorAction SilentlyContinue
  Expand-Archive -Path $zip -DestinationPath $clientDir -Force
  Write-Host "Client updated: $clientDir\run-client.bat (release $($rel.tag_name))"
} else {
  Write-Warning "No client zip in latest release."
}

try {
  $port = 18443
  if (Test-Path ".env") {
    $line = Get-Content ".env" | Where-Object { $_ -match "^OFD_HOST_PORT=(.+)$" } | Select-Object -First 1
    if ($line -match "^OFD_HOST_PORT=(.+)$") { $port = [int]$Matches[1] }
  }
  $r = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/health" -TimeoutSec 5
  Write-Host "Health: $($r.status) mode=$($r.sync.mode)"
} catch {
  Write-Warning "Health check failed: $_"
}

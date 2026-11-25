<#
.SYNOPSIS
  Download latest desktop zip from GitHub Releases and unpack (no Docker).
#>
param(
  [string]$Repo = "Viacheslav666/Check_OFD",
  [string]$InstallDir = "$env:USERPROFILE\OFD-Marking"
)

$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null

$rel = Invoke-RestMethod -Uri "https://api.github.com/repos/$Repo/releases/latest" -Headers @{ "User-Agent" = "ofd-desktop-install" }
$asset = $rel.assets | Where-Object { $_.name -eq "ofd-marking-desktop-windows.zip" } | Select-Object -First 1
if (-not $asset) {
  throw "ofd-marking-desktop-windows.zip not in latest release ($($rel.tag_name)). Wait for CD or build with package-desktop.ps1."
}

$zip = Join-Path $env:TEMP "ofd-marking-desktop-windows.zip"
Write-Host "Downloading $($rel.tag_name)..."
Invoke-WebRequest -Uri $asset.browser_download_url -OutFile $zip

Get-ChildItem $InstallDir -Force | Where-Object { $_.Name -ne 'central-env.bat' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Expand-Archive -Path $zip -DestinationPath $InstallDir -Force

Write-Host ""
Write-Host "Installed: $InstallDir"
Write-Host "  1) Агентов на RDP клиентов добавьте во вкладке «Агенты» (ключ ОФД хранится на агентах)"
Write-Host "  2) Run: $InstallDir\Start-OFD.bat"
Write-Host "Need JDK 21+ on PATH. Data: $env:LOCALAPPDATA\OFD-Marking\data"

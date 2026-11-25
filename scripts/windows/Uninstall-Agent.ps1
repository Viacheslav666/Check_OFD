# Остановка и удаление службы агента ОФД (данные и agent.yml остаются в папке).
param([string]$InstallDir = "C:\OFD-Agent", [int]$Port = 18500)
$exe = Join-Path $InstallDir "ofd-agent.exe"
if (Test-Path $exe) { & $exe stop; & $exe uninstall }
Get-NetFirewallRule -DisplayName "OFD Agent $Port" -ErrorAction SilentlyContinue | Remove-NetFirewallRule
Write-Host "Служба ofd-agent удалена. Папка $InstallDir сохранена."

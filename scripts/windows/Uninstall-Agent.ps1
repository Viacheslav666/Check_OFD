<#
.SYNOPSIS
  Удаление службы агента ОФД и правила брандмауэра.
.DESCRIPTION
  По умолчанию папка агента (agent.yml с ключом ОФД и локальная база data) сохраняется.
  -RemoveData удаляет папку полностью (включая ключ API ОФД и токен).
#>
[CmdletBinding()]
param([string]$InstallDir = "C:\OFD-Agent", [switch]$RemoveData)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here "AgentCommon.ps1")
Assert-Admin

$exe = Join-Path $InstallDir "ofd-agent.exe"
Stop-AgentService $exe
Get-NetFirewallRule -Group "OFD Agent" -ErrorAction SilentlyContinue | Remove-NetFirewallRule
Write-Host "Служба ofd-agent и правила брандмауэра «OFD Agent» удалены."

if ($RemoveData) {
  Set-Location $env:TEMP
  Remove-Item -Recurse -Force $InstallDir
  Write-Host "Папка $InstallDir удалена (вместе с ключом ОФД и локальной базой)."
} else {
  Write-Host "Папка $InstallDir сохранена (agent.yml, data). Полное удаление: -RemoveData"
}

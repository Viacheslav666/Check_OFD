<#
.SYNOPSIS
  Смена белого списка IP агента ОФД (например, если у центрального приложения сменился адрес).

.DESCRIPTION
  Меняет agent.allowed-ips в agent.yml и правило брандмауэра «OFD Agent <порт>».
  Переустановка и перезапуск не нужны: агент перечитывает список из agent.yml в течение ~10 секунд
  (в логе data\ofd-agent.log появится «Белый список IP обновлён»). Ключ API ОФД и токен не меняются.

.EXAMPLE
  .\Set-AgentAllowedIps.ps1 -AllowedIps 192.168.31.196
.EXAMPLE
  .\Set-AgentAllowedIps.ps1 -AllowedIps 192.168.31.196,192.168.31.200     # на время переезда центра
.EXAMPLE
  .\Set-AgentAllowedIps.ps1 -Show
#>
[CmdletBinding()]
param(
  [string[]]$AllowedIps,
  [string]$InstallDir = "C:\OFD-Agent",
  [switch]$Show,
  [switch]$Restart
)
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here "AgentCommon.ps1")

$cfg = Join-Path $InstallDir "agent.yml"
if (-not (Test-Path $cfg)) { throw "Не найден $cfg. Укажите -InstallDir." }
$port = Get-AgentPort $cfg

if ($Show -or -not $AllowedIps) {
  Write-Host "agent.yml: allowed-ips = $((Get-AllowedIpsFromYaml $cfg) -join ', ')"
  $r = Get-NetFirewallRule -DisplayName "OFD Agent $port" -ErrorAction SilentlyContinue
  if ($r) { Write-Host "Брандмауэр (порт $port): $(($r | Get-NetFirewallAddressFilter).RemoteAddress -join ', ')" }
  else { Write-Host "Правило брандмауэра «OFD Agent $port» не найдено." }
  return
}

Assert-Admin
$ips = Split-IpList $AllowedIps
if ($ips.Count -eq 0) { throw "Список пуст." }
foreach ($ip in $ips) { Assert-IpOrCidr $ip }

$old = Get-AllowedIpsFromYaml $cfg
Set-AllowedIpsInYaml $cfg $ips
Set-AgentFirewall $port $ips
Write-Host "Белый список: $($old -join ', ') -> $($ips -join ', ')"

if ($Restart) {
  Restart-Service -Name "ofd-agent" -Force
  Write-Host "Служба ofd-agent перезапущена."
} else {
  Write-Host "Агент применит список без перезапуска в течение ~10 секунд (см. $InstallDir\data\ofd-agent.log)."
}

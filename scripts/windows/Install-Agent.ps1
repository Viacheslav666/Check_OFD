<#
.SYNOPSIS
  Установка агента ОФД на RDP клиента как службы Windows (WinSW).

.DESCRIPTION
  - Требуется Java 21+ (Eclipse Temurin JRE) в PATH.
  - Копирует ofd-agent.jar, создаёт agent.yml (если его ещё нет), скачивает WinSW,
    регистрирует и запускает службу «ofd-agent».
  - Открывает входящий порт агента в брандмауэре ТОЛЬКО для IP центрального приложения.
  - Ключ API ОФД остаётся в agent.yml на этой машине и никуда не передаётся.

.EXAMPLE
  .\Install-Agent.ps1 -CentralIp 10.10.50.2 -ApiKey "<ключ из ЛК ОФД>" -Inn 635001808204 -Name "РДП Самара"
#>
param(
  [Parameter(Mandatory = $true)][string]$CentralIp,
  [string]$ApiKey = "",
  [string]$Inn = "635001808204",
  [string]$Name = $env:COMPUTERNAME,
  [int]$Port = 18500,
  [ValidateSet("live", "mock")][string]$Mode = "live",
  [string]$InstallDir = "C:\OFD-Agent",
  [string]$WinSwUrl = "https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe"
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole(
    [Security.Principal.WindowsBuiltInRole]::Administrator)) {
  throw "Запустите PowerShell от имени администратора."
}
java -version 2>$null
if ($LASTEXITCODE -ne 0) { throw "Нужна Java 21+ (Eclipse Temurin JRE) в PATH." }
if ($Mode -eq "live" -and [string]::IsNullOrWhiteSpace($ApiKey)) {
  throw "Для режима live укажите -ApiKey (ключ API из ЛК ОФД: Настройки → Управление передачей данных)."
}

New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $InstallDir "data") | Out-Null

$jar = Get-ChildItem -Path $here -Filter "ofd-agent*.jar" | Select-Object -First 1
if (-not $jar) { throw "ofd-agent.jar не найден рядом со скриптом." }
Copy-Item $jar.FullName (Join-Path $InstallDir "ofd-agent.jar") -Force
Copy-Item (Join-Path $here "ofd-agent.xml") (Join-Path $InstallDir "ofd-agent.xml") -Force

# Токен: 32 случайных байта в hex
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
$token = ($bytes | ForEach-Object { $_.ToString("x2") }) -join ""

$cfg = Join-Path $InstallDir "agent.yml"
if (Test-Path $cfg) {
  Write-Host "agent.yml уже существует — не перезаписываю (токен и ключ остаются прежними)."
  $token = $null
} else {
@"
server:
  port: $Port
agent:
  name: "$Name"
  token: "$token"
  allowed-ips: "$CentralIp"
  data-dir: "./data"
ofd:
  mode: $Mode
  api-key: "$ApiKey"
  inn: "$Inn"
  sync-on-startup: true
  scheduler:
    enabled: true
    cron: "0 */15 * * * *"
"@ | Set-Content -Encoding UTF8 $cfg
}

# Права на папку: только Администраторы и SYSTEM (в agent.yml секреты)
icacls $InstallDir /inheritance:r /grant:r "Administrators:(OI)(CI)F" "SYSTEM:(OI)(CI)F" | Out-Null

$exe = Join-Path $InstallDir "ofd-agent.exe"
if (-not (Test-Path $exe)) {
  $local = Join-Path $here "WinSW-x64.exe"
  if (Test-Path $local) { Copy-Item $local $exe } else { Invoke-WebRequest -Uri $WinSwUrl -OutFile $exe }
}

& $exe stop 2>$null | Out-Null
& $exe uninstall 2>$null | Out-Null
& $exe install
& $exe start

$ruleName = "OFD Agent $Port"
Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue | Remove-NetFirewallRule
New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort $Port `
  -RemoteAddress $CentralIp -Action Allow | Out-Null

Start-Sleep -Seconds 8
try {
  $h = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/agent/v1/health" -TimeoutSec 5
  Write-Host "Агент запущен: $($h.status), версия $($h.version)"
} catch {
  Write-Warning "Агент ещё не ответил. Лог: $InstallDir\data\ofd-agent.log и $InstallDir\ofd-agent.out.log"
}

$ip = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.PrefixOrigin -ne "WellKnown" } |
  Select-Object -First 1).IPAddress
Write-Host ""
Write-Host "=== Внесите в ЛК центрального приложения (вкладка «Агенты») ==="
Write-Host "Имя:   $Name"
Write-Host "IP:    $ip  (или адрес этой машины в VPN/NetBird)"
Write-Host "Порт:  $Port"
if ($token) { Write-Host "Токен: $token" } else { Write-Host "Токен: см. agent.token в $cfg" }
Write-Host "Файл конфигурации: $cfg"

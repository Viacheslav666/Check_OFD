<#
.SYNOPSIS
  Установка агента ОФД на RDP клиента как службы Windows (без Docker).

.DESCRIPTION
  Запускается из распакованного архива ofd-agent-windows.zip (PowerShell от имени администратора).
  - Java ставить не нужно: в архиве есть папка jre (Java 21, Eclipse Temurin / jlink).
  - Копирует агента в C:\OFD-Agent, создаёт agent.yml (ключ API ОФД, ИНН, токен, белый список IP),
    регистрирует и запускает службу Windows «ofd-agent» (WinSW, автозапуск, перезапуск при сбое).
  - Открывает входящий TCP-порт агента (18500) в брандмауэре ТОЛЬКО для адресов из белого списка.
  - Ключ API ОФД остаётся в agent.yml на этой машине и никуда не передаётся.
  Проверено для Windows Server 2019 Standard x64 (Windows PowerShell 5.1).

.EXAMPLE
  .\Install-Agent.ps1 -Inn 635001808204 -Name "РДП Самара"
  (ключ API ОФД будет запрошен скрытым вводом; белый список по умолчанию — 192.168.31.196)

.EXAMPLE
  .\Install-Agent.ps1 -Inn 635001808204 -Name "РДП Самара" -AllowedIps 192.168.31.196,10.10.50.2 -Port 18500
#>
[CmdletBinding()]
param(
  [string]$Inn = "",
  [string]$Name = $env:COMPUTERNAME,
  [Alias("CentralIp")][string[]]$AllowedIps = @("192.168.31.196"),
  [int]$Port = 18500,
  [string]$ApiKey = "",
  [string]$Token = "",
  [ValidateSet("live", "mock")][string]$Mode = "live",
  [string]$InstallDir = "C:\OFD-Agent",
  [string]$JavaHome = "",
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here "AgentCommon.ps1")

Assert-Admin
Set-Tls12

# --- проверки параметров ---
$AllowedIps = Split-IpList $AllowedIps
foreach ($ip in $AllowedIps) { Assert-IpOrCidr $ip }
if ($AllowedIps.Count -eq 0) { throw "Белый список пуст: укажите -AllowedIps (IP центрального приложения)." }

$cfg = Join-Path $InstallDir "agent.yml"
$keepCfg = (Test-Path $cfg) -and -not $Force
if (-not $keepCfg) {
  if ($Mode -eq "live") {
    if ([string]::IsNullOrWhiteSpace($Inn)) { $Inn = Read-Host "ИНН организации (10 или 12 цифр; несколько — через запятую)" }
    foreach ($i in ($Inn -split "[,;\s]+" | Where-Object { $_ })) {
      if ($i -notmatch '^\d{10}(\d{2})?$') { throw "Неверный ИНН: '$i' (нужно 10 или 12 цифр)." }
    }
    if ([string]::IsNullOrWhiteSpace($ApiKey)) {
      $sec = Read-Host "Ключ API ОФД (ЛК ОФД: Настройки → Управление передачей данных)" -AsSecureString
      $ApiKey = [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
    }
    if ([string]::IsNullOrWhiteSpace($ApiKey)) { throw "Для режима live нужен ключ API ОФД." }
  }
  if ([string]::IsNullOrWhiteSpace($Token)) { $Token = New-AgentToken }
  if ($Token.Length -lt 16) { throw "Токен должен быть не короче 16 символов." }
}

# --- файлы ---
$jar = Get-ChildItem -Path $here -Filter "ofd-agent*.jar" | Select-Object -First 1
if (-not $jar) { throw "ofd-agent.jar не найден рядом со скриптом." }
$winsw = Join-Path $here "WinSW-x64.exe"
if (-not (Test-Path $winsw)) { throw "WinSW-x64.exe не найден рядом со скриптом (он входит в архив ofd-agent-windows.zip)." }
$bundledJre = Join-Path $here "jre"
if (-not $JavaHome -and -not (Test-Path (Join-Path $bundledJre "bin\java.exe"))) {
  throw "В архиве нет папки jre. Скачайте полный архив ofd-agent-windows.zip или укажите -JavaHome <папка Java 21>."
}

New-Item -ItemType Directory -Force -Path $InstallDir, (Join-Path $InstallDir "data") | Out-Null
$exe = Join-Path $InstallDir "ofd-agent.exe"
Stop-AgentService $exe

Copy-Item $jar.FullName (Join-Path $InstallDir "ofd-agent.jar") -Force
Copy-Item $winsw $exe -Force
if (-not $JavaHome) {
  $dstJre = Join-Path $InstallDir "jre"
  if (Test-Path $dstJre) { Remove-Item -Recurse -Force $dstJre }
  Copy-Item $bundledJre $dstJre -Recurse -Force
}
foreach ($s in "AgentCommon.ps1", "Set-AgentAllowedIps.ps1", "Update-Agent.ps1", "Uninstall-Agent.ps1") {
  if (Test-Path (Join-Path $here $s)) { Copy-Item (Join-Path $here $s) $InstallDir -Force }
}
$xml = Get-Content -Raw -Encoding UTF8 (Join-Path $here "ofd-agent.xml")
if ($JavaHome) { $xml = $xml.Replace('%BASE%\jre\bin\java.exe', (Join-Path $JavaHome "bin\java.exe")) }
Write-Utf8NoBom (Join-Path $InstallDir "ofd-agent.xml") $xml

# --- agent.yml ---
if ($keepCfg) {
  Write-Host "agent.yml уже есть — не перезаписываю (ключ ОФД и токен прежние). Для перезаписи: -Force."
  Write-Host "Белый список обновляю на: $($AllowedIps -join ', ')"
  Set-AllowedIpsInYaml $cfg $AllowedIps
  $Port = Get-AgentPort $cfg $Port
  $Token = $null
} else {
  $yml = @"
# Конфигурация агента ОФД. Файл содержит секреты: доступ только у Администраторов и SYSTEM.
server:
  port: $Port
agent:
  name: $(Quote-Yaml $Name)
  # Общий секрет с центральным приложением (тот же — в карточке агента в ЛК центра)
  token: $(Quote-Yaml $Token)
  # IP центрального приложения (через запятую, можно CIDR). Изменения применяются без перезапуска
  # (~10 с). Чтобы заодно обновить брандмауэр: .\Set-AgentAllowedIps.ps1 -AllowedIps <IP>
  allowed-ips: $(Quote-Yaml ($AllowedIps -join ","))
  allowlist-reload-seconds: 10
  data-dir: "./data"
  retention-days: 120
ofd:
  mode: $Mode
  # Ключ API ОФД этого клиента. Хранится только здесь.
  api-key: $(Quote-Yaml $ApiKey)
  inn: $(Quote-Yaml $Inn)
  sync-on-startup: true
  scheduler:
    enabled: true
    cron: "0 */15 * * * *"
"@
  Write-Utf8NoBom $cfg $yml
}
$ApiKey = $null

# Права на папку: только Администраторы и SYSTEM (в agent.yml секреты)
& icacls $InstallDir /inheritance:r /grant:r "*S-1-5-32-544:(OI)(CI)F" "*S-1-5-18:(OI)(CI)F" | Out-Null

# --- служба ---
& $exe install | Out-Host
& $exe start | Out-Host

# --- брандмауэр ---
Set-AgentFirewall $Port $AllowedIps

$ok = Wait-AgentListening $Port 90
if ($ok) { Write-Host "Служба ofd-agent запущена, порт $Port слушается." -ForegroundColor Green }
else { Write-Warning "Агент пока не слушает порт $Port. Логи: $InstallDir\data\ofd-agent.log и $InstallDir\ofd-agent.out.log" }

$ips = (Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" }).IPAddress -join ", "
Write-Host ""
Write-Host "=== Внесите в ЛК центрального приложения (вкладка «Агенты») ==="
Write-Host "Имя:   $Name"
Write-Host "IP:    $ips"
Write-Host "Порт:  $Port"
if ($Token) { Write-Host "Токен: $Token" } else { Write-Host "Токен: прежний (agent.token в $cfg)" }
Write-Host "Белый список IP: $($AllowedIps -join ', ')"
Write-Host "Сменить IP центра: $InstallDir\Set-AgentAllowedIps.ps1 -AllowedIps <IP>"

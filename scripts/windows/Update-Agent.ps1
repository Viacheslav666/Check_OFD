<#
.SYNOPSIS
  Обновление агента ОФД до новой версии без переустановки.

.DESCRIPTION
  Запускается из распакованного НОВОГО архива ofd-agent-windows.zip (от имени администратора).
  Останавливает службу, сохраняет старые ofd-agent.jar и jre в backup\<дата>, копирует новые,
  запускает службу. agent.yml (ключ ОФД, токен, белый список) и data (локальная база чеков) не меняются.
  Если новая версия не запустилась за 90 секунд — автоматически возвращает предыдущую.
#>
[CmdletBinding()]
param([string]$InstallDir = "C:\OFD-Agent")
$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here "AgentCommon.ps1")
Assert-Admin

$cfg = Join-Path $InstallDir "agent.yml"
$exe = Join-Path $InstallDir "ofd-agent.exe"
if (-not (Test-Path $cfg) -or -not (Test-Path $exe)) { throw "Агент не установлен в $InstallDir. Используйте Install-Agent.ps1." }
$jar = Get-ChildItem -Path $here -Filter "ofd-agent*.jar" | Select-Object -First 1
if (-not $jar) { throw "ofd-agent.jar не найден рядом со скриптом." }
if ((Resolve-Path $here).Path -eq (Resolve-Path $InstallDir).Path) { throw "Запустите скрипт из папки нового архива, а не из $InstallDir." }
$port = Get-AgentPort $cfg

$backup = Join-Path $InstallDir ("backup\" + (Get-Date -Format "yyyyMMdd-HHmmss"))
New-Item -ItemType Directory -Force -Path $backup | Out-Null

Write-Host "Останавливаю службу ofd-agent..."
& $exe stop | Out-Host
(Get-Service ofd-agent).WaitForStatus("Stopped", [TimeSpan]::FromSeconds(60))

foreach ($item in "ofd-agent.jar", "jre", "ofd-agent.xml", "ofd-agent.exe") {
  $p = Join-Path $InstallDir $item
  if (Test-Path $p) { Copy-Item $p $backup -Recurse -Force }
}

Copy-Item $jar.FullName (Join-Path $InstallDir "ofd-agent.jar") -Force
if (Test-Path (Join-Path $here "jre\bin\java.exe")) {
  $dstJre = Join-Path $InstallDir "jre"
  if (Test-Path $dstJre) { Remove-Item -Recurse -Force $dstJre }
  Copy-Item (Join-Path $here "jre") $dstJre -Recurse -Force
}
foreach ($s in "AgentCommon.ps1", "Set-AgentAllowedIps.ps1", "Update-Agent.ps1", "Uninstall-Agent.ps1") {
  if (Test-Path (Join-Path $here $s)) { Copy-Item (Join-Path $here $s) $InstallDir -Force }
}

& $exe start | Out-Host
if (Wait-AgentListening $port 90) {
  Write-Host "Агент обновлён и запущен (порт $port). Резервная копия: $backup" -ForegroundColor Green
} else {
  Write-Warning "Новая версия не запустилась — возвращаю предыдущую из $backup"
  & $exe stop | Out-Host
  Start-Sleep -Seconds 3
  Copy-Item (Join-Path $backup "ofd-agent.jar") $InstallDir -Force
  if (Test-Path (Join-Path $backup "jre")) {
    Remove-Item -Recurse -Force (Join-Path $InstallDir "jre")
    Copy-Item (Join-Path $backup "jre") (Join-Path $InstallDir "jre") -Recurse -Force
  }
  & $exe start | Out-Host
  throw "Обновление не удалось, восстановлена прежняя версия. Смотрите $InstallDir\data\ofd-agent.log"
}

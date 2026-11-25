# Собирает dist\ofd-agent-windows.zip: ofd-agent.jar + WinSW-конфиг + установщик + пример agent.yml
$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root
mvn -B -pl agent -am -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
$out = Join-Path $Root "dist\agent"
Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $out | Out-Null
$jar = Get-ChildItem "agent\target\ofd-agent-*-exec.jar" | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $out "ofd-agent.jar")
Copy-Item "agent\src\main\winsw\ofd-agent.xml" $out
Copy-Item "deploy\agent\agent.yml.example" $out
Copy-Item (Join-Path $PSScriptRoot "Install-Agent.ps1") $out
Copy-Item (Join-Path $PSScriptRoot "Uninstall-Agent.ps1") $out
@"
Агент ОФД (Windows, RDP клиента)
================================
1. Установите Java 21 (Eclipse Temurin JRE), если её нет.
2. PowerShell от администратора, в этой папке:
   .\Install-Agent.ps1 -CentralIp <IP центра> -ApiKey "<ключ из ЛК ОФД>" -Inn <ИНН> -Name "<имя>"
3. Скрипт выведет IP, порт и токен — внесите их в ЛК центрального приложения (вкладка «Агенты»).
Подробно: README.md репозитория, раздел «Установка агента на RDP».
"@ | Set-Content -Encoding UTF8 (Join-Path $out "README.txt")
$zip = Join-Path $Root "dist\ofd-agent-windows.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $out "*") -DestinationPath $zip
Write-Host "Built $zip"

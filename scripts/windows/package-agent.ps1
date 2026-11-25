<#
.SYNOPSIS
  Сборка релизного архива dist\ofd-agent-windows.zip для установки агента на RDP (Windows, без Docker).

.DESCRIPTION
  Состав архива: ofd-agent.jar, jre\ (Java 21: по умолчанию урезанный runtime через jlink из текущего JDK 21,
  либо -Runtime temurin — готовый Eclipse Temurin JRE 21 для Windows x64), WinSW-x64.exe, ofd-agent.xml,
  Install-Agent.ps1, Update-Agent.ps1, Set-AgentAllowedIps.ps1, Uninstall-Agent.ps1, AgentCommon.ps1,
  agent.yml.example, README.txt.
  Запускать на Windows x64 с JDK 21 (jlink собирает runtime под ОС, на которой запущен).
#>
param([ValidateSet("jlink", "temurin")][string]$Runtime = "jlink", [switch]$SkipBuild)
$ErrorActionPreference = "Stop"
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $Root

$WinSwUrl = "https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe"
$WinSwSha256 = "05b82d46ad331cc16bdc00de5c6332c1ef818df8ceefcd49c726553209b3a0da"
# Модули Java, нужные агенту (Spring Boot, Tomcat, H2, HttpClient/TLS, логирование)
$Modules = "java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,java.naming," +
  "java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql," +
  "java.transaction.xa,java.xml,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.charsets,jdk.localedata,jdk.management," +
  "jdk.naming.dns,jdk.net,jdk.unsupported,jdk.zipfs"

if (-not $SkipBuild) {
  mvn -B -pl agent -am -DskipTests package
  if ($LASTEXITCODE -ne 0) { throw "Maven package failed" }
}
$out = Join-Path $Root "dist\agent"
Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $out | Out-Null

$jar = Get-ChildItem "agent\target\ofd-agent-*-exec.jar" | Select-Object -First 1
Copy-Item $jar.FullName (Join-Path $out "ofd-agent.jar")
Copy-Item "agent\src\main\winsw\ofd-agent.xml" $out
Copy-Item "deploy\agent\agent.yml.example" $out
foreach ($s in "Install-Agent.ps1", "Update-Agent.ps1", "Set-AgentAllowedIps.ps1", "Uninstall-Agent.ps1", "AgentCommon.ps1") {
  Copy-Item (Join-Path $PSScriptRoot $s) $out
}
Copy-Item "deploy\agent\README.txt" $out

# Java runtime
$jre = Join-Path $out "jre"
if ($Runtime -eq "jlink") {
  $jlink = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\jlink.exe" } else { "jlink" }
  & $jlink --add-modules $Modules --include-locales=en,ru --strip-debug --no-man-pages --no-header-files `
    --compress=zip-6 --output $jre
  if ($LASTEXITCODE -ne 0) { throw "jlink failed" }
} else {
  $api = "https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse"
  $pkg = (Invoke-RestMethod $api)[0].binary.package
  $zipJre = Join-Path $env:TEMP $pkg.name
  Invoke-WebRequest $pkg.link -OutFile $zipJre
  if ((Get-FileHash $zipJre -Algorithm SHA256).Hash.ToLower() -ne $pkg.checksum.ToLower()) { throw "SHA256 Temurin JRE не совпал" }
  $tmp = Join-Path $env:TEMP "temurin-jre"
  Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
  Expand-Archive $zipJre $tmp
  Move-Item (Get-ChildItem $tmp -Directory | Select-Object -First 1).FullName $jre
}
& (Join-Path $jre "bin\java.exe") -version

# WinSW
$winsw = Join-Path $out "WinSW-x64.exe"
Invoke-WebRequest $WinSwUrl -OutFile $winsw
if ((Get-FileHash $winsw -Algorithm SHA256).Hash.ToLower() -ne $WinSwSha256) { throw "SHA256 WinSW не совпал" }

$zip = Join-Path $Root "dist\ofd-agent-windows.zip"
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path (Join-Path $out "*") -DestinationPath $zip
Write-Host "Built $zip"

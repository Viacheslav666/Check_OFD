# Общие функции скриптов агента ОФД (подключается через dot-source).

$script:FirewallGroup = "OFD Agent"

function Assert-Admin {
  $p = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
  if (-not $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Запустите PowerShell от имени администратора."
  }
}

function Set-Tls12 {
  [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
}

function Split-IpList([string[]]$list) {
  return @($list | ForEach-Object { $_ -split "[,;\s]+" } | Where-Object { $_ } | Select-Object -Unique)
}

function Assert-IpOrCidr([string]$entry) {
  $addr = $entry; $prefix = $null
  if ($entry.Contains("/")) { $addr, $prefix = $entry.Split("/", 2) }
  $ip = $null
  if (-not [System.Net.IPAddress]::TryParse($addr, [ref]$ip)) { throw "Неверный IP-адрес в белом списке: '$entry'" }
  if ($ip.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork -and ($addr -split '\.').Count -ne 4) {
    throw "Неверный IPv4-адрес в белом списке: '$entry'"
  }
  if ($null -ne $prefix) {
    $max = if ($ip.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork) { 32 } else { 128 }
    $n = 0
    if (-not [int]::TryParse($prefix, [ref]$n) -or $n -lt 0 -or $n -gt $max) { throw "Неверная маска в белом списке: '$entry'" }
  }
}

function New-AgentToken {
  $bytes = New-Object byte[] 32
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  return ($bytes | ForEach-Object { $_.ToString("x2") }) -join ""
}

function Quote-Yaml([string]$s) {
  if ($null -eq $s) { $s = "" }
  return "'" + $s.Replace("'", "''") + "'"
}

function Write-Utf8NoBom([string]$path, [string]$text) {
  [IO.File]::WriteAllText($path, $text, (New-Object Text.UTF8Encoding $false))
}

function Get-AgentPort([string]$cfg, [int]$default = 18500) {
  $t = [IO.File]::ReadAllText($cfg)
  $m = [regex]::Match($t, '(?ms)^server:\s*\r?\n(?:[ \t]+.*\r?\n)*?[ \t]+port:\s*(\d+)')
  if ($m.Success) { return [int]$m.Groups[1].Value }
  return $default
}

function Get-AllowedIpsFromYaml([string]$cfg) {
  $t = [IO.File]::ReadAllText($cfg)
  $m = [regex]::Match($t, '(?m)^[ \t]+allowed-ips:[ \t]*(.*?)[ \t]*\r?$')
  if (-not $m.Success) { return @() }
  return Split-IpList @($m.Groups[1].Value.Trim("'", '"'))
}

# Меняет строку agent.allowed-ips в agent.yml (остальное содержимое файла не трогает).
function Set-AllowedIpsInYaml([string]$cfg, [string[]]$ips) {
  $t = [IO.File]::ReadAllText($cfg)
  if ($t.Length -gt 0 -and $t[0] -eq [char]0xFEFF) { $t = $t.Substring(1) }
  $value = Quote-Yaml ($ips -join ",")
  $re = [regex]'(?m)^([ \t]+allowed-ips:)[ \t]*.*?(\r?)$'
  if ($re.IsMatch($t)) {
    $t = $re.Replace($t, { param($m) $m.Groups[1].Value + " " + $value + $m.Groups[2].Value }, 1)
  } elseif ($t -match '(?m)^agent:[ \t]*\r?$') {
    $t = [regex]::Replace($t, '(?m)^agent:[ \t]*(\r?)$', { param($m) "agent:" + $m.Groups[1].Value + "`n  allowed-ips: " + $value + $m.Groups[1].Value }, 1)
  } else {
    $t = $t.TrimEnd() + "`nagent:`n  allowed-ips: " + $value + "`n"
  }
  Write-Utf8NoBom $cfg $t
}

function Set-AgentFirewall([int]$port, [string[]]$ips) {
  $name = "OFD Agent $port"
  Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue | Remove-NetFirewallRule
  New-NetFirewallRule -DisplayName $name -Group $script:FirewallGroup -Direction Inbound -Protocol TCP `
    -LocalPort $port -RemoteAddress $ips -Action Allow -Profile Any | Out-Null
  Write-Host "Брандмауэр: порт $port открыт только для $($ips -join ', ')"
}

function Stop-AgentService([string]$exe) {
  $svc = Get-Service -Name "ofd-agent" -ErrorAction SilentlyContinue
  if ($svc) {
    if ($svc.Status -ne "Stopped") { Stop-Service -Name "ofd-agent" -Force -ErrorAction SilentlyContinue }
    $svc.WaitForStatus("Stopped", [TimeSpan]::FromSeconds(60))
    if (Test-Path $exe) { & $exe uninstall | Out-Host } else { & sc.exe delete ofd-agent | Out-Null }
    Start-Sleep -Seconds 2
  }
}

function Wait-AgentListening([int]$port, [int]$seconds = 60) {
  $deadline = (Get-Date).AddSeconds($seconds)
  while ((Get-Date) -lt $deadline) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { return $true }
    Start-Sleep -Seconds 3
  }
  return $false
}

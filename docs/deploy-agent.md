# Развёртывание агента на RDP клиента

## Требования

| | |
|---|---|
| ОС | Windows Server 2019 Standard x64 (основная; подойдут 2016/2022, Windows 10/11 x64), права администратора |
| ПО | ничего: Java 21 (Temurin/jlink) и WinSW входят в `ofd-agent-windows.zip`; Docker не нужен |
| Ресурсы | ~150–300 МБ RAM, < 0,5 ядра при сборе; диск: ~250 МБ программа + 100–400 МБ база за 120 дней |
| Сеть | постоянный IP RDP; исходящий HTTPS к `ofd.ru`; входящий TCP 18500 **только с IP центра** (сейчас `192.168.31.196`) |
| Ключ ОФД | свой у каждого клиента (один ключ = один агент), хранится только в `agent.yml` |

## Windows (служба, без Docker)

```powershell
# PowerShell от администратора, в распакованной папке ofd-agent-windows.zip
Set-ExecutionPolicy -Scope Process Bypass -Force
.\Install-Agent.ps1 -Inn 635001808204 -Name "РДП Самара"            # ключ ОФД спросит скрытым вводом
# необязательно: -AllowedIps 192.168.31.196,10.10.50.2  -Port 18500  -ApiKey "..."  -Token "..."  -Force
```

Состав архива: `ofd-agent.jar`, `jre\` (Java 21), `WinSW-x64.exe`, `ofd-agent.xml`, `Install-Agent.ps1`,
`Update-Agent.ps1`, `Set-AgentAllowedIps.ps1`, `Uninstall-Agent.ps1`, `AgentCommon.ps1`, `agent.yml.example`, `README.txt`.

Что делает установщик:
1. Проверяет права администратора, ИНН (10/12 цифр), адреса белого списка (IPv4/IPv6/CIDR).
2. Копирует агента, Java и служебные скрипты в `C:\OFD-Agent`.
3. Пишет `agent.yml` (UTF-8): ключ ОФД, ИНН, сгенерированный токен (64 hex), `allowed-ips`. Если `agent.yml`
   уже есть — не перезаписывает его (только белый список), для перезаписи — `-Force`.
4. Оставляет права на папку только Администраторам и SYSTEM (`icacls`, SID — работает на русской Windows).
5. Регистрирует и запускает службу `ofd-agent` (WinSW: автозапуск с задержкой, перезапуск при сбое).
6. Создаёт правило брандмауэра «OFD Agent 18500» (группа «OFD Agent»): вход TCP 18500 только с адресов белого списка.
7. Ждёт, пока агент начнёт слушать порт, и выводит имя, IP, порт и токен для ЛК центра.

Файлы: `C:\OFD-Agent\agent.yml` (секреты), `C:\OFD-Agent\data\agent.mv.db` (H2), `C:\OFD-Agent\data\ofd-agent.log`.
Служба: `Get-Service ofd-agent`, `Restart-Service ofd-agent` (или `C:\OFD-Agent\ofd-agent.exe status|restart`).

## Смена IP центра (белый список) — без переустановки

```powershell
C:\OFD-Agent\Set-AgentAllowedIps.ps1 -AllowedIps 192.168.31.200
C:\OFD-Agent\Set-AgentAllowedIps.ps1 -Show
```

- Меняет `agent.allowed-ips` в `agent.yml` и правило брандмауэра; служба не перезапускается.
- Агент проверяет дату изменения `agent.yml` раз в `agent.allowlist-reload-seconds` (10 с) и применяет новый список;
  в логе: «Белый список IP обновлён из agent.yml: [...] -> [...]».
- Пустой, ошибочный (`192.168.31.999`, имя хоста) или невалидный YAML не применяются — действует прежний список, в логе ошибка.
- При ручной правке `agent.yml` не забудьте правило брандмауэра (или запустите скрипт — он сделает оба шага).
- Ключ ОФД (раз в год), токен и порт: правка `agent.yml` → `Restart-Service ofd-agent`.

## Обновление / удаление

- `.\Update-Agent.ps1` из папки нового архива: резервная копия в `C:\OFD-Agent\backup\<дата>`, замена `ofd-agent.jar` и `jre`,
  запуск; `agent.yml` и `data` сохраняются; если новая версия не поднялась за 90 с — откат.
- `C:\OFD-Agent\Uninstall-Agent.ps1` — служба и правила брандмауэра; `-RemoveData` — удалить и папку (ключ, база).

## Сборка архива

- Windows + JDK 21: `scripts\windows\package-agent.ps1` (runtime через `jlink`, ~60 МБ; `-Runtime temurin` — готовый JRE).
- Linux: `scripts/package-agent-windows.sh [каталог]` — скачивает Temurin JRE 21 для Windows x64 и WinSW с проверкой SHA256.
- GitHub Actions `release.yml` собирает `ofd-agent-windows.zip` и прикладывает к релизу.

## Linux / Docker (не основной вариант)

```bash
docker build -f Dockerfile.agent -t ofd-agent .
docker run -d --name ofd-agent --restart unless-stopped -p 18500:18500 -v ofd-agent-data:/data \
  -e AGENT_NAME="РДП Самара" -e AGENT_TOKEN="<32+ символа>" -e AGENT_ALLOWED_IPS="192.168.31.196" \
  -e OFD_MODE=live -e OFD_API_KEY="<ключ>" -e OFD_INN=635001808204 ofd-agent
```

При пробросе порта Docker агент видит IP шлюза Docker, а не центра — указывайте подсеть Docker
(например `172.16.0.0/12`) **и** ограничьте порт брандмауэром хоста адресом центра, либо используйте `--network host`.

## Проверка

С машины центра: `curl http://<IP агента>:18500/agent/v1/health` → `{"status":"UP"…}`.
С чужого IP — `403`. Без токена `/agent/v1/status` — `401`.

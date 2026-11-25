# Развёртывание агента на RDP клиента

## Требования

| | |
|---|---|
| ОС | Windows Server 2016+ / Windows 10+ (x64); либо Linux с Docker |
| ПО | Java 21 (Eclipse Temurin JRE) |
| Ресурсы | ~150–250 МБ RAM, < 200 МБ диска на 90–120 дней чеков |
| Сеть | исходящий HTTPS к `ofd.ru`; входящий TCP 18500 **только с IP центра** |

## Windows (служба)

```powershell
# PowerShell от администратора, в распакованной папке ofd-agent-windows.zip
.\Install-Agent.ps1 -CentralIp 10.10.50.2 -ApiKey "<ключ>" -Inn 635001808204 -Name "РДП Самара"
```

Файлы: `C:\OFD-Agent\agent.yml` (секреты), `C:\OFD-Agent\data\agent.mv.db` (H2), `C:\OFD-Agent\data\ofd-agent.log`.
Управление: `C:\OFD-Agent\ofd-agent.exe start|stop|restart|status`. Удаление: `Uninstall-Agent.ps1`.

Смена ключа ОФД (раз в год): отредактируйте `ofd.api-key` в `agent.yml` → `ofd-agent.exe restart`.
Смена IP центра: `agent.allowed-ips` + правило брандмауэра «OFD Agent 18500».

## Linux / Docker

```bash
docker build -f Dockerfile.agent -t ofd-agent .
docker run -d --name ofd-agent --restart unless-stopped -p 18500:18500 -v ofd-agent-data:/data \
  -e AGENT_NAME="РДП Самара" -e AGENT_TOKEN="<32+ символа>" -e AGENT_ALLOWED_IPS="10.10.50.2" \
  -e OFD_MODE=live -e OFD_API_KEY="<ключ>" -e OFD_INN=635001808204 ofd-agent
```

При пробросе порта Docker агент видит IP шлюза Docker, а не центра — указывайте подсеть Docker
(например `172.16.0.0/12`) **и** ограничьте порт брандмауэром хоста адресом центра, либо используйте `--network host`.

## Проверка

С машины центра: `curl http://<IP агента>:18500/agent/v1/health` → `{"status":"UP"…}`.
С чужого IP — `403`. Без токена `/agent/v1/status` — `401`.

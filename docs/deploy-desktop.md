# Desktop без Docker (рекомендуется для RDP)

## Зачем на каждой машине свой пакет

Запросы к ОФД.ру делает **локальный сервер** внутри пакета.  
Клиент JavaFX только UI → `http://127.0.0.1:18443`.

Ставите zip на каждый RDP-хост → исходящий IP в ОФД = IP этой машины.

## Требования на ПК

| | |
|---|---|
| ОС | Windows 10/11 / Server (RDP) |
| ПО | **JDK 21+** (Eclipse Temurin), в PATH |
| Docker | **не нужен** |
| RAM | ~1.5–2 ГБ свободно под стек |
| Диск | ~500 МБ ПО + данные в `%LOCALAPPDATA%\OFD-Marking\data` |

## Установка

### Вариант A — с GitHub Release

```powershell
irm https://raw.githubusercontent.com/Viacheslav666/Check_OFD/main/scripts/windows/Install-Desktop.ps1 -OutFile Install-Desktop.ps1
.\Install-Desktop.ps1
cd $env:USERPROFILE\OFD-Marking
# live:
copy ofd-env.example.bat ofd-env.bat
notepad ofd-env.bat
.\Start-OFD.bat
```

### Вариант B — вручную

1. Скачайте `ofd-marking-desktop-windows.zip` из Releases
2. Распакуйте, запустите `Start-OFD.bat`

## Синхронизация ОФД

Автосинка нет (ни при старте, ни по крону). В клиенте нажмите **«Синхронизация ОФД»** — запросы к ofd.ru уйдут с этой машины.

## Проверка

```powershell
Invoke-RestMethod http://127.0.0.1:18443/api/health
Invoke-RestMethod https://api.ipify.org   # внешний IP этой машины
```

Стоп сервера: `Stop-OFD.bat`.

## CI/CD

Push в `main` → workflow **Release CD** собирает Windows desktop zip и публикует Release `desktop-<sha>`.  
На площадках снова запускаете `Install-Desktop.ps1` или меняете файлы вручную.

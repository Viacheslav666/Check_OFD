# Развёртывание по площадкам (разные IP → ОФД)

> **Актуальный способ без Docker:** [`deploy-desktop.md`](deploy-desktop.md)

Запросы к ОФД.ру делает **сервер**, не JavaFX.  
На каждом RDP-ПК нужен свой локальный сервер → свой исходящий IP.

Рекомендуется zip `ofd-marking-desktop-windows.zip` + `Start-OFD.bat` (встроенная H2, JDK 21).

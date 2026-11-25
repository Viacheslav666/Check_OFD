# Доля маркированных чеков по торговым точкам (ОФД.ру)

Сервис + desktop-клиент JavaFX: сбор чеков из API [ОФД.ру](https://ofd.ru/razrabotchikam/ofd), расчёт доли чеков с маркированными товарами (ФФД 1.2: тег 1163 и/или SubjectType 31/33) по торговым точкам.

**Для RDP / разных IP в ОФД:** ставьте **desktop-пакет на каждую машину** (локальный сервер + H2 + JavaFX). Docker не обязателен. См. [`docs/deploy-desktop.md`](docs/deploy-desktop.md).

## Состав

| Модуль | Назначение |
|---|---|
| `server/` | Spring Boot 21, Flyway, сборщик ОФД, REST (Postgres или встроенная H2) |
| `client/` | JavaFX-клиент (дашборд, рейтинг, группы, матрица, чеки, Excel) |
| `docs/` | ТЗ→requirements, API mapping, деплой |

## Windows desktop (без Docker) — основной вариант

1. Нужен только **JDK 21+** (Temurin) в PATH.
2. Скачайте `ofd-marking-desktop-windows.zip` из [Releases](https://github.com/Viacheslav666/Check_OFD/releases) **или**:

```powershell
irm https://raw.githubusercontent.com/Viacheslav666/Check_OFD/main/scripts/windows/Install-Desktop.ps1 -OutFile Install-Desktop.ps1
.\Install-Desktop.ps1
cd $env:USERPROFILE\OFD-Marking
copy ofd-env.example.bat ofd-env.bat   # для live — пропишите OFD_API_KEY
.\Start-OFD.bat
```

Данные: `%LOCALAPPDATA%\OFD-Marking\data`. Стоп: `Stop-OFD.bat`.

CI/CD: каждый push в `main` публикует новый desktop zip в Releases.

## Docker (опционально, если Docker уже есть)

На машине с IP `10.10.50.2` или `192.168.31.196`:

```bash
git clone https://github.com/Viacheslav666/Check_OFD.git
cd Check_OFD

# mock: PostgreSQL + API в контейнерах
docker compose up -d --build

# проверка
curl -s http://127.0.0.1:18443/api/health
# с другого ПК:
# curl -s http://10.10.50.2:18443/api/health
# curl -s http://192.168.31.196:18443/api/health
```

Клиент JavaFX — на ПК (не в Docker):

```bash
OFD_CLIENT_SERVER_URL=http://10.10.50.2:18443 ./scripts/run-client.sh
# или
OFD_CLIENT_SERVER_URL=http://192.168.31.196:18443 ./scripts/run-client.sh
```

Порт на хосте (если 18443 занят; **8080 не трогайте**, если там монитор):

```bash
OFD_HOST_PORT=18444 docker compose up -d --build
```

Live ОФД:

```bash
OFD_MODE=live OFD_API_KEY=... OFD_INN=7707083893 docker compose up -d --build
```

Остановка: `docker compose down` (данные БД в volume `ofd_pgdata` сохраняются).

## Быстрый старт без Docker (demo / mock)

```bash
# PostgreSQL
sudo -u postgres createuser -P ofd   # пароль: ofd
sudo -u postgres createdb -O ofd ofd_marking

# Сервер (mock-данные)
chmod +x scripts/*.sh
./scripts/run-server-mock.sh
# API: http://127.0.0.1:18443/api/health

# Клиент (другой терминал)
./scripts/run-client.sh
```

Режим mock задаётся `OFD_MODE=mock` (по умолчанию). Реальный ключ не нужен.

## Установка в прод (Windows Server)

### 1. PostgreSQL

1. Установите PostgreSQL 16+.
2. Создайте БД `ofd_marking` и пользователя `ofd`.
3. Проверьте доступ с сервера приложения.

### 2. API KEY в личном кабинете ОФД

1. Войдите в ЛК ОФД.ру.
2. **Настройки → Управление передачей данных**.
3. Создайте заявку на доступ к API; ключ появится во вкладке «Ключи доступа к API ОФД».
4. Срок действия ключа — 1 год. В запросах передаётся как `AuthToken`.

### 3. Сервис (WinSW)

1. Соберите: `mvn -pl server -am -DskipTests package`.
2. Скопируйте `server/target/ofd-marking-server-*.jar` и `server/src/main/resources/winsw/ofd-marking-server.xml` на сервер.
3. Скачайте [WinSW](https://github.com/winsw/winsw/releases), переименуйте в `ofd-marking-server.exe`.
4. Задайте env (или правьте XML): `OFD_MODE=live`, `OFD_API_KEY=...`, `OFD_INN=...` (10 или 12 цифр), `DB_*`.
5. `ofd-marking-server.exe install` → `start`.

Порт по умолчанию: **18443**.

### 4. Клиент (MSI)

Сборка MSI через `jpackage` на Windows — см. [`scripts/package-msi.md`](scripts/package-msi.md).  
В настройках клиента укажите URL сервера (`http://<host>:18443`).

## Правила маркировки (ФФД 1.2)

Чек маркирован, если **хотя бы одна** позиция:

- содержит код маркировки в теге **1163** (`ProductCode` с непустым значением), **и/или**
- имеет тег **1212** (`SubjectType`) = **31** или **33**.

## Товарные группы

Бакалея, табак, никотинсодержащая, пиво, разливное пиво, вода, молочка, соки — rule-based справочник (`nomenclature_rule` в БД).

## API клиента

- `GET /api/trade-points`, `/trade-points/worst`, `/groups`, `/matrix`
- `GET /api/receipts`, `/receipts/{id}`
- `GET|PUT /api/settings`
- `GET /api/sync/status`, `POST /api/sync/trigger`
- `GET /api/export/{trade-points|groups|matrix}`

## Документация

- [`docs/requirements.md`](docs/requirements.md) — user stories и AC
- [`docs/api-mapping.md`](docs/api-mapping.md) — mapping ОФД (допущения Q1–Q4 изолированы)
- [`docs/roles/`](docs/roles/) — отчёты аналитика / backend / frontend / tester

## Тесты и CI

```bash
mvn clean verify
```

Testcontainers (PostgreSQL) + WireMock (ОФД) + e2e collection→metrics. CI: `.github/workflows/ci.yml`.

## Лимиты ОФД

- `receipts-info`: не чаще **1 запроса / 2 с**, окно **≤ 30 суток**, глубина **90 суток**.
- Основной метод: `/api/integration/v2/inn/{INN}/kkt/{RNM}/receipts-info`.

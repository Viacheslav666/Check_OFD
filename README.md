# Доля маркированных чеков по торговым точкам (ОФД.ру)

Сервис Windows Server + desktop-клиент JavaFX: сбор чеков из API [ОФД.ру](https://ofd.ru/razrabotchikam/ofd), хранение в PostgreSQL, расчёт доли чеков с маркированными товарами (ФФД 1.2: тег 1163 и/или SubjectType 31/33) по торговым точкам.

## Состав

| Модуль | Назначение |
|---|---|
| `server/` | Spring Boot 21, Flyway, сборщик ОФД, REST, WinSW |
| `client/` | JavaFX-клиент (дашборд, рейтинг, группы, матрица, чеки, Excel) |
| `docs/` | ТЗ→requirements, API mapping, отчёты ролей |

## Быстрый старт (demo / mock, без API KEY)

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

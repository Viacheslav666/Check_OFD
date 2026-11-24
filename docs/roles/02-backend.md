# Роль 2 — Backend

**Артефакты модуля `server/`:**

- Spring Boot 3.3 / Java 21, порт `18443`
- Flyway `V1__init.sql` — ТТ, ККТ, чеки, позиции, правила номенклатуры, settings, sync_state
- OFD-клиент: `RateLimitedHttpOfdClient` (live) + `MockOfdClient` (demo)
- Rate limit 2 с, retries на 429/5xx, окна ≤30 дн., глубина 90 дн.
- Mapping изолирован в `ofd.mapping.OfdMapper` + `OfdProperties` (Q1–Q4)
- Маркировка: `MarkingDetector` (1163 / SubjectType 31|33)
- Scheduler + startup sync, REST `/api/*`, Excel export, WinSW XML
- API KEY ОФД только из env `OFD_API_KEY`, маскируется в логах

**Запуск mock:** `OFD_MODE=mock mvn -pl server spring-boot:run`

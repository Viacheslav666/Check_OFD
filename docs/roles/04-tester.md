# Роль 4 — Tester

**Покрытие:**

- Unit: `MarkingDetectorTest`, `OfdMapperTest`, `ClientConfigTest`
- WireMock: `WireMockOfdClientTest` — KKT + receipts-info с ФФД 1.2 (с/без маркировки), retry 429
- Integration/E2E: `CollectionE2ETest` — Testcontainers PostgreSQL, mock OFD → ingest → `/api/trade-points` и worst
- CI: `.github/workflows/ci.yml` — `mvn clean verify`

Все тесты должны быть зелёными локально и в CI.

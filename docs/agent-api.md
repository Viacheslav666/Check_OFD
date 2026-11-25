# Контракт HTTP API агента (v1)

Инициатор всех запросов — центральное приложение. Базовый адрес: `http(s)://<IP агента>:<порт>` (по умолчанию 18500).
Константы и DTO — модуль `contract` (`ru.ofd.marking.contract`).

## Доступ

| Проверка | Где | Ошибка |
|---|---|---|
| IP источника TCP-соединения входит в `agent.allowed-ips` (IP, CIDR, IPv6) | все пути | `403 {"error":"IP … is not allowed"}` |
| Заголовок `X-Agent-Token` совпадает с `agent.token` | все пути, кроме `/agent/v1/health` | `401 {"error":"Invalid agent token"}` |

## Методы

### `GET /agent/v1/health`
`{"status":"UP","api":"v1","version":"2.0.0"}` — проверка доступности (только IP).

### `GET /agent/v1/status` → `AgentStatusDto`
```json
{"agentName":"РДП Самара","instanceId":"3f1c…","version":"2.0.0","mode":"live","apiKeyConfigured":true,
 "inns":["635001808204"],"kktCount":4,"receiptsStored":2160,"maxSeq":2160,"collecting":false,
 "lastCollectStartedAt":"…","lastCollectFinishedAt":"…","lastCollectSuccessAt":"…","lastCollectError":null,
 "lastCollectInserted":12,"collectedUpTo":"2026-11-25T07:45:00Z","serverTime":"…"}
```
`instanceId` меняется, только если локальная БД агента создана заново. Ключ ОФД не возвращается.

### `GET /agent/v1/kkts` → `AgentKktDto[]`
`[{"inn":"635001808204","kkt":{"Id":"…","KktRegId":"0008082040000001","KktName":"Касса 1","FnNumber":"…","FiscalAddress":"…","FiscalPlace":"…"}}]`
Поле `kkt` — как в ответе ОФД `/inn/{ИНН}/kkts`.

### `GET /agent/v1/receipts?after=<seq>&limit=<1..1000>` → `ReceiptPageDto`
```json
{"instanceId":"3f1c…","items":[{"seq":1,"inn":"635001808204","kktRegNumber":"0008…","collectedAt":"…",
  "receipt":{ "Id":"…","DocRawId":"…","FnNumber":"…","DocNumber":124154,"DocDateTime":"…","Items":[…] }}],
 "nextCursor":500,"hasMore":true,"maxSeq":2160}
```
- `receipt` — чек в формате ОФД `receipts-info` (ФФД 1.2, позиции, `ProductCode`, `SubjectType`).
- Записи упорядочены по `seq`; следующий запрос — `after=nextCursor`; `hasMore=false` — догнали.
- По умолчанию `limit=500`, максимум 1000.

### `POST /agent/v1/collect` → `CollectResultDto`
`{"accepted":true,"collecting":true,"message":"Сбор запущен"}` — сбор из ОФД вне расписания (в фоне).
Если сбор уже идёт — `accepted=false`. Окончание видно по `status.collecting=false`.

## Алгоритм центра

1. `status` → если `instanceId` сменился или `maxSeq < курсор` — курсор := 0.
2. `kkts` → upsert ККТ/ТТ (ТТ = адрес + место расчётов из ОФД), привязка к агенту и ИНН.
3. Цикл `receipts?after=курсор` → запись чеков (дедуп: Id ОФД, затем ФН+ФД) → сохранение курсора после страницы.
4. Ошибки агента не влияют на остальных; в карточке агента — текст ошибки и число неудачных опросов подряд.

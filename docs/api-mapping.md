# Mapping слой ОФД.ру → доменная модель

**Важно:** имена и формы полей ОФД **не** протекают в домен/REST клиента. Всё преобразование — в `ru.ofd.marking.ofd.mapping`.

## Методы ОФД, которые используем

| Назначение | Метод | Лимит | Период |
|---|---|---|---|
| Список ККТ | `GET /api/integration/v2/inn/{INN}/kkts?AuthToken=` (2.1 / 3.2) | 1/сек | — |
| Чеки с деталями/марками | `GET /api/integration/v2/inn/{INN}/kkt/{RNM}/receipts-info?dateFrom&dateTo&AuthToken=` (2.10.1) | **1 / 2 с** | ≤30 дн., глубина 90 дн. |

Auth: query `AuthToken={API KEY}`. Ключ **никогда** не логируется (маскируется в URI логов).

## JSON → Domain

### ККТ (`OfdKktDto` → `Kkt` + `TradePoint`)

| ОФД поле | Домен | Примечание |
|---|---|---|
| `Id` | `kkt.ofd_id` | UUID кассы в ИС |
| `KktRegId` | `kkt.reg_number` (РНМ) | путь `{RNM}` в receipts-info |
| `KktName` | `kkt.name` | |
| `SerialNumber` | `kkt.serial_number` | |
| `FnNumber` | `kkt.fn_number` | |
| `FiscalAddress` | часть ключа ТТ | адрес установки |
| `FiscalPlace` | часть ключа ТТ | место расчётов |
| `Path` | `kkt.lk_path` | опционально |

Ключ ТТ: `normalize(FiscalAddress) + " | " + normalize(FiscalPlace)`.

### Чек (`OfdReceiptDto` → `Receipt`)

| ОФД поле | Домен |
|---|---|
| `Id` / `DocRawId` | `receipt.ofd_raw_id` (unique) |
| `Tag` | `receipt.doc_tag` (3/31/4/41) |
| `IsBso`, `IsCorrection` | флаги |
| `OperationType` | enum string |
| `UserInn` | `receipt.user_inn` |
| `KktRegNumber` | связь с ККТ |
| `FnNumber`, `DocNumber`, `DocShiftNumber`, `ReceiptNumber` | реквизиты |
| `DocDateTime` | `receipt.doc_date_time` (**основной** для метрик) |
| `CDateUtc` | `receipt.c_date_utc` |
| `TotalSumm` и суммы | копейки → `BigDecimal` / long |
| `Items[]` | `receipt_item` |
| `DecimalFiscalSign` | ФП |

### Позиция → `ReceiptItem` + маркировка

| ОФД поле | Домен / правило |
|---|---|
| `Name` | `name` + классификация группы |
| `Price`, `Quantity`, `Total` | |
| `SubjectType` (тег 1212) | `subject_type`; **31 или 33 ⇒ marked** |
| `ProductCode` (тег 1163) | JSON; любой непустой `Code_*` ⇒ marked |
| `ProductCheckResultDetails` | справочно |
| `NDS_Rate`, `NDS_Summ` | |

`ProductCode` keys (из табл. 14): `Code_Undefined`, `Code_EAN_8`, `Code_EAN_13`, `Code_ITF_14`, `Code_GS_1`, `Code_GS_1M`, `Code_KMK`, `Code_MI`, `Code_EGAIS_2`, `Code_EGAIS_3`, `Code_F_1`…`Code_F_6`.

Пустой `{}` → нет кода.

## Assumptions (конфиг)

```yaml
ofd:
  base-url: https://ofd.ru
  assume-receipts-info-all: true   # Q4
  filter-time-field: DOC_DATE_TIME # Q2
  rate-limit-ms: 2000
  max-window-days: 30
  max-depth-days: 90
  inn-pattern: '^\d{10}(\d{2})?$'  # Q3
```

Неизвестные JSON-поля игнорируются (`FAIL_ON_UNKNOWN_PROPERTIES=false`).

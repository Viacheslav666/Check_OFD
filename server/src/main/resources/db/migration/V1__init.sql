CREATE TABLE trade_point (
    id              BIGSERIAL PRIMARY KEY,
    key_hash        VARCHAR(64)  NOT NULL UNIQUE,
    address         TEXT         NOT NULL,
    place           TEXT         NOT NULL,
    display_name    TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE kkt (
    id              BIGSERIAL PRIMARY KEY,
    ofd_id          VARCHAR(64),
    reg_number      VARCHAR(32)  NOT NULL UNIQUE,
    name            TEXT,
    serial_number   VARCHAR(64),
    fn_number       VARCHAR(64),
    lk_path         TEXT,
    trade_point_id  BIGINT       NOT NULL REFERENCES trade_point(id),
    last_seen_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kkt_trade_point ON kkt(trade_point_id);

CREATE TABLE receipt (
    id                  BIGSERIAL PRIMARY KEY,
    ofd_raw_id          VARCHAR(64)  NOT NULL UNIQUE,
    doc_tag             INT          NOT NULL,
    is_bso              BOOLEAN      NOT NULL DEFAULT FALSE,
    is_correction       BOOLEAN      NOT NULL DEFAULT FALSE,
    operation_type      VARCHAR(32),
    user_inn            VARCHAR(12),
    kkt_reg_number      VARCHAR(32)  NOT NULL,
    trade_point_id      BIGINT       NOT NULL REFERENCES trade_point(id),
    fn_number           VARCHAR(64),
    doc_number          BIGINT,
    doc_shift_number    INT,
    receipt_number      INT,
    doc_date_time       TIMESTAMPTZ  NOT NULL,
    c_date_utc          TIMESTAMPTZ,
    total_sum_kopecks   BIGINT       NOT NULL DEFAULT 0,
    fiscal_sign         VARCHAR(64),
    has_marked_goods    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_receipt_tp_dt ON receipt(trade_point_id, doc_date_time);
CREATE INDEX idx_receipt_marked ON receipt(has_marked_goods);
CREATE INDEX idx_receipt_kkt ON receipt(kkt_reg_number);

CREATE TABLE receipt_item (
    id                  BIGSERIAL PRIMARY KEY,
    receipt_id          BIGINT       NOT NULL REFERENCES receipt(id) ON DELETE CASCADE,
    line_no             INT          NOT NULL,
    name                TEXT         NOT NULL,
    price_kopecks       BIGINT,
    quantity            NUMERIC(18,6),
    total_kopecks       BIGINT,
    subject_type        INT,
    product_code_json   TEXT,
    product_group       VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    is_marked           BOOLEAN      NOT NULL DEFAULT FALSE,
    nds_rate            INT
);

CREATE INDEX idx_item_receipt ON receipt_item(receipt_id);
CREATE INDEX idx_item_group ON receipt_item(product_group);

CREATE TABLE nomenclature_rule (
    id              BIGSERIAL PRIMARY KEY,
    product_group   VARCHAR(32)  NOT NULL,
    pattern         TEXT         NOT NULL,
    priority        INT          NOT NULL DEFAULT 100,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    description     TEXT
);

CREATE TABLE app_settings (
    key             VARCHAR(64) PRIMARY KEY,
    value           TEXT         NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE sync_state (
    id                  BIGSERIAL PRIMARY KEY,
    kkt_reg_number      VARCHAR(32)  NOT NULL UNIQUE,
    last_success_to     TIMESTAMPTZ,
    last_attempt_at     TIMESTAMPTZ,
    last_error          TEXT,
    initial_done        BOOLEAN      NOT NULL DEFAULT FALSE
);

INSERT INTO app_settings(key, value) VALUES
    ('worst_threshold_percent', '25.0'),
    ('default_period_days', '30');

-- Rule-based nomenclature (higher priority = checked first)
INSERT INTO nomenclature_rule(product_group, pattern, priority, description) VALUES
    ('TOBACCO', '(?i)(табак|сигарет|стик|iqos|heet|парламент|winston|camel|marlboro|philip\\s*morris)', 10, 'Табак'),
    ('NICOTINE', '(?i)(никотин|вейп|vape|жидкост.*(ник|пар)|pod|snus|снюс)', 20, 'Никотинсодержащая'),
    ('DRAFT_BEER', '(?i)(разливн.*(пив|эль)|пив.*разлив|draft)', 30, 'Разливное пиво'),
    ('BEER', '(?i)(пиво|lager|эль\\b|стаут|porter|балтика|крушовиц|хайнекен)', 40, 'Пиво'),
    ('WATER', '(?i)(вода\\b|aqua|borjomi|нарзан|ессентук|минерал)', 50, 'Вода'),
    ('DAIRY', '(?i)(молоко|кефир|йогурт|творог|сметана|сыр\\b|ряженк|простокваш)', 60, 'Молочка'),
    ('JUICE', '(?i)(сок\\b|нектар|smoothie|смузи|добрый|rich\\b|j7\\b)', 70, 'Соки'),
    ('BAKALEYA', '(?i)(крупа|мука|сахар|соль|макарон|рис\\b|гречк|консерв|масло\\s*подсолн|чай\\b|кофе\\b)', 80, 'Бакалея');

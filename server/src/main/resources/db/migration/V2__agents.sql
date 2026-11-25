-- v2.0: данные приходят от агентов на RDP клиентов, а не напрямую из ОФД.
CREATE TABLE agent (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL UNIQUE,
    host                VARCHAR(255) NOT NULL,
    port                INT          NOT NULL,
    use_tls             BOOLEAN      NOT NULL DEFAULT FALSE,
    token               TEXT         NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    comment             TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_cursor         BIGINT       NOT NULL DEFAULT 0,
    agent_instance_id   VARCHAR(64),
    agent_max_seq       BIGINT,
    last_poll_at        TIMESTAMP WITH TIME ZONE,
    last_success_at     TIMESTAMP WITH TIME ZONE,
    last_error          TEXT,
    last_status_json    TEXT,
    receipts_received   BIGINT       NOT NULL DEFAULT 0,
    consecutive_failures INT         NOT NULL DEFAULT 0
);

ALTER TABLE kkt ADD COLUMN agent_id BIGINT;
ALTER TABLE kkt ADD COLUMN inn VARCHAR(12);
ALTER TABLE kkt ADD CONSTRAINT fk_kkt_agent FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE SET NULL;

ALTER TABLE receipt ADD COLUMN agent_id BIGINT;
ALTER TABLE receipt ADD CONSTRAINT fk_receipt_agent FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE SET NULL;
CREATE INDEX idx_receipt_agent ON receipt(agent_id);
-- Фискальная идентичность чека: номер ФН + номер ФД (дедупликация между агентами)
CREATE INDEX idx_receipt_fiscal ON receipt(fn_number, doc_number);
CREATE INDEX idx_receipt_inn ON receipt(user_inn);

-- Состояние синхронизации теперь хранится в таблице agent (курсор) и на самих агентах.
DROP TABLE sync_state;

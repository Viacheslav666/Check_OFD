package ru.ofd.agent.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.ofd.marking.contract.AgentKktDto;
import ru.ofd.marking.contract.AgentReceiptDto;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

/**
 * Локальное хранилище агента (H2 в файле). Чеки хранятся «как пришли из ОФД» (JSON),
 * каждой новой записи присваивается монотонный seq — курсор синхронизации центра.
 */
@Repository
public class AgentStore {

    private static final String[] DDL = {
        "CREATE TABLE IF NOT EXISTS agent_meta (meta_key VARCHAR(64) PRIMARY KEY, meta_value VARCHAR(1000))",
        "CREATE TABLE IF NOT EXISTS agent_kkt (reg_number VARCHAR(32) PRIMARY KEY, inn VARCHAR(12) NOT NULL,"
                + " payload CLOB NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL)",
        "CREATE TABLE IF NOT EXISTS agent_receipt (seq BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + " raw_id VARCHAR(64) NOT NULL UNIQUE, inn VARCHAR(12) NOT NULL, kkt_reg_number VARCHAR(32) NOT NULL,"
                + " doc_date_time TIMESTAMP WITH TIME ZONE, collected_at TIMESTAMP WITH TIME ZONE NOT NULL, payload CLOB NOT NULL)",
        "CREATE INDEX IF NOT EXISTS idx_agent_receipt_doc ON agent_receipt(doc_date_time)",
        "CREATE TABLE IF NOT EXISTS agent_collect_state (kkt_reg_number VARCHAR(32) PRIMARY KEY, inn VARCHAR(12) NOT NULL,"
                + " last_success_to TIMESTAMP WITH TIME ZONE, initial_done BOOLEAN DEFAULT FALSE NOT NULL,"
                + " last_attempt_at TIMESTAMP WITH TIME ZONE, last_error VARCHAR(2000))"
    };

    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private String instanceId;

    public AgentStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        for (String sql : DDL) {
            jdbc.execute(sql);
        }
        List<String> ids = jdbc.queryForList("SELECT meta_value FROM agent_meta WHERE meta_key = 'instance_id'", String.class);
        if (ids.isEmpty()) {
            instanceId = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO agent_meta(meta_key, meta_value) VALUES ('instance_id', ?)", instanceId);
        } else {
            instanceId = ids.get(0);
        }
    }

    public String instanceId() {
        return instanceId;
    }

    // ---------- ККТ ----------

    public void upsertKkt(String inn, OfdKktDto kkt) {
        String json = write(kkt);
        int n = jdbc.update("UPDATE agent_kkt SET inn = ?, payload = ?, updated_at = ? WHERE reg_number = ?",
                inn, json, ts(Instant.now()), kkt.getKktRegId());
        if (n == 0) {
            jdbc.update("INSERT INTO agent_kkt(reg_number, inn, payload, updated_at) VALUES (?,?,?,?)",
                    kkt.getKktRegId(), inn, json, ts(Instant.now()));
        }
    }

    public List<AgentKktDto> kkts() {
        return jdbc.query("SELECT inn, payload FROM agent_kkt ORDER BY inn, reg_number",
                (rs, i) -> new AgentKktDto(rs.getString("inn"), read(rs.getString("payload"), OfdKktDto.class)));
    }

    public int kktCount() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM agent_kkt", Integer.class);
        return n == null ? 0 : n;
    }

    // ---------- Чеки ----------

    /** @return true, если чек новый и сохранён; false — такой raw_id уже есть. */
    public boolean insertReceiptIfAbsent(String inn, String kktRegNumber, String rawId, Instant docDateTime,
                                         OfdReceiptDto receipt) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM agent_receipt WHERE raw_id = ?", Integer.class, rawId);
        if (exists != null && exists > 0) {
            return false;
        }
        try {
            jdbc.update("INSERT INTO agent_receipt(raw_id, inn, kkt_reg_number, doc_date_time, collected_at, payload)"
                            + " VALUES (?,?,?,?,?,?)",
                    rawId, inn, kktRegNumber, docDateTime == null ? null : ts(docDateTime), ts(Instant.now()), write(receipt));
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    public List<AgentReceiptDto> receiptsAfter(long after, int limit) {
        return jdbc.query("SELECT seq, inn, kkt_reg_number, collected_at, payload FROM agent_receipt"
                        + " WHERE seq > ? ORDER BY seq LIMIT ?",
                (rs, i) -> new AgentReceiptDto(rs.getLong("seq"), rs.getString("inn"), rs.getString("kkt_reg_number"),
                        instant(rs, "collected_at"), read(rs.getString("payload"), OfdReceiptDto.class)),
                after, limit);
    }

    public long maxSeq() {
        Long v = jdbc.queryForObject("SELECT COALESCE(MAX(seq), 0) FROM agent_receipt", Long.class);
        return v == null ? 0 : v;
    }

    public long receiptCount() {
        Long v = jdbc.queryForObject("SELECT COUNT(*) FROM agent_receipt", Long.class);
        return v == null ? 0 : v;
    }

    public int purgeOlderThan(Instant border) {
        return jdbc.update("DELETE FROM agent_receipt WHERE doc_date_time < ?", ts(border));
    }

    // ---------- Состояние сбора по ККТ ----------

    public record CollectState(String kktRegNumber, String inn, Instant lastSuccessTo, boolean initialDone,
                               Instant lastAttemptAt, String lastError) {}

    public Optional<CollectState> collectState(String rnm) {
        return jdbc.query("SELECT * FROM agent_collect_state WHERE kkt_reg_number = ?",
                (rs, i) -> new CollectState(rs.getString("kkt_reg_number"), rs.getString("inn"),
                        instant(rs, "last_success_to"), rs.getBoolean("initial_done"),
                        instant(rs, "last_attempt_at"), rs.getString("last_error")), rnm).stream().findFirst();
    }

    public void saveCollectState(CollectState s) {
        String err = s.lastError() == null ? null : s.lastError().substring(0, Math.min(1900, s.lastError().length()));
        int n = jdbc.update("UPDATE agent_collect_state SET inn=?, last_success_to=?, initial_done=?, last_attempt_at=?,"
                        + " last_error=? WHERE kkt_reg_number=?",
                s.inn(), ts(s.lastSuccessTo()), s.initialDone(), ts(s.lastAttemptAt()), err, s.kktRegNumber());
        if (n == 0) {
            jdbc.update("INSERT INTO agent_collect_state(kkt_reg_number, inn, last_success_to, initial_done,"
                            + " last_attempt_at, last_error) VALUES (?,?,?,?,?,?)",
                    s.kktRegNumber(), s.inn(), ts(s.lastSuccessTo()), s.initialDone(), ts(s.lastAttemptAt()), err);
        }
    }

    /** Минимальная граница, до которой собраны данные по всем ККТ (отставание агента от ОФД). */
    public Instant collectedUpTo() {
        List<Timestamp> v = jdbc.queryForList("SELECT MIN(last_success_to) FROM agent_collect_state", Timestamp.class);
        return v.isEmpty() || v.get(0) == null ? null : v.get(0).toInstant();
    }

    // ---------- утилиты ----------

    private static Timestamp ts(Instant i) {
        return i == null ? null : Timestamp.from(i);
    }

    private static Instant instant(ResultSet rs, String col) throws SQLException {
        OffsetDateTime odt = rs.getObject(col, OffsetDateTime.class);
        return odt == null ? null : odt.toInstant();
    }

    private String write(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

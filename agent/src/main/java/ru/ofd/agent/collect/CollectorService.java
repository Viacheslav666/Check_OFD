package ru.ofd.agent.collect;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ofd.agent.config.AgentProperties;
import ru.ofd.agent.config.OfdProperties;
import ru.ofd.agent.ofd.OfdApiClient;
import ru.ofd.agent.store.AgentStore;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

/**
 * Сбор чеков из ОФД.ру: по каждой организации (ИНН) — список ККТ, по каждой ККТ — окна не длиннее
 * max-window-days (30), первичная загрузка на max-depth-days (90) назад, далее инкрементально
 * с перекрытием 1 час. Лимит ОФД 1 запрос / 2 с соблюдается в клиенте ОФД.
 */
@Service
public class CollectorService {

    private static final Logger log = LoggerFactory.getLogger(CollectorService.class);
    public static final String VERSION = "2.0.0";

    private final OfdApiClient ofd;
    private final OfdProperties ofdProps;
    private final AgentProperties agentProps;
    private final AgentStore store;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<Instant> lastStartedAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastFinishedAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastSuccessAt = new AtomicReference<>();
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final AtomicLong lastInserted = new AtomicLong();

    public CollectorService(OfdApiClient ofd, OfdProperties ofdProps, AgentProperties agentProps, AgentStore store) {
        this.ofd = ofd;
        this.ofdProps = ofdProps;
        this.agentProps = agentProps;
        this.store = store;
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Запуск в фоне; false — сбор уже идёт. */
    public boolean startAsync() {
        if (running.get()) {
            return false;
        }
        Thread t = new Thread(this::collect, "ofd-collect");
        t.setDaemon(true);
        t.start();
        return true;
    }

    public void collect() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        lastStartedAt.set(Instant.now());
        long inserted = 0;
        StringBuilder errors = new StringBuilder();
        try {
            Instant now = Instant.now();
            for (String inn : ofdProps.getInns()) {
                List<OfdKktDto> kkts;
                try {
                    kkts = ofd.listKkts(inn);
                } catch (Exception e) {
                    log.error("ИНН {}: не удалось получить список ККТ: {}", inn, e.getMessage());
                    errors.append("ИНН ").append(inn).append(": ").append(e.getMessage()).append("; ");
                    continue;
                }
                for (OfdKktDto k : kkts) {
                    if (k.getKktRegId() == null || k.getKktRegId().isBlank()) continue;
                    store.upsertKkt(inn, k);
                    try {
                        inserted += collectKkt(inn, k.getKktRegId(), now);
                    } catch (Exception e) {
                        log.error("ККТ {}: ошибка сбора: {}", k.getKktRegId(), e.getMessage());
                        errors.append("ККТ ").append(k.getKktRegId()).append(": ").append(e.getMessage()).append("; ");
                    }
                }
            }
            int purged = store.purgeOlderThan(now.minus(agentProps.getRetentionDays(), ChronoUnit.DAYS));
            if (purged > 0) {
                log.info("Удалено {} чеков старше {} дней", purged, agentProps.getRetentionDays());
            }
            lastInserted.set(inserted);
            if (errors.isEmpty()) {
                lastError.set(null);
                lastSuccessAt.set(Instant.now());
            } else {
                lastError.set(errors.toString());
            }
            log.info("Сбор завершён: новых чеков {}, ошибок: {}", inserted, errors.isEmpty() ? "нет" : errors);
        } catch (Exception e) {
            lastError.set(e.getMessage());
            log.error("Сбор упал", e);
        } finally {
            lastFinishedAt.set(Instant.now());
            running.set(false);
        }
    }

    private long collectKkt(String inn, String rnm, Instant end) {
        AgentStore.CollectState st = store.collectState(rnm)
                .orElse(new AgentStore.CollectState(rnm, inn, null, false, null, null));
        Instant from;
        Instant depthBorder = end.minus(ofdProps.getMaxDepthDays(), ChronoUnit.DAYS);
        if (st.initialDone() && st.lastSuccessTo() != null) {
            from = st.lastSuccessTo().minus(1, ChronoUnit.HOURS);
            if (from.isBefore(depthBorder)) from = depthBorder;
        } else {
            from = depthBorder;
        }
        Instant successTo = st.lastSuccessTo();
        long inserted = 0;
        try {
            while (from.isBefore(end)) {
                Instant to = from.plus(ofdProps.getMaxWindowDays(), ChronoUnit.DAYS);
                if (to.isAfter(end)) to = end;
                List<OfdReceiptDto> receipts = ofd.receiptsInfo(inn, rnm, from, to);
                for (OfdReceiptDto r : receipts) {
                    String rawId = r.getDocRawId() != null && !r.getDocRawId().isBlank() ? r.getDocRawId() : r.getId();
                    if (rawId == null || rawId.isBlank()) continue;
                    if (r.getKktRegNumber() == null) r.setKktRegNumber(rnm);
                    if (store.insertReceiptIfAbsent(inn, rnm, rawId, parseDocDate(r.getDocDateTime()), r)) {
                        inserted++;
                    }
                }
                successTo = to;
                from = to;
            }
            store.saveCollectState(new AgentStore.CollectState(rnm, inn, successTo, true, Instant.now(), null));
        } catch (RuntimeException e) {
            store.saveCollectState(new AgentStore.CollectState(rnm, inn, successTo, st.initialDone(), Instant.now(),
                    e.getMessage()));
            throw e;
        }
        return inserted;
    }

    /** Дата документа ОФД — локальное время кассы без зоны; для хранения считаем по МСК. */
    static Instant parseDocDate(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Instant.parse(v.endsWith("Z") ? v : v + "Z");
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(v).atZone(ZoneId.of("Europe/Moscow")).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public AgentStatusDto status() {
        return new AgentStatusDto(
                agentProps.getName(),
                store.instanceId(),
                VERSION,
                ofdProps.getMode().name(),
                ofdProps.getApiKey() != null && !ofdProps.getApiKey().isBlank(),
                ofdProps.getInns(),
                store.kktCount(),
                store.receiptCount(),
                store.maxSeq(),
                running.get(),
                lastStartedAt.get(),
                lastFinishedAt.get(),
                lastSuccessAt.get(),
                lastError.get(),
                lastInserted.get(),
                store.collectedUpTo(),
                Instant.now());
    }
}

package ru.ofd.marking.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.ofd.marking.config.CentralProperties;
import ru.ofd.marking.contract.AgentKktDto;
import ru.ofd.marking.contract.AgentReceiptDto;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.contract.ReceiptPageDto;
import ru.ofd.marking.domain.entity.Agent;
import ru.ofd.marking.domain.entity.Kkt;
import ru.ofd.marking.domain.repo.AgentRepository;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.service.ReceiptIngestService;
import ru.ofd.marking.service.TradePointService;

/**
 * Центр опрашивает агентов: статус → список ККТ → чеки после сохранённого курсора (страницами).
 * Курсор фиксируется после каждой успешно записанной страницы; повторная выдача безопасна
 * благодаря дедупликации (Id ОФД, затем ФН+ФД).
 */
@Service
public class AgentSyncService {

    private static final Logger log = LoggerFactory.getLogger(AgentSyncService.class);

    private final AgentRepository agentRepository;
    private final AgentHttpClient client;
    private final TradePointService tradePointService;
    private final ReceiptIngestService ingestService;
    private final CentralProperties props;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;
    private final ExecutorService pool;
    /** Запись в БД — последовательно, чтобы параллельные агенты не создавали одну ТТ/чек дважды. */
    private final Object writeLock = new Object();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> lastStatus = new AtomicReference<>("Ожидание первого опроса агентов");
    private final AtomicReference<Instant> lastFinishedAt = new AtomicReference<>();

    public AgentSyncService(AgentRepository agentRepository, AgentHttpClient client, TradePointService tradePointService,
                            ReceiptIngestService ingestService, CentralProperties props, TransactionTemplate tx,
                            ObjectMapper mapper) {
        this.agentRepository = agentRepository;
        this.client = client;
        this.tradePointService = tradePointService;
        this.ingestService = ingestService;
        this.props = props;
        this.tx = tx;
        this.mapper = mapper;
        this.pool = Executors.newFixedThreadPool(Math.max(1, props.getParallelism()), r -> {
            Thread t = new Thread(r, "agent-poll");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    public record SyncStatus(boolean running, String message, Instant lastFinishedAt, String mode) {}

    public record PollResult(Long agentId, String agentName, boolean ok, int kkts, long inserted, long received,
                             String error) {}

    public SyncStatus status() {
        return new SyncStatus(running.get(), lastStatus.get(), lastFinishedAt.get(), "agents");
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Опрос всех включённых агентов. collectFirst — сначала попросить агентов собрать свежие данные из ОФД. */
    public SyncStatus runSync(boolean collectFirst) {
        if (!running.compareAndSet(false, true)) {
            return status();
        }
        lastStatus.set("Опрос агентов…");
        try {
            List<Agent> agents = agentRepository.findByEnabledTrueOrderByNameAsc();
            if (agents.isEmpty()) {
                lastStatus.set("Нет включённых агентов — добавьте агента во вкладке «Агенты»");
                return status();
            }
            if (collectFirst) {
                requestCollectAndWait(agents);
            }
            List<Future<PollResult>> futures = new ArrayList<>();
            for (Agent a : agents) {
                futures.add(pool.submit(() -> pollAgent(a.getId())));
            }
            int ok = 0;
            long inserted = 0;
            List<String> failed = new ArrayList<>();
            for (Future<PollResult> f : futures) {
                PollResult r = f.get();
                if (r.ok()) {
                    ok++;
                } else {
                    failed.add(r.agentName() + ": " + r.error());
                }
                inserted += r.inserted();
            }
            String msg = (failed.isEmpty() ? "OK" : "Есть ошибки") + ": агентов на связи " + ok + "/" + agents.size()
                    + ", новых чеков " + inserted + (failed.isEmpty() ? "" : "; " + String.join("; ", failed));
            lastStatus.set(msg);
            log.info("Опрос агентов завершён: {}", msg);
        } catch (Exception e) {
            lastStatus.set("ERROR: " + e.getMessage());
            log.error("Опрос агентов упал", e);
        } finally {
            lastFinishedAt.set(Instant.now());
            running.set(false);
        }
        return status();
    }

    private void requestCollectAndWait(List<Agent> agents) throws InterruptedException {
        List<Agent> started = new ArrayList<>();
        for (Agent a : agents) {
            try {
                client.collect(a);
                started.add(a);
            } catch (Exception e) {
                log.warn("Агент {}: не удалось запустить сбор: {}", a.getName(), e.getMessage());
            }
        }
        long deadline = System.currentTimeMillis() + props.getCollectWaitSeconds() * 1000L;
        Thread.sleep(300);
        while (!started.isEmpty() && System.currentTimeMillis() < deadline) {
            started.removeIf(a -> {
                try {
                    return !client.status(a).collecting();
                } catch (Exception e) {
                    return true;
                }
            });
            if (!started.isEmpty()) {
                lastStatus.set("Агенты собирают данные из ОФД: " + started.size());
                Thread.sleep(1000);
            }
        }
    }

    /** Опрос одного агента (используется расписанием, кнопкой и тестом в ЛК). */
    public PollResult pollAgent(Long agentId) {
        Agent a = agentRepository.findById(agentId).orElseThrow();
        Instant started = Instant.now();
        long inserted = 0;
        long received = 0;
        int kktCount = 0;
        try {
            AgentStatusDto st = client.status(a);
            long cursor = a.getLastCursor();
            boolean reset = false;
            if (a.getAgentInstanceId() != null && !a.getAgentInstanceId().equals(st.instanceId())) {
                log.warn("Агент {}: база агента пересоздана (instanceId сменился) — перечитываем чеки с начала", a.getName());
                reset = true;
            } else if (st.maxSeq() < cursor) {
                log.warn("Агент {}: курсор центра {} больше maxSeq агента {} — сбрасываем курсор", a.getName(), cursor, st.maxSeq());
                reset = true;
            }
            if (reset) {
                cursor = 0;
            }
            final long startCursor = cursor;
            update(agentId, x -> {
                x.setAgentInstanceId(st.instanceId());
                x.setLastCursor(startCursor);
                x.setLastStatusJson(json(st));
                x.setLastPollAt(started);
            });

            Map<String, Kkt> kktByRnm = new HashMap<>();
            List<AgentKktDto> kkts = client.kkts(a);
            kktCount = kkts.size();
            synchronized (writeLock) {
                for (AgentKktDto k : kkts) {
                    if (k.kkt() == null || k.kkt().getKktRegId() == null) continue;
                    Kkt kkt = tradePointService.upsertKkt(k.kkt(), agentId, k.inn());
                    kktByRnm.put(kkt.getRegNumber(), kkt);
                }
            }

            ReceiptPageDto page;
            do {
                page = client.receipts(a, cursor, Math.min(props.getPageSize(), 1000));
                long pageInserted = 0;
                synchronized (writeLock) {
                    for (AgentReceiptDto r : page.items()) {
                        if (r.receipt() == null) continue;
                        String rnm = r.kktRegNumber() != null ? r.kktRegNumber() : r.receipt().getKktRegNumber();
                        Kkt kkt = kktByRnm.computeIfAbsent(rnm, x -> resolveUnknownKkt(x, a, r.inn()));
                        if (r.receipt().getUserInn() == null) r.receipt().setUserInn(r.inn());
                        if (ingestService.ingest(r.receipt(), kkt, agentId)) {
                            pageInserted++;
                        }
                    }
                }
                inserted += pageInserted;
                received += page.items().size();
                if (page.nextCursor() < cursor) {
                    throw new IllegalStateException("агент вернул курсор меньше текущего");
                }
                cursor = page.nextCursor();
                final long c = cursor;
                final long ins = pageInserted;
                final long maxSeq = page.maxSeq();
                update(agentId, x -> {
                    x.setLastCursor(c);
                    x.setAgentMaxSeq(maxSeq);
                    x.setReceiptsReceived(x.getReceiptsReceived() + ins);
                });
            } while (page.hasMore() && !page.items().isEmpty());

            update(agentId, x -> {
                x.setLastSuccessAt(Instant.now());
                x.setLastError(null);
                x.setConsecutiveFailures(0);
            });
            log.info("Агент {}: ККТ {}, получено чеков {}, новых {}, курсор {}", a.getName(), kktCount, received, inserted, cursor);
            return new PollResult(agentId, a.getName(), true, kktCount, inserted, received, null);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.warn("Агент {} ({}): ошибка опроса: {}", a.getName(), a.baseUrl(), msg);
            update(agentId, x -> {
                x.setLastPollAt(started);
                x.setLastError(msg);
                x.setConsecutiveFailures(x.getConsecutiveFailures() + 1);
            });
            return new PollResult(agentId, a.getName(), false, kktCount, inserted, received, msg);
        }
    }

    private Kkt resolveUnknownKkt(String rnm, Agent a, String inn) {
        return tradePointService.findKkt(rnm).orElseGet(() -> {
            OfdKktDto dto = new OfdKktDto();
            dto.setKktRegId(rnm);
            dto.setKktName("ККТ " + rnm);
            dto.setFiscalAddress("Адрес не указан (агент " + a.getName() + ")");
            dto.setFiscalPlace("");
            return tradePointService.upsertKkt(dto, a.getId(), inn);
        });
    }

    public void resetCursor(Long agentId) {
        update(agentId, x -> {
            x.setLastCursor(0);
            x.setAgentInstanceId(null);
        });
    }

    private void update(Long agentId, Consumer<Agent> change) {
        tx.executeWithoutResult(s -> agentRepository.findById(agentId).ifPresent(x -> {
            change.accept(x);
            agentRepository.save(x);
        }));
    }

    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }
}

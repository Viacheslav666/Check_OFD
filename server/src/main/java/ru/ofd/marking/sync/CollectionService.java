package ru.ofd.marking.sync;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ofd.marking.config.OfdProperties;
import ru.ofd.marking.domain.entity.Kkt;
import ru.ofd.marking.domain.entity.SyncState;
import ru.ofd.marking.domain.repo.SyncStateRepository;
import ru.ofd.marking.ofd.client.OfdApiClient;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;
import ru.ofd.marking.service.ReceiptIngestService;
import ru.ofd.marking.service.TradePointService;

@Service
public class CollectionService {

    private static final Logger log = LoggerFactory.getLogger(CollectionService.class);

    private final OfdApiClient ofdApiClient;
    private final OfdProperties props;
    private final TradePointService tradePointService;
    private final ReceiptIngestService ingestService;
    private final SyncStateRepository syncStateRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> lastStatus = new AtomicReference<>("IDLE");
    private final AtomicReference<Instant> lastFinishedAt = new AtomicReference<>(null);

    public CollectionService(OfdApiClient ofdApiClient,
                             OfdProperties props,
                             TradePointService tradePointService,
                             ReceiptIngestService ingestService,
                             SyncStateRepository syncStateRepository) {
        this.ofdApiClient = ofdApiClient;
        this.props = props;
        this.tradePointService = tradePointService;
        this.ingestService = ingestService;
        this.syncStateRepository = syncStateRepository;
    }

    public SyncStatus status() {
        return new SyncStatus(running.get(), lastStatus.get(), lastFinishedAt.get(), props.getMode().name());
    }

    public synchronized SyncStatus runSync() {
        if (!running.compareAndSet(false, true)) {
            return status();
        }
        lastStatus.set("RUNNING");
        int inserted = 0;
        int kkts = 0;
        try {
            String inn = props.getInn();
            List<OfdKktDto> ofdKkts = ofdApiClient.listKkts(inn);
            kkts = ofdKkts.size();
            Instant now = Instant.now();
            for (OfdKktDto dto : ofdKkts) {
                Kkt kkt = tradePointService.upsertKkt(dto);
                inserted += syncKkt(inn, kkt, now);
            }
            lastStatus.set("OK: kkts=" + kkts + ", inserted=" + inserted);
            log.info("Sync finished: kkts={}, inserted={}", kkts, inserted);
        } catch (Exception e) {
            lastStatus.set("ERROR: " + e.getMessage());
            log.error("Sync failed", e);
        } finally {
            lastFinishedAt.set(Instant.now());
            running.set(false);
        }
        return status();
    }

    private int syncKkt(String inn, Kkt kkt, Instant now) {
        SyncState state = syncStateRepository.findByKktRegNumber(kkt.getRegNumber())
                .orElseGet(() -> {
                    SyncState s = new SyncState();
                    s.setKktRegNumber(kkt.getRegNumber());
                    return s;
                });
        state.setLastAttemptAt(Instant.now());
        int inserted = 0;
        try {
            Instant cursorFrom;
            Instant end = now;
            if (!state.isInitialDone()) {
                cursorFrom = now.minus(props.getMaxDepthDays(), ChronoUnit.DAYS);
            } else if (state.getLastSuccessTo() != null) {
                cursorFrom = state.getLastSuccessTo().minus(1, ChronoUnit.HOURS);
            } else {
                cursorFrom = now.minus(props.getMaxDepthDays(), ChronoUnit.DAYS);
            }

            while (cursorFrom.isBefore(end)) {
                Instant windowTo = cursorFrom.plus(props.getMaxWindowDays(), ChronoUnit.DAYS);
                if (!windowTo.isBefore(end)) {
                    windowTo = end;
                }
                if (!windowTo.isAfter(cursorFrom)) {
                    break;
                }
                List<OfdReceiptDto> receipts = ofdApiClient.receiptsInfo(
                        inn, kkt.getRegNumber(), cursorFrom, windowTo);
                for (OfdReceiptDto r : receipts) {
                    if (ingestService.ingest(r, kkt)) {
                        inserted++;
                    }
                }
                cursorFrom = windowTo;
                state.setLastSuccessTo(windowTo);
                state.setLastError(null);
            }
            state.setInitialDone(true);
        } catch (Exception e) {
            state.setLastError(e.getMessage());
            throw e;
        } finally {
            syncStateRepository.save(state);
        }
        return inserted;
    }

    public record SyncStatus(boolean running, String message, Instant lastFinishedAt, String mode) {}
}

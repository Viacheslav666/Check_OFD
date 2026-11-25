package ru.ofd.agent.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.ofd.agent.collect.CollectorService;
import ru.ofd.agent.store.AgentStore;
import ru.ofd.marking.contract.AgentApi;
import ru.ofd.marking.contract.AgentKktDto;
import ru.ofd.marking.contract.AgentReceiptDto;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.contract.CollectResultDto;
import ru.ofd.marking.contract.ReceiptPageDto;

/** HTTP API агента для центрального приложения (контракт — {@link AgentApi}). */
@RestController
public class AgentController {

    private final CollectorService collector;
    private final AgentStore store;

    public AgentController(CollectorService collector, AgentStore store) {
        this.collector = collector;
        this.store = store;
    }

    @GetMapping(AgentApi.HEALTH)
    public Map<String, Object> health() {
        return Map.of("status", "UP", "api", AgentApi.VERSION, "version", CollectorService.VERSION);
    }

    @GetMapping(AgentApi.STATUS)
    public AgentStatusDto status() {
        return collector.status();
    }

    @GetMapping(AgentApi.KKTS)
    public List<AgentKktDto> kkts() {
        return store.kkts();
    }

    @GetMapping(AgentApi.RECEIPTS)
    public ReceiptPageDto receipts(@RequestParam(defaultValue = "0") long after,
                                   @RequestParam(defaultValue = "" + AgentApi.DEFAULT_PAGE_SIZE) int limit) {
        int size = Math.max(1, Math.min(limit, AgentApi.MAX_PAGE_SIZE));
        long maxSeq = store.maxSeq();
        // Берём на 1 больше, чтобы честно определить hasMore.
        List<AgentReceiptDto> rows = store.receiptsAfter(Math.max(0, after), size + 1);
        boolean hasMore = rows.size() > size;
        List<AgentReceiptDto> page = hasMore ? rows.subList(0, size) : rows;
        long next = page.isEmpty() ? Math.max(0, after) : page.get(page.size() - 1).seq();
        return new ReceiptPageDto(store.instanceId(), page, next, hasMore, maxSeq);
    }

    @PostMapping(AgentApi.COLLECT)
    public CollectResultDto collect() {
        boolean started = collector.startAsync();
        return new CollectResultDto(started, true, started ? "Сбор запущен" : "Сбор уже идёт");
    }
}

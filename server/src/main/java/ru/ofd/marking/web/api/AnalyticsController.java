package ru.ofd.marking.web.api;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ofd.marking.export.ExcelExportService;
import ru.ofd.marking.service.MetricsService;
import ru.ofd.marking.service.SettingsService;
import ru.ofd.marking.sync.CollectionService;
import ru.ofd.marking.web.dto.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class AnalyticsController {

    private final MetricsService metricsService;
    private final SettingsService settingsService;
    private final CollectionService collectionService;
    private final ExcelExportService excelExportService;

    public AnalyticsController(MetricsService metricsService,
                               SettingsService settingsService,
                               CollectionService collectionService,
                               ExcelExportService excelExportService) {
        this.metricsService = metricsService;
        this.settingsService = settingsService;
        this.collectionService = collectionService;
        this.excelExportService = excelExportService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "sync", collectionService.status());
    }

    @GetMapping("/trade-points")
    public List<TradePointStatsDto> tradePoints(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Period p = period(from, to);
        return metricsService.tradePointStats(p.from, p.to);
    }

    @GetMapping("/trade-points/worst")
    public List<TradePointStatsDto> worst(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "threshold", required = false) Double threshold) {
        Period p = period(from, to);
        return metricsService.worstTradePoints(p.from, p.to, threshold);
    }

    @GetMapping("/groups")
    public List<GroupStatsDto> groups(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Period p = period(from, to);
        return metricsService.groupStats(p.from, p.to);
    }

    @GetMapping("/matrix")
    public List<MatrixCellDto> matrix(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Period p = period(from, to);
        return metricsService.matrix(p.from, p.to);
    }

    @GetMapping("/receipts")
    public Page<ReceiptSummaryDto> receipts(
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "tradePointId", required = false) Long tradePointId,
            @RequestParam(value = "kktRegNumber", required = false) String kktRegNumber,
            @RequestParam(defaultValue = "false") boolean markedOnly,
            @RequestParam(value = "nameContains", required = false) String nameContains,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Period p = period(from, to);
        return metricsService.searchReceiptsFiltered(p.from, p.to, tradePointId, kktRegNumber,
                markedOnly, nameContains, PageRequest.of(page, size));
    }

    @GetMapping("/receipts/{id}")
    public ReceiptDetailDto receipt(@PathVariable Long id) {
        return metricsService.receiptDetail(id);
    }

    @GetMapping("/settings")
    public Map<String, String> settings() {
        return settingsService.all();
    }

    @PutMapping("/settings")
    public Map<String, String> updateSettings(@RequestBody Map<String, String> body) {
        return settingsService.update(body);
    }

    @GetMapping("/sync/status")
    public CollectionService.SyncStatus syncStatus() {
        return collectionService.status();
    }

    @PostMapping("/sync/trigger")
    public CollectionService.SyncStatus syncTrigger() {
        Thread t = new Thread(collectionService::runSync, "ofd-manual-sync");
        t.start();
        return collectionService.status();
    }

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(
            @PathVariable String type,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Period p = period(from, to);
        byte[] data = switch (type) {
            case "trade-points" -> excelExportService.exportTradePoints(p.from, p.to);
            case "groups" -> excelExportService.exportGroups(p.from, p.to);
            case "matrix" -> excelExportService.exportMatrix(p.from, p.to);
            default -> throw new IllegalArgumentException("Unknown export type: " + type);
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ofd-" + type + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private Period period(Instant from, Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(30, ChronoUnit.DAYS) : from;
        return new Period(start, end);
    }

    private record Period(Instant from, Instant to) {}
}

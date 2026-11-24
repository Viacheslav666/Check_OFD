package ru.ofd.marking.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ofd.marking.domain.ProductGroup;
import ru.ofd.marking.domain.entity.Receipt;
import ru.ofd.marking.domain.entity.TradePoint;
import ru.ofd.marking.domain.repo.ReceiptItemRepository;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.domain.repo.TradePointRepository;
import ru.ofd.marking.web.dto.GroupStatsDto;
import ru.ofd.marking.web.dto.MatrixCellDto;
import ru.ofd.marking.web.dto.ReceiptDetailDto;
import ru.ofd.marking.web.dto.ReceiptItemDto;
import ru.ofd.marking.web.dto.ReceiptSummaryDto;
import ru.ofd.marking.web.dto.TradePointStatsDto;

@Service
@Transactional(readOnly = true)
public class MetricsService {

    private final ReceiptRepository receiptRepository;
    private final ReceiptItemRepository itemRepository;
    private final TradePointRepository tradePointRepository;
    private final SettingsService settingsService;

    public MetricsService(ReceiptRepository receiptRepository,
                          ReceiptItemRepository itemRepository,
                          TradePointRepository tradePointRepository,
                          SettingsService settingsService) {
        this.receiptRepository = receiptRepository;
        this.itemRepository = itemRepository;
        this.tradePointRepository = tradePointRepository;
        this.settingsService = settingsService;
    }

    public List<TradePointStatsDto> tradePointStats(Instant from, Instant to) {
        Map<Long, TradePoint> tps = new HashMap<>();
        tradePointRepository.findAll().forEach(tp -> tps.put(tp.getId(), tp));
        List<TradePointStatsDto> result = new ArrayList<>();
        for (Object[] row : receiptRepository.aggregateByTradePoint(from, to)) {
            Long tpId = (Long) row[0];
            long total = ((Number) row[1]).longValue();
            long marked = ((Number) row[2]).longValue();
            TradePoint tp = tps.get(tpId);
            double pct = total == 0 ? 0 : (marked * 100.0 / total);
            result.add(new TradePointStatsDto(
                    tpId,
                    tp == null ? "?" : tp.getDisplayName(),
                    tp == null ? "" : tp.getAddress(),
                    tp == null ? "" : tp.getPlace(),
                    total, marked, pct));
        }
        result.sort(Comparator.comparing(TradePointStatsDto::markedPercent));
        return result;
    }

    public List<TradePointStatsDto> worstTradePoints(Instant from, Instant to, Double threshold) {
        double thr = threshold == null ? settingsService.worstThreshold() : threshold;
        return tradePointStats(from, to).stream()
                .filter(s -> s.markedPercent() < thr)
                .sorted(Comparator.comparing(TradePointStatsDto::markedPercent))
                .toList();
    }

    public List<GroupStatsDto> groupStats(Instant from, Instant to) {
        Map<ProductGroup, GroupStatsDto> map = new HashMap<>();
        for (ProductGroup g : ProductGroup.values()) {
            map.put(g, new GroupStatsDto(g.name(), g.getTitleRu(), 0, 0, 0));
        }
        for (Object[] row : itemRepository.aggregateByGroup(from, to)) {
            ProductGroup g = (ProductGroup) row[0];
            long total = ((Number) row[1]).longValue();
            long marked = ((Number) row[2]).longValue();
            double pct = total == 0 ? 0 : marked * 100.0 / total;
            map.put(g, new GroupStatsDto(g.name(), g.getTitleRu(), total, marked, pct));
        }
        return map.values().stream()
                .sorted(Comparator.comparing(GroupStatsDto::groupCode))
                .toList();
    }

    public List<MatrixCellDto> matrix(Instant from, Instant to) {
        Map<Long, String> names = new HashMap<>();
        tradePointRepository.findAll().forEach(tp -> names.put(tp.getId(), tp.getDisplayName()));
        List<MatrixCellDto> cells = new ArrayList<>();
        for (Object[] row : itemRepository.matrix(from, to)) {
            Long tpId = (Long) row[0];
            ProductGroup g = (ProductGroup) row[1];
            long total = ((Number) row[2]).longValue();
            long marked = ((Number) row[3]).longValue();
            double pct = total == 0 ? 0 : marked * 100.0 / total;
            cells.add(new MatrixCellDto(tpId, names.getOrDefault(tpId, "?"),
                    g.name(), g.getTitleRu(), total, marked, pct));
        }
        return cells;
    }

    public Page<ReceiptSummaryDto> searchReceipts(Instant from, Instant to, Long tpId, String kkt,
                                                  boolean markedOnly, String nameContains, Pageable pageable) {
        Page<Receipt> page = receiptRepository.search(from, to, tpId, blankToNull(kkt), markedOnly, pageable);
        return page.map(r -> {
            boolean nameOk = nameContains == null || nameContains.isBlank()
                    || r.getItems().stream().anyMatch(i -> i.getName() != null
                    && i.getName().toLowerCase().contains(nameContains.toLowerCase()));
            if (!nameOk) {
                return null;
            }
            return toSummary(r);
        }).map(dto -> dto); // keep type; nulls filtered below via custom — simpler filter in memory for name
    }

    public Page<ReceiptSummaryDto> searchReceiptsFiltered(Instant from, Instant to, Long tpId, String kkt,
                                                          boolean markedOnly, String nameContains, Pageable pageable) {
        // For name filter we load page then filter; for demo scale it's fine. Re-query without name first.
        Page<Receipt> page = receiptRepository.search(from, to, tpId, blankToNull(kkt), markedOnly, pageable);
        List<ReceiptSummaryDto> filtered = page.getContent().stream()
                .filter(r -> nameContains == null || nameContains.isBlank()
                        || r.getItems().stream().anyMatch(i -> i.getName() != null
                        && i.getName().toLowerCase().contains(nameContains.toLowerCase())))
                .map(this::toSummary)
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, pageable, page.getTotalElements());
    }

    public ReceiptDetailDto receiptDetail(Long id) {
        Receipt r = receiptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Чек не найден: " + id));
        List<ReceiptItemDto> items = r.getItems().stream()
                .map(i -> new ReceiptItemDto(i.getId(), i.getLineNo(), i.getName(),
                        i.getPriceKopecks(), i.getQuantity(), i.getTotalKopecks(),
                        i.getSubjectType(), i.getProductCodeJson(),
                        i.getProductGroup().name(), i.getProductGroup().getTitleRu(),
                        i.isMarked(), i.getNdsRate()))
                .toList();
        return new ReceiptDetailDto(
                r.getId(), r.getOfdRawId(), r.getDocTag(), r.isBso(), r.isCorrection(),
                r.getOperationType(), r.getUserInn(), r.getKktRegNumber(),
                r.getTradePoint().getId(), r.getTradePoint().getDisplayName(),
                r.getFnNumber(), r.getDocNumber(), r.getDocShiftNumber(), r.getReceiptNumber(),
                r.getDocDateTime(), r.getCDateUtc(), r.getTotalSumKopecks(),
                r.getFiscalSign(), r.isHasMarkedGoods(), items);
    }

    private ReceiptSummaryDto toSummary(Receipt r) {
        return new ReceiptSummaryDto(
                r.getId(), r.getOfdRawId(), r.getDocTag(), r.getOperationType(),
                r.getKktRegNumber(), r.getTradePoint().getId(), r.getTradePoint().getDisplayName(),
                r.getDocNumber(), r.getDocDateTime(), r.getTotalSumKopecks(), r.isHasMarkedGoods());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}

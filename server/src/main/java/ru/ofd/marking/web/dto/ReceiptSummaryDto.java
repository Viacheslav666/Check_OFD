package ru.ofd.marking.web.dto;
import java.time.Instant;
public record ReceiptSummaryDto(
        Long id, String ofdRawId, int docTag, String operationType,
        String kktRegNumber, Long tradePointId, String tradePointName,
        Long docNumber, Instant docDateTime, long totalSumKopecks, boolean hasMarkedGoods) {}

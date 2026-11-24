package ru.ofd.marking.web.dto;
import java.time.Instant;
import java.util.List;
public record ReceiptDetailDto(
        Long id, String ofdRawId, int docTag, boolean bso, boolean correction,
        String operationType, String userInn, String kktRegNumber,
        Long tradePointId, String tradePointName, String fnNumber,
        Long docNumber, Integer docShiftNumber, Integer receiptNumber,
        Instant docDateTime, Instant cDateUtc, long totalSumKopecks,
        String fiscalSign, boolean hasMarkedGoods, List<ReceiptItemDto> items) {}

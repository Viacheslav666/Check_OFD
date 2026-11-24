package ru.ofd.marking.web.dto;
import java.math.BigDecimal;
public record ReceiptItemDto(
        Long id, int lineNo, String name, Long priceKopecks, BigDecimal quantity,
        Long totalKopecks, Integer subjectType, String productCodeJson,
        String productGroup, String productGroupTitle, boolean marked, Integer ndsRate) {}

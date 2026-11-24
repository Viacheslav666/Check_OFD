package ru.ofd.marking.ofd.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.ofd.marking.domain.MarkingDetector;
import ru.ofd.marking.domain.ProductGroup;
import ru.ofd.marking.domain.entity.Receipt;
import ru.ofd.marking.domain.entity.ReceiptItem;
import ru.ofd.marking.domain.entity.TradePoint;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;
import ru.ofd.marking.service.NomenclatureClassifier;

/**
 * Единственный слой, где «сырые» поля ОФД становятся доменом.
 * Assumptions (Q1–Q4) не протекают наружу.
 */
@Component
public class OfdMapper {

    private static final DateTimeFormatter FLEX = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ObjectMapper objectMapper;
    private final NomenclatureClassifier classifier;

    public OfdMapper(ObjectMapper objectMapper, NomenclatureClassifier classifier) {
        this.objectMapper = objectMapper;
        this.classifier = classifier;
    }

    public Receipt toReceipt(OfdReceiptDto dto, TradePoint tradePoint) {
        Receipt r = new Receipt();
        String rawId = firstNonBlank(dto.getDocRawId(), dto.getId());
        r.setOfdRawId(rawId);
        r.setDocTag(dto.getTag() == null ? 3 : dto.getTag());
        r.setBso(Boolean.TRUE.equals(dto.getIsBso()));
        r.setCorrection(Boolean.TRUE.equals(dto.getIsCorrection()));
        r.setOperationType(dto.getOperationType());
        r.setUserInn(trimInn(dto.getUserInn()));
        r.setKktRegNumber(dto.getKktRegNumber());
        r.setTradePoint(tradePoint);
        r.setFnNumber(dto.getFnNumber());
        r.setDocNumber(dto.getDocNumber());
        r.setDocShiftNumber(dto.getDocShiftNumber());
        r.setReceiptNumber(dto.getReceiptNumber());
        r.setDocDateTime(parseInstant(dto.getDocDateTime()));
        r.setCDateUtc(parseInstant(dto.getCDateUtc()));
        r.setTotalSumKopecks(dto.getTotalSumm() == null ? 0L : dto.getTotalSumm());
        r.setFiscalSign(dto.getDecimalFiscalSign());

        boolean anyMarked = false;
        if (dto.getItems() != null) {
            int line = 1;
            for (OfdReceiptDto.OfdItemDto item : dto.getItems()) {
                ReceiptItem ri = toItem(item, line++);
                r.addItem(ri);
                if (ri.isMarked()) {
                    anyMarked = true;
                }
            }
        }
        r.setHasMarkedGoods(anyMarked);
        return r;
    }

    private ReceiptItem toItem(OfdReceiptDto.OfdItemDto item, int line) {
        ReceiptItem ri = new ReceiptItem();
        ri.setLineNo(line);
        ri.setName(item.getName() == null ? "" : item.getName());
        ri.setPriceKopecks(item.getPrice());
        ri.setQuantity(item.getQuantity() == null ? null : BigDecimal.valueOf(item.getQuantity()));
        ri.setTotalKopecks(item.getTotal());
        ri.setSubjectType(item.getSubjectType());
        ri.setNdsRate(item.getNDS_Rate());
        String pcJson = productCodeJson(item.getProductCode());
        ri.setProductCodeJson(pcJson);
        ri.setMarked(MarkingDetector.isMarked(item.getSubjectType(), pcJson));
        ProductGroup group = classifier.classify(ri.getName());
        ri.setProductGroup(group);
        return ri;
    }

    private String productCodeJson(Map<String, Object> productCode) {
        if (productCode == null || productCode.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(productCode);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value.endsWith("Z") ? value : value + "Z");
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, FLEX);
            return ldt.atZone(ZoneId.of("Europe/Moscow")).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a.trim();
        if (b != null && !b.isBlank()) return b.trim();
        throw new IllegalArgumentException("Receipt without Id/DocRawId");
    }

    private static String trimInn(String inn) {
        if (inn == null) return null;
        return inn.trim();
    }
}

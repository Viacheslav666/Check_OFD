package ru.ofd.marking.ofd.mapping;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ofd.marking.domain.ProductGroup;
import ru.ofd.marking.domain.entity.Receipt;
import ru.ofd.marking.domain.entity.TradePoint;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;
import ru.ofd.marking.service.NomenclatureClassifier;

@ExtendWith(MockitoExtension.class)
class OfdMapperTest {

    @Mock NomenclatureClassifier classifier;
    OfdMapper mapper;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(classifier.classify(anyString())).thenReturn(ProductGroup.OTHER);
        mapper = new OfdMapper(new ObjectMapper(), classifier);
    }

    @Test
    void mapsMarkedReceiptFromFfd12Sample() {
        OfdReceiptDto dto = new OfdReceiptDto();
        dto.setId("ee19889a-11b4-428d-83be-3dc9065dc28c");
        dto.setTag(3);
        dto.setDocDateTime("2022-10-01T11:16:00");
        dto.setCDateUtc("2022-10-01T08:16:08");
        dto.setKktRegNumber("0005956962031875");
        dto.setTotalSumm(923900L);
        OfdReceiptDto.OfdItemDto marked = new OfdReceiptDto.OfdItemDto();
        marked.setName("ботинки");
        marked.setSubjectType(33);
        marked.setProductCode(Map.of("Code_GS_1M", "0104660184311215215osnRMP5"));
        marked.setPrice(799000L);
        marked.setQuantity(1.0);
        marked.setTotal(799000L);
        OfdReceiptDto.OfdItemDto plain = new OfdReceiptDto.OfdItemDto();
        plain.setName("носки");
        plain.setSubjectType(1);
        plain.setProductCode(Map.of());
        dto.setItems(List.of(marked, plain));

        TradePoint tp = new TradePoint();
        tp.setId(1L);
        Receipt r = mapper.toReceipt(dto, tp);

        assertTrue(r.isHasMarkedGoods());
        assertEquals(2, r.getItems().size());
        assertTrue(r.getItems().get(0).isMarked());
        assertFalse(r.getItems().get(1).isMarked());
    }

    @Test
    void accepts12DigitInn() {
        OfdReceiptDto dto = new OfdReceiptDto();
        dto.setId("x");
        dto.setUserInn("123456789012");
        dto.setDocDateTime("2026-01-01T10:00:00");
        dto.setItems(List.of());
        Receipt r = mapper.toReceipt(dto, new TradePoint());
        assertEquals("123456789012", r.getUserInn());
    }
}

package ru.ofd.marking.ofd.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

/**
 * Demo/mock mode: realistic ФФД 1.2 receipts with and without marking, no real API key.
 */
@Component
@ConditionalOnProperty(name = "ofd.mode", havingValue = "mock", matchIfMissing = true)
public class MockOfdClient implements OfdApiClient {

    private final ObjectMapper mapper;
    private final List<OfdReceiptDto> templates;

    public MockOfdClient(ObjectMapper mapper) throws Exception {
        this.mapper = mapper;
        try (InputStream in = new ClassPathResource("mock/sample-receipts.json").getInputStream()) {
            this.templates = mapper.readValue(in, new TypeReference<>() {});
        }
    }

    @Override
    public List<OfdKktDto> listKkts(String inn) {
        List<OfdKktDto> list = new ArrayList<>();
        list.add(kkt("0001000100010001", "Касса ТТ-1", "г. Москва, ул. Ленина, 1", "Магазин №1"));
        list.add(kkt("0001000100010002", "Касса ТТ-1б", "г. Москва, ул. Ленина, 1", "Магазин №1"));
        list.add(kkt("0002000200020002", "Касса ТТ-2", "г. Москва, пр. Мира, 10", "Магазин №2"));
        list.add(kkt("0003000300030003", "Касса ТТ-3", "г. Химки, ул. Юности, 5", "Павильон"));
        return list;
    }

    @Override
    public List<OfdReceiptDto> receiptsInfo(String inn, String rnm, Instant from, Instant to) {
        List<OfdReceiptDto> out = new ArrayList<>();
        Instant cursor = from;
        int i = 0;
        while (cursor.isBefore(to) && i < 40) {
            OfdReceiptDto tpl = templates.get(i % templates.size());
            OfdReceiptDto copy = mapper.convertValue(tpl, OfdReceiptDto.class);
            String id = UUID.nameUUIDFromBytes((rnm + "|" + cursor + "|" + i).getBytes()).toString();
            copy.setId(id);
            copy.setDocRawId(id);
            copy.setKktRegNumber(rnm);
            copy.setDocDateTime(cursor.toString().replace("Z", "").substring(0, 19));
            copy.setCDateUtc(cursor.minus(3, ChronoUnit.HOURS).toString().replace("Z", "").substring(0, 19));
            copy.setDocNumber((long) (1000 + i));
            out.add(copy);
            cursor = cursor.plus(6, ChronoUnit.HOURS);
            i++;
        }
        return out;
    }

    private static OfdKktDto kkt(String rnm, String name, String address, String place) {
        OfdKktDto d = new OfdKktDto();
        d.setId(UUID.nameUUIDFromBytes(rnm.getBytes()).toString());
        d.setKktRegId(rnm);
        d.setKktName(name);
        d.setSerialNumber("SN-" + rnm.substring(0, 8));
        d.setFnNumber("9960" + rnm.substring(0, 12));
        d.setFiscalAddress(address);
        d.setFiscalPlace(place);
        d.setPath("/Мои кассы/");
        return d;
    }
}

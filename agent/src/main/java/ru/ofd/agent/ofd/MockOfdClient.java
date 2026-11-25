package ru.ofd.agent.ofd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 * Демо-режим (OFD_MODE=mock): реалистичные чеки ФФД 1.2 с маркировкой и без, без ключа ОФД.
 * Набор ККТ и ТТ зависит от ИНН, поэтому два демо-агента с разными ИНН дают разные точки.
 * Чеки детерминированы (слоты по 4 часа), повторный запрос того же периода даёт те же чеки.
 */
@Component
@ConditionalOnProperty(name = "ofd.mode", havingValue = "mock", matchIfMissing = true)
public class MockOfdClient implements OfdApiClient {

    private static final long SLOT_HOURS = 4;
    /** Доля «маркированных» слотов по ККТ (в %): у последней точки доля ниже порога 25%. */
    private static final int[] MARKED_SHARE = {70, 55, 40, 15};
    private static final int[] MARKED_TEMPLATES = {0, 2, 3, 5};
    private static final int[] PLAIN_TEMPLATES = {1, 4};

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
        String p = prefix(inn);
        String city = cityFor(inn);
        List<OfdKktDto> list = new ArrayList<>();
        list.add(kkt(p + "0001", "Касса 1", city + ", ул. Ленина, 1", "Магазин «Центральный»"));
        list.add(kkt(p + "0002", "Касса 2", city + ", ул. Ленина, 1", "Магазин «Центральный»"));
        list.add(kkt(p + "0003", "Касса 1", city + ", пр. Мира, 10", "Магазин «Мира»"));
        list.add(kkt(p + "0004", "Касса 1", city + ", ул. Юности, 5", "Павильон «Юность»"));
        return list;
    }

    @Override
    public List<OfdReceiptDto> receiptsInfo(String inn, String rnm, Instant from, Instant to) {
        List<OfdReceiptDto> out = new ArrayList<>();
        int kktIdx = Math.floorMod(Integer.parseInt(rnm.substring(rnm.length() - 1)) - 1, MARKED_SHARE.length);
        long slotMs = SLOT_HOURS * 3600_000L;
        long first = Math.floorDiv(from.toEpochMilli() + slotMs - 1, slotMs);
        for (long slot = first; slot * slotMs < to.toEpochMilli(); slot++) {
            Instant at = Instant.ofEpochMilli(slot * slotMs);
            int h = Math.floorMod((int) (slot * 2654435761L ^ rnm.hashCode()), 100);
            boolean marked = h < MARKED_SHARE[kktIdx];
            int[] pool = marked ? MARKED_TEMPLATES : PLAIN_TEMPLATES;
            OfdReceiptDto copy = mapper.convertValue(templates.get(pool[Math.floorMod(h, pool.length)]), OfdReceiptDto.class);
            String id = UUID.nameUUIDFromBytes((inn + "|" + rnm + "|" + slot).getBytes()).toString();
            copy.setId(id);
            copy.setDocRawId(id);
            copy.setUserInn(inn);
            copy.setKktRegNumber(rnm);
            copy.setFnNumber(fn(rnm));
            LocalDateTime local = LocalDateTime.ofInstant(at, ZoneId.of("Europe/Moscow"));
            copy.setDocDateTime(local.toString().length() == 16 ? local + ":00" : local.toString().substring(0, 19));
            copy.setCDateUtc(at.truncatedTo(ChronoUnit.SECONDS).toString().replace("Z", ""));
            copy.setDocNumber(slot % 1_000_000);
            out.add(copy);
        }
        return out;
    }

    private static String prefix(String inn) {
        String digits = inn == null ? "000000" : inn.replaceAll("\\D", "");
        String tail = digits.length() >= 6 ? digits.substring(digits.length() - 6) : String.format("%6s", digits).replace(' ', '0');
        return "000" + tail + "000";
    }

    private static String cityFor(String inn) {
        String[] cities = {"г. Самара", "г. Тольятти", "г. Сызрань", "г. Новокуйбышевск"};
        return cities[Math.floorMod(inn == null ? 0 : inn.hashCode(), cities.length)];
    }

    private static String fn(String rnm) {
        return "9960" + rnm.substring(rnm.length() - 12);
    }

    private static OfdKktDto kkt(String rnm, String name, String address, String place) {
        OfdKktDto d = new OfdKktDto();
        d.setId(UUID.nameUUIDFromBytes(rnm.getBytes()).toString());
        d.setKktRegId(rnm);
        d.setKktName(name);
        d.setSerialNumber("SN-" + rnm.substring(rnm.length() - 8));
        d.setFnNumber(fn(rnm));
        d.setFiscalAddress(address);
        d.setFiscalPlace(place);
        d.setPath("/Мои кассы/");
        return d;
    }
}

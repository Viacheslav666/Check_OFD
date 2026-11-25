package ru.ofd.agent.ofd;

import java.time.Instant;
import java.util.List;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

public interface OfdApiClient {
    List<OfdKktDto> listKkts(String inn);
    List<OfdReceiptDto> receiptsInfo(String inn, String rnm, Instant from, Instant to);
}

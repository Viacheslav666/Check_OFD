package ru.ofd.marking.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ofd.marking.domain.TradePointKey;
import ru.ofd.marking.domain.entity.Kkt;
import ru.ofd.marking.domain.entity.TradePoint;
import ru.ofd.marking.domain.repo.KktRepository;
import ru.ofd.marking.domain.repo.TradePointRepository;
import ru.ofd.marking.ofd.dto.OfdKktDto;

@Service
public class TradePointService {

    private final TradePointRepository tradePointRepository;
    private final KktRepository kktRepository;

    public TradePointService(TradePointRepository tradePointRepository, KktRepository kktRepository) {
        this.tradePointRepository = tradePointRepository;
        this.kktRepository = kktRepository;
    }

    @Transactional
    public Kkt upsertKkt(OfdKktDto dto) {
        String address = dto.getFiscalAddress() == null ? "" : dto.getFiscalAddress();
        String place = dto.getFiscalPlace() == null ? "" : dto.getFiscalPlace();
        String hash = TradePointKey.hash(address, place);
        TradePoint tp = tradePointRepository.findByKeyHash(hash).orElseGet(() -> {
            TradePoint n = new TradePoint();
            n.setKeyHash(hash);
            n.setAddress(address);
            n.setPlace(place);
            n.setDisplayName(TradePointKey.displayName(address, place));
            return tradePointRepository.save(n);
        });

        Kkt kkt = kktRepository.findByRegNumber(dto.getKktRegId()).orElseGet(Kkt::new);
        kkt.setOfdId(dto.getId());
        kkt.setRegNumber(dto.getKktRegId());
        kkt.setName(dto.getKktName());
        kkt.setSerialNumber(dto.getSerialNumber());
        kkt.setFnNumber(dto.getFnNumber());
        kkt.setLkPath(dto.getPath());
        kkt.setTradePoint(tp);
        kkt.setLastSeenAt(Instant.now());
        return kktRepository.save(kkt);
    }
}

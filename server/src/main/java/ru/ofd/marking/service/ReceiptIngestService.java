package ru.ofd.marking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ofd.marking.domain.entity.Kkt;
import ru.ofd.marking.domain.entity.Receipt;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;
import ru.ofd.marking.ofd.mapping.OfdMapper;

@Service
public class ReceiptIngestService {

    private final ReceiptRepository receiptRepository;
    private final OfdMapper mapper;

    public ReceiptIngestService(ReceiptRepository receiptRepository, OfdMapper mapper) {
        this.receiptRepository = receiptRepository;
        this.mapper = mapper;
    }

    @Transactional
    public boolean ingest(OfdReceiptDto dto, Kkt kkt) {
        String rawId = dto.getDocRawId() != null ? dto.getDocRawId() : dto.getId();
        if (rawId == null || receiptRepository.existsByOfdRawId(rawId)) {
            return false;
        }
        Receipt receipt = mapper.toReceipt(dto, kkt.getTradePoint());
        if (receipt.getKktRegNumber() == null) {
            receipt.setKktRegNumber(kkt.getRegNumber());
        }
        receiptRepository.save(receipt);
        return true;
    }
}

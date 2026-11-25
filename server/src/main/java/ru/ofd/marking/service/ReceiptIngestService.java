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
    public boolean ingest(OfdReceiptDto dto, Kkt kkt, Long agentId) {
        String rawId = dto.getDocRawId() != null && !dto.getDocRawId().isBlank() ? dto.getDocRawId() : dto.getId();
        if (rawId == null || receiptRepository.existsByOfdRawId(rawId)) {
            return false;
        }
        // Один и тот же фискальный документ мог прийти от двух агентов с разным Id в ОФД
        if (dto.getFnNumber() != null && dto.getDocNumber() != null
                && receiptRepository.existsByFnNumberAndDocNumber(dto.getFnNumber(), dto.getDocNumber())) {
            return false;
        }
        Receipt receipt = mapper.toReceipt(dto, kkt.getTradePoint());
        if (receipt.getKktRegNumber() == null) {
            receipt.setKktRegNumber(kkt.getRegNumber());
        }
        if (receipt.getUserInn() == null) {
            receipt.setUserInn(kkt.getInn());
        }
        receipt.setAgentId(agentId);
        receiptRepository.save(receipt);
        return true;
    }
}

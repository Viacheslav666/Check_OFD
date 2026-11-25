package ru.ofd.marking.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

/**
 * Чек, сохранённый агентом.
 *
 * @param seq монотонный номер записи в БД агента — курсор инкрементальной синхронизации
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentReceiptDto(long seq, String inn, String kktRegNumber, Instant collectedAt, OfdReceiptDto receipt) {
}

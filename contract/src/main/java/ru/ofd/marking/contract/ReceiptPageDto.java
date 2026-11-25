package ru.ofd.marking.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Страница чеков после курсора.
 *
 * @param nextCursor значение {@code after} для следующего запроса (seq последнего чека страницы)
 * @param hasMore    есть ли ещё чеки после {@code nextCursor}
 * @param maxSeq     последний seq в БД агента (для расчёта отставания центра)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReceiptPageDto(String instanceId, List<AgentReceiptDto> items, long nextCursor, boolean hasMore, long maxSeq) {
}

package ru.ofd.marking.contract;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.List;

/**
 * Состояние агента. Ключ API ОФД в ответ никогда не попадает (только признак {@code apiKeyConfigured}).
 *
 * @param instanceId уникальный ID локальной БД агента; меняется, если БД агента пересоздана —
 *                   центр тогда сбрасывает курсор и перечитывает чеки (дубли отсекаются).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentStatusDto(
        String agentName,
        String instanceId,
        String version,
        String mode,
        boolean apiKeyConfigured,
        List<String> inns,
        int kktCount,
        long receiptsStored,
        long maxSeq,
        boolean collecting,
        Instant lastCollectStartedAt,
        Instant lastCollectFinishedAt,
        Instant lastCollectSuccessAt,
        String lastCollectError,
        long lastCollectInserted,
        Instant collectedUpTo,
        Instant serverTime) {
}

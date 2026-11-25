package ru.ofd.marking.web.dto;

import java.time.Instant;
import java.util.List;

/**
 * Карточка агента для ЛК и страницы статусов. Токен не возвращается — только признак и 4 последних символа.
 *
 * @param state ONLINE | OFFLINE | ERROR | DISABLED | NEW
 * @param pendingOnAgent сколько записей на агенте ещё не забрано центром (agentMaxSeq − lastCursor)
 * @param lagMinutes на сколько минут данные агента отстают от текущего времени (по collectedUpTo агента)
 */
public record AgentView(
        Long id, String name, String host, int port, boolean useTls, boolean enabled, String comment,
        boolean tokenSet, String tokenHint, String baseUrl,
        String state, boolean online,
        Instant lastPollAt, Instant lastSuccessAt, String lastError, int consecutiveFailures,
        long lastCursor, Long agentMaxSeq, Long pendingOnAgent, long receiptsReceived,
        long receiptsInCentral, long kktsInCentral,
        String agentVersion, String agentMode, List<String> inns, Boolean agentCollecting,
        Instant agentLastCollectSuccessAt, String agentLastCollectError, Instant collectedUpTo, Long lagMinutes) {
}

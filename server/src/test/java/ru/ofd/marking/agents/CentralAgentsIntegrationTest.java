package ru.ofd.marking.agents;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import ru.ofd.marking.IntegrationTestBase;
import ru.ofd.marking.TestAgents;
import ru.ofd.marking.domain.repo.AgentRepository;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.web.dto.AgentRequest;
import ru.ofd.marking.web.dto.AgentView;
import ru.ofd.marking.web.dto.TradePointStatsDto;

/** Центр ↔ настоящие mock-агенты по HTTP: агрегация, курсор, дедупликация, IP и токен. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CentralAgentsIntegrationTest extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;
    @Autowired AgentSyncService sync;
    @Autowired AgentRepository agentRepository;
    @Autowired ReceiptRepository receiptRepository;

    TestAgents.Running a1;
    TestAgents.Running a2;
    TestAgents.Running blocked;

    @BeforeAll
    void startAgents() {
        a1 = TestAgents.get("samara", "635001808204", "127.0.0.1,::1");
        a2 = TestAgents.get("tolyatti", "632100000017", "127.0.0.1,::1");
        // агент, у которого в белом списке только чужой IP
        blocked = TestAgents.get("blocked", "631000000005", "10.9.9.9");
    }

    private AgentView create(String name, int port, String token) {
        ResponseEntity<AgentView> r = rest.postForEntity("/api/admin/agents",
                new AgentRequest(name, "127.0.0.1", port, false, token, true, null), AgentView.class);
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        return r.getBody();
    }

    private List<AgentView> agents() {
        return rest.exchange("/api/agents", HttpMethod.GET, null, new ParameterizedTypeReference<List<AgentView>>() {})
                .getBody();
    }

    private AgentView agent(String name) {
        return agents().stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow();
    }

    private long agentReceipts(TestAgents.Running r) {
        return r.collector().status().receiptsStored();
    }

    @Test
    @Order(1)
    void registerAgentsValidationAndTokenSecrecy() {
        create("РДП Самара", a1.port(), a1.token());
        create("РДП Тольятти", a2.port(), a2.token());
        create("Неверный токен", a2.port(), "wrong-token-0000000000");
        create("Чужой IP", blocked.port(), blocked.token());

        ResponseEntity<String> noToken = rest.postForEntity("/api/admin/agents",
                new AgentRequest("Без токена", "127.0.0.1", 1, false, "", true, null), String.class);
        assertThat(noToken.getStatusCode().value()).isEqualTo(400);
        ResponseEntity<String> badHost = rest.postForEntity("/api/admin/agents",
                new AgentRequest("Плохой хост", "http://x y", 1, false, "0123456789abcdef0", true, null), String.class);
        assertThat(badHost.getStatusCode().value()).isEqualTo(400);

        String json = rest.getForObject("/api/agents", String.class);
        assertThat(json).doesNotContain(a1.token()).doesNotContain(a2.token()).contains("tokenHint");
        assertThat(agents()).hasSize(4).allMatch(a -> "NEW".equals(a.state()));
    }

    @Test
    @Order(2)
    void testConnectionReportsIpAndTokenProblems() {
        Map<?, ?> ok = rest.postForObject("/api/admin/agents/" + agent("РДП Самара").id() + "/test", null, Map.class);
        assertThat(ok.get("ok")).isEqualTo(true);
        Map<?, ?> badToken = rest.postForObject("/api/admin/agents/" + agent("Неверный токен").id() + "/test", null, Map.class);
        assertThat(badToken.get("reachable")).isEqualTo(true);
        assertThat(badToken.get("ok")).isEqualTo(false);
        assertThat(String.valueOf(badToken.get("error"))).contains("401");
        Map<?, ?> badIp = rest.postForObject("/api/admin/agents/" + agent("Чужой IP").id() + "/test", null, Map.class);
        assertThat(badIp.get("reachable")).isEqualTo(false);
        assertThat(String.valueOf(badIp.get("error"))).contains("403");
    }

    @Test
    @Order(3)
    void syncAggregatesAllAgents() {
        AgentSyncService.SyncStatus st = sync.runSync(true);
        assertThat(st.message()).contains("на связи 2/4");

        long expected = agentReceipts(a1) + agentReceipts(a2);
        assertThat(receiptRepository.count()).isEqualTo(expected);

        AgentView samara = agent("РДП Самара");
        assertThat(samara.state()).isEqualTo("ONLINE");
        assertThat(samara.lastCursor()).isEqualTo(a1.collector().status().maxSeq());
        assertThat(samara.pendingOnAgent()).isZero();
        assertThat(samara.kktsInCentral()).isEqualTo(4);
        assertThat(samara.inns()).containsExactly("635001808204");
        assertThat(samara.receiptsInCentral()).isEqualTo(agentReceipts(a1));
        assertThat(agent("Неверный токен").state()).isEqualTo("ERROR");
        assertThat(agent("Неверный токен").lastError()).contains("401");
        assertThat(agent("Чужой IP").lastError()).contains("403");

        Instant to = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant from = to.minus(120, ChronoUnit.DAYS);
        List<TradePointStatsDto> tps = rest.exchange("/api/trade-points?from={f}&to={t}", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TradePointStatsDto>>() {}, from, to).getBody();
        assertThat(tps).hasSizeGreaterThanOrEqualTo(3);
        assertThat(tps.stream().mapToLong(TradePointStatsDto::totalReceipts).sum()).isEqualTo(expected);
        assertThat(tps).allSatisfy(t -> assertThat(t.markedPercent()).isBetween(0.0, 100.0));

        List<TradePointStatsDto> worst = rest.exchange("/api/trade-points/worst?from={f}&to={t}&threshold=25",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<TradePointStatsDto>>() {}, from, to).getBody();
        assertThat(worst).isNotEmpty();
        assertThat(worst).allSatisfy(t -> assertThat(t.markedPercent()).isLessThan(25.0));
        assertThat(worst).anySatisfy(t -> assertThat(t.displayName()).contains("Юность"));
    }

    @Test
    @Order(4)
    void secondSyncIsIncremental() {
        long before = receiptRepository.count();
        long cursorBefore = agent("РДП Самара").lastCursor();
        AgentSyncService.SyncStatus st = sync.runSync(false);
        assertThat(st.message()).contains("новых чеков 0");
        assertThat(receiptRepository.count()).isEqualTo(before);
        assertThat(agent("РДП Самара").lastCursor()).isEqualTo(cursorBefore);
    }

    @Test
    @Order(5)
    void resetCursorAndDuplicateAgentDoNotCreateDuplicates() {
        long before = receiptRepository.count();
        rest.postForObject("/api/admin/agents/" + agent("РДП Самара").id() + "/reset-cursor", null, AgentView.class);
        assertThat(agent("РДП Самара").lastCursor()).isZero();
        // тот же агент, зарегистрированный второй раз под другим именем
        create("Самара-дубль", a1.port(), a1.token());
        sync.runSync(false);
        assertThat(receiptRepository.count()).isEqualTo(before);
        assertThat(agent("Самара-дубль").state()).isEqualTo("ONLINE");
        assertThat(agent("РДП Самара").lastCursor()).isEqualTo(a1.collector().status().maxSeq());
    }

    @Test
    @Order(6)
    void disabledAndDeletedAgentsAreNotPolled() {
        AgentView dup = agent("Самара-дубль");
        rest.delete("/api/admin/agents/" + dup.id());
        AgentView bad = agent("Неверный токен");
        rest.put("/api/admin/agents/" + bad.id(), new AgentRequest(null, null, null, null, null, false, null));
        rest.delete("/api/admin/agents/" + agent("Чужой IP").id());
        AgentSyncService.SyncStatus st = sync.runSync(false);
        assertThat(st.message()).startsWith("OK").contains("2/2");
        assertThat(agent("Неверный токен").state()).isEqualTo("DISABLED");
        assertThat(agentRepository.count()).isEqualTo(3);
    }
}

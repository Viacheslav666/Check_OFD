package ru.ofd.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import ru.ofd.agent.collect.CollectorService;
import ru.ofd.marking.contract.AgentApi;
import ru.ofd.marking.contract.AgentKktDto;
import ru.ofd.marking.contract.AgentReceiptDto;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.contract.ReceiptPageDto;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.config.name=agent",
        "spring.datasource.url=jdbc:h2:mem:agent-it;DB_CLOSE_DELAY=-1",
        "agent.data-dir=target/agent-it",
        "agent.name=Тестовый агент",
        "agent.token=test-token-0123456789",
        "agent.allowed-ips=127.0.0.1,::1",
        "ofd.mode=mock",
        "ofd.inn=635001808204,6316000000",
        "ofd.sync-on-startup=false",
        "ofd.scheduler.enabled=false"
})
class AgentApiTest {

    static final String TOKEN = "test-token-0123456789";

    @Autowired TestRestTemplate rest;
    @Autowired CollectorService collector;

    private <T> ResponseEntity<T> get(String url, String token, Class<T> type) {
        HttpHeaders h = new HttpHeaders();
        if (token != null) h.set(AgentApi.TOKEN_HEADER, token);
        return rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), type);
    }

    @Test
    void tokenIsRequiredAndKeyNeverExposed() {
        assertThat(get(AgentApi.HEALTH, null, String.class).getStatusCode().value()).isEqualTo(200);
        assertThat(get(AgentApi.STATUS, null, String.class).getStatusCode().value()).isEqualTo(401);
        assertThat(get(AgentApi.STATUS, "bad-token-xxxxxxxxxx", String.class).getStatusCode().value()).isEqualTo(401);
        ResponseEntity<String> ok = get(AgentApi.STATUS, TOKEN, String.class);
        assertThat(ok.getStatusCode().value()).isEqualTo(200);
        assertThat(ok.getBody()).doesNotContainIgnoringCase("apiKey\":\"").contains("apiKeyConfigured");
    }

    @Test
    void collectsAndServesIncrementally() {
        collector.collect();
        AgentStatusDto st = get(AgentApi.STATUS, TOKEN, AgentStatusDto.class).getBody();
        assertThat(st).isNotNull();
        assertThat(st.inns()).containsExactly("635001808204", "6316000000");
        assertThat(st.kktCount()).isEqualTo(8);
        assertThat(st.receiptsStored()).isGreaterThan(1000);
        assertThat(st.lastCollectError()).isNull();

        AgentKktDto[] kkts = get(AgentApi.KKTS, TOKEN, AgentKktDto[].class).getBody();
        assertThat(kkts).hasSize(8);

        // Повторный сбор того же периода не создаёт дублей
        long before = st.receiptsStored();
        collector.collect();
        AgentStatusDto st2 = get(AgentApi.STATUS, TOKEN, AgentStatusDto.class).getBody();
        assertThat(st2.receiptsStored()).isEqualTo(before);
        assertThat(st2.lastCollectInserted()).isZero();

        // Постраничная выдача по курсору
        long after = 0;
        int pages = 0;
        Set<String> ids = new HashSet<>();
        while (true) {
            ReceiptPageDto page = get(AgentApi.RECEIPTS + "?after=" + after + "&limit=400", TOKEN, ReceiptPageDto.class).getBody();
            assertThat(page.instanceId()).isEqualTo(st.instanceId());
            for (AgentReceiptDto r : page.items()) {
                assertThat(r.seq()).isGreaterThan(after);
                assertThat(r.receipt().getItems()).isNotEmpty();
                ids.add(r.receipt().getDocRawId());
            }
            pages++;
            after = page.nextCursor();
            if (!page.hasMore()) break;
        }
        assertThat(pages).isGreaterThan(1);
        assertThat(ids).hasSize((int) before);
        assertThat(after).isEqualTo(st.maxSeq());

        ReceiptPageDto empty = get(AgentApi.RECEIPTS + "?after=" + after, TOKEN, ReceiptPageDto.class).getBody();
        assertThat(empty.items()).isEmpty();
        assertThat(empty.hasMore()).isFalse();
        assertThat(empty.nextCursor()).isEqualTo(after);
    }
}

package ru.ofd.marking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.ofd.marking.agents.AgentAdminService;
import ru.ofd.marking.agents.AgentSyncService;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.web.dto.AgentRequest;

/** Боевая СУБД: миграции V1+V2 на PostgreSQL 16 и опрос агента. Пропускается, если Docker недоступен. */
@SpringBootTest(properties = {"central.poll-enabled=false", "central.poll-on-startup=false"})
@Testcontainers(disabledWithoutDocker = true)
class PostgresMigrationTest {

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ofd_marking").withUsername("ofd").withPassword("ofd");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired AgentAdminService admin;
    @Autowired AgentSyncService sync;
    @Autowired ReceiptRepository receipts;

    @Test
    void migratesAndIngestsFromAgent() {
        TestAgents.Running a = TestAgents.get("pg-agent", "631100000002", "127.0.0.1,::1");
        admin.create(new AgentRequest("pg", "127.0.0.1", a.port(), false, a.token(), true, null));
        AgentSyncService.SyncStatus st = sync.runSync(false);
        assertThat(st.message()).startsWith("OK");
        assertThat(receipts.count()).isEqualTo(a.collector().status().receiptsStored());
    }
}

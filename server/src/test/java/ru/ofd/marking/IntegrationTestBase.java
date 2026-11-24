package ru.ofd.marking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class IntegrationTestBase {

    static {
        if (System.getProperty("docker.host") == null && System.getenv("DOCKER_HOST") == null) {
            System.setProperty("docker.host", "unix:///var/run/docker.sock");
        }
        // Docker Engine 29+ requires API >= 1.44; docker-java defaults are too old.
        System.setProperty("api.version", "1.44");
        System.setProperty("testcontainers.ryuk.disabled", "true");
    }

    @Container
    static PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ofd_marking")
            .withUsername("ofd")
            .withPassword("ofd");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("ofd.mode", () -> "mock");
        registry.add("ofd.sync-on-startup", () -> "false");
        registry.add("ofd.scheduler.enabled", () -> "false");
        registry.add("ofd.inn", () -> "7707083893");
    }
}

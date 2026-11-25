package ru.ofd.marking;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/** Центр на H2 (режим PostgreSQL) — те же миграции Flyway, что и в бою. Postgres проверяется в PostgresMigrationTest. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:central;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "central.poll-enabled=false",
        "central.poll-on-startup=false",
        "central.collect-wait-seconds=20"
})
public abstract class IntegrationTestBase {
}

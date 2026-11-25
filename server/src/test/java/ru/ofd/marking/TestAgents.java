package ru.ofd.marking;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import ru.ofd.agent.AgentApplication;
import ru.ofd.agent.collect.CollectorService;

/**
 * Настоящие агенты (модуль agent, режим mock) внутри JVM теста — центр ходит к ним по HTTP.
 * Поднимаются один раз на весь прогон.
 */
public final class TestAgents {

    public record Running(String name, int port, String token, String inn, ConfigurableApplicationContext ctx) {
        public CollectorService collector() {
            return ctx.getBean(CollectorService.class);
        }
    }

    private static final Map<String, Running> AGENTS = new LinkedHashMap<>();

    private TestAgents() {
    }

    public static synchronized Running get(String name, String inn, String allowedIps) {
        return AGENTS.computeIfAbsent(name, n -> start(n, inn, allowedIps));
    }

    private static Running start(String name, String inn, String allowedIps) {
        String token = "token-" + name + "-0123456789abcdef";
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(AgentApplication.class)
                .properties(AgentApplication.CONFIG_NAME)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1",
                        "--spring.autoconfigure.exclude="
                                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
                        "--logging.file.name=",
                        "--agent.name=" + name,
                        "--agent.token=" + token,
                        "--agent.allowed-ips=" + allowedIps,
                        "--agent.data-dir=target/test-agents/" + name,
                        "--ofd.mode=mock",
                        "--ofd.inn=" + inn,
                        "--ofd.sync-on-startup=false",
                        "--ofd.scheduler.enabled=false");
        int port = ((ServletWebServerApplicationContext) ctx).getWebServer().getPort();
        Running r = new Running(name, port, token, inn, ctx);
        r.collector().collect();
        return r;
    }
}

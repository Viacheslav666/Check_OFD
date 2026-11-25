package ru.ofd.agent;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Агент ОФД: ставится на RDP клиента, хранит ключ API ОФД локально, собирает чеки
 * и отдаёт их центральному приложению по HTTP (только с IP из белого списка и с токеном).
 * Конфигурация — agent.yml (рядом с jar или в config/), переменные окружения AGENT_* / OFD_*.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class AgentApplication {

    public static final String CONFIG_NAME = "spring.config.name=agent";

    public static void main(String[] args) {
        new SpringApplicationBuilder(AgentApplication.class)
                .properties(CONFIG_NAME)
                .run(args);
    }
}

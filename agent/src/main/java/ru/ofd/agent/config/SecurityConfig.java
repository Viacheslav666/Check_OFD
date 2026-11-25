package ru.ofd.agent.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ru.ofd.agent.security.AgentSecurityFilter;
import ru.ofd.agent.security.AllowlistHolder;
import ru.ofd.agent.security.AllowlistReloader;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    AllowlistHolder allowlistHolder(AgentProperties props) {
        AllowlistHolder h = new AllowlistHolder(props.getAllowedIps());
        log.info("Белый список IP агента: {}", h.entries());
        return h;
    }

    /**
     * Файл для горячей перезагрузки: agent.config-file, иначе первый существующий из
     * ./config/agent.yml и ./agent.yml (те же места, где Spring ищет внешний agent.yml).
     */
    @Bean
    AllowlistReloader allowlistReloader(AllowlistHolder holder,
                                        @Value("${agent.config-file:}") String configFile) {
        List<Path> c = new ArrayList<>();
        if (configFile != null && !configFile.isBlank()) {
            c.add(Path.of(configFile));
        } else {
            c.add(Path.of("config", "agent.yml"));
            c.add(Path.of("agent.yml"));
        }
        return new AllowlistReloader(holder, c);
    }

    @Bean
    FilterRegistrationBean<AgentSecurityFilter> agentSecurityFilter(AllowlistHolder allowlist, AgentTokenHolder token) {
        FilterRegistrationBean<AgentSecurityFilter> reg = new FilterRegistrationBean<>(
                new AgentSecurityFilter(allowlist, token.token()));
        // Защищаем всё, включая /actuator и ошибки: снаружи агенту нужен только API.
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}

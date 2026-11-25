package ru.ofd.agent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ru.ofd.agent.security.AgentSecurityFilter;
import ru.ofd.agent.security.IpAllowlist;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    FilterRegistrationBean<AgentSecurityFilter> agentSecurityFilter(AgentProperties props, AgentTokenHolder token) {
        IpAllowlist allowlist = new IpAllowlist(props.getAllowedIps());
        if (allowlist.isEmpty()) {
            throw new IllegalStateException("agent.allowed-ips пуст: укажите IP центрального приложения");
        }
        log.info("Белый список IP агента: {}", props.getAllowedIps());
        FilterRegistrationBean<AgentSecurityFilter> reg = new FilterRegistrationBean<>(
                new AgentSecurityFilter(allowlist, token.token()));
        // Защищаем всё, включая /actuator и ошибки: снаружи агенту нужен только API.
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }
}

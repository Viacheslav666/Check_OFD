package ru.ofd.agent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.ofd.marking.contract.AgentApi;

/**
 * 1) IP клиента (request.getRemoteAddr(), заголовки X-Forwarded-* НЕ учитываются) должен быть в белом списке — иначе 403;
 * 2) для всех путей кроме /health нужен заголовок X-Agent-Token с верным токеном — иначе 401.
 */
public class AgentSecurityFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AgentSecurityFilter.class);

    private final AllowlistHolder allowlist;
    private final byte[] token;

    public AgentSecurityFilter(AllowlistHolder allowlist, String token) {
        this.allowlist = allowlist;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        if (!allowlist.get().allows(ip)) {
            log.warn("Отклонён запрос {} {} с IP {}: адреса нет в белом списке agent.allowed-ips",
                    request.getMethod(), request.getRequestURI(), ip);
            deny(response, HttpServletResponse.SC_FORBIDDEN, "IP " + ip + " is not allowed");
            return;
        }
        if (!AgentApi.HEALTH.equals(request.getRequestURI())) {
            String provided = request.getHeader(AgentApi.TOKEN_HEADER);
            if (provided == null || !MessageDigest.isEqual(token, provided.getBytes(StandardCharsets.UTF_8))) {
                log.warn("Отклонён запрос {} {} с IP {}: неверный или отсутствующий {}",
                        request.getMethod(), request.getRequestURI(), ip, AgentApi.TOKEN_HEADER);
                deny(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid agent token");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private static void deny(HttpServletResponse response, int code, String msg) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + msg.replace("\"", "'") + "\"}");
    }
}

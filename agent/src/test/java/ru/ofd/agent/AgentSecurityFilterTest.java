package ru.ofd.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.ofd.agent.security.AgentSecurityFilter;
import ru.ofd.agent.security.IpAllowlist;
import ru.ofd.marking.contract.AgentApi;

class AgentSecurityFilterTest {

    private final AgentSecurityFilter filter =
            new AgentSecurityFilter(new IpAllowlist(List.of("10.10.50.2")), "secret-token-0123456789");

    private MockHttpServletResponse call(String ip, String path, String token) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", path);
        req.setRemoteAddr(ip);
        req.addHeader("X-Forwarded-For", "10.10.50.2"); // подмена заголовка не должна помогать
        if (token != null) req.addHeader(AgentApi.TOKEN_HEADER, token);
        MockHttpServletResponse resp = new MockHttpServletResponse();
        filter.doFilter(req, resp, new MockFilterChain());
        return resp;
    }

    @Test
    void rejectsForeignIpEvenWithValidToken() throws Exception {
        assertThat(call("10.10.50.99", AgentApi.STATUS, "secret-token-0123456789").getStatus()).isEqualTo(403);
        assertThat(call("10.10.50.99", AgentApi.HEALTH, null).getStatus()).isEqualTo(403);
    }

    @Test
    void requiresTokenExceptHealth() throws Exception {
        assertThat(call("10.10.50.2", AgentApi.HEALTH, null).getStatus()).isEqualTo(200);
        assertThat(call("10.10.50.2", AgentApi.RECEIPTS, null).getStatus()).isEqualTo(401);
        assertThat(call("10.10.50.2", AgentApi.RECEIPTS, "wrong").getStatus()).isEqualTo(401);
        assertThat(call("10.10.50.2", AgentApi.RECEIPTS, "secret-token-0123456789").getStatus()).isEqualTo(200);
    }
}

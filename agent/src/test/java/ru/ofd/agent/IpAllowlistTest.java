package ru.ofd.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import ru.ofd.agent.security.IpAllowlist;

class IpAllowlistTest {

    @Test
    void exactIpAndCidr() {
        IpAllowlist a = new IpAllowlist(List.of("10.10.50.2", "192.168.31.0/24, ::1"));
        assertThat(a.allows("10.10.50.2")).isTrue();
        assertThat(a.allows("10.10.50.3")).isFalse();
        assertThat(a.allows("192.168.31.196")).isTrue();
        assertThat(a.allows("192.168.32.1")).isFalse();
        assertThat(a.allows("0:0:0:0:0:0:0:1")).isTrue();
        assertThat(a.allows("::ffff:10.10.50.2")).isTrue();
    }

    @Test
    void garbageIsRejectedNotResolved() {
        IpAllowlist a = new IpAllowlist(List.of("127.0.0.1"));
        assertThat(a.allows("localhost")).isFalse();
        assertThat(a.allows("")).isFalse();
        assertThat(a.allows(null)).isFalse();
        assertThatThrownBy(() -> new IpAllowlist(List.of("example.com"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new IpAllowlist(List.of("10.0.0.0/33"))).isInstanceOf(IllegalArgumentException.class);
    }
}

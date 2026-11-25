package ru.ofd.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.ofd.agent.security.AllowlistHolder;
import ru.ofd.agent.security.AllowlistReloader;

class AllowlistReloaderTest {

    @TempDir
    Path dir;

    private long t = System.currentTimeMillis() - 100_000;

    private void write(Path f, String text) throws Exception {
        Files.writeString(f, text, StandardCharsets.UTF_8);
        t += 5_000;
        Files.setLastModifiedTime(f, FileTime.fromMillis(t));
    }

    @Test
    void reloadsAllowlistWithoutRestart() throws Exception {
        Path yml = dir.resolve("agent.yml");
        write(yml, "agent:\n  name: test\n  allowed-ips: \"192.168.31.196\"\n");
        AllowlistHolder h = new AllowlistHolder(List.of("192.168.31.196"));
        AllowlistReloader r = new AllowlistReloader(h, List.of(dir.resolve("config/agent.yml"), yml));

        assertThat(r.checkNow()).isFalse(); // файл не менялся
        assertThat(h.get().allows("10.20.30.40")).isFalse();

        write(yml, "agent:\n  allowed-ips: \"192.168.31.196, 10.20.30.0/24\"\n");
        assertThat(r.checkNow()).isTrue();
        assertThat(h.get().allows("10.20.30.40")).isTrue();
        assertThat(h.get().allows("192.168.31.196")).isTrue();

        // Смена адреса центра: старый больше не пускается
        write(yml, "\uFEFFagent:\n  allowed-ips:\n    - 192.168.31.200\n");
        assertThat(r.checkNow()).isTrue();
        assertThat(h.get().allows("192.168.31.200")).isTrue();
        assertThat(h.get().allows("192.168.31.196")).isFalse();
        assertThat(h.entries()).containsExactly("192.168.31.200");
    }

    @Test
    void keepsPreviousListOnEmptyOrInvalidValue() throws Exception {
        Path yml = dir.resolve("agent.yml");
        write(yml, "agent:\n  allowed-ips: \"192.168.31.196\"\n");
        AllowlistHolder h = new AllowlistHolder(List.of("192.168.31.196"));
        AllowlistReloader r = new AllowlistReloader(h, List.of(yml));

        write(yml, "agent:\n  allowed-ips: \"\"\n");
        assertThat(r.checkNow()).isFalse();
        write(yml, "agent:\n  allowed-ips: \"192.168.31.999\"\n");
        assertThat(r.checkNow()).isFalse();
        write(yml, "agent: [не yaml\n");
        assertThat(r.checkNow()).isFalse();
        assertThat(h.get().allows("192.168.31.196")).isTrue();
        assertThat(h.entries()).containsExactly("192.168.31.196");
    }
}

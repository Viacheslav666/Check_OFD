package ru.ofd.agent.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Токен доступа центра к агенту. Берётся из agent.token (AGENT_TOKEN); если пусто —
 * читается из data-dir/agent-token.txt, а при отсутствии файла генерируется (32 байта) и сохраняется туда.
 * Значение токена в лог не пишется.
 */
@Component
public class AgentTokenHolder {
    private static final Logger log = LoggerFactory.getLogger(AgentTokenHolder.class);
    public static final String TOKEN_FILE = "agent-token.txt";

    private final String token;

    public AgentTokenHolder(AgentProperties props) throws IOException {
        if (props.getToken() != null && !props.getToken().isBlank()) {
            if (props.getToken().trim().length() < 16) {
                throw new IllegalStateException("agent.token слишком короткий: нужно не меньше 16 символов");
            }
            this.token = props.getToken().trim();
            return;
        }
        Path file = Path.of(props.getDataDir()).resolve(TOKEN_FILE);
        if (Files.exists(file)) {
            this.token = Files.readString(file, StandardCharsets.UTF_8).trim();
        } else {
            byte[] buf = new byte[32];
            new SecureRandom().nextBytes(buf);
            this.token = HexFormat.of().formatHex(buf);
            Files.createDirectories(file.getParent());
            Files.writeString(file, token, StandardCharsets.UTF_8);
            log.warn("Токен агента сгенерирован и сохранён в {} — внесите его в карточку агента в ЛК центрального приложения",
                    file.toAbsolutePath());
        }
    }

    public String token() {
        return token;
    }
}

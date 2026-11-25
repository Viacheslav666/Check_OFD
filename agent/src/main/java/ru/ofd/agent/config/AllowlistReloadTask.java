package ru.ofd.agent.config;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ofd.agent.security.AllowlistReloader;

/** Проверка agent.yml раз в agent.allowlist-reload-seconds секунд (по умолчанию 10; 0 — выключено). */
@Component
@ConditionalOnExpression("${agent.allowlist-reload-seconds:10} > 0")
public class AllowlistReloadTask {
    private final AllowlistReloader reloader;

    public AllowlistReloadTask(AllowlistReloader reloader) {
        this.reloader = reloader;
    }

    @Scheduled(fixedDelayString = "${agent.allowlist-reload-seconds:10}",
            initialDelayString = "${agent.allowlist-reload-seconds:10}", timeUnit = TimeUnit.SECONDS)
    public void check() {
        reloader.checkNow();
    }
}

package ru.ofd.agent.collect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ofd.agent.config.OfdProperties;

@Component
public class CollectScheduler {
    private static final Logger log = LoggerFactory.getLogger(CollectScheduler.class);

    private final CollectorService collector;
    private final OfdProperties props;

    public CollectScheduler(CollectorService collector, OfdProperties props) {
        this.collector = collector;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (props.isSyncOnStartup()) {
            log.info("Первый сбор после старта (режим {}, ИНН {})", props.getMode(), props.getInns());
            collector.startAsync();
        }
    }

    @Scheduled(cron = "${ofd.scheduler.cron:0 */15 * * * *}")
    public void scheduled() {
        if (props.getScheduler().isEnabled()) {
            collector.collect();
        }
    }
}

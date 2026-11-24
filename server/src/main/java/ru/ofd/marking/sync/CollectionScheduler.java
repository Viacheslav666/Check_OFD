package ru.ofd.marking.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ofd.marking.config.OfdProperties;

@Component
public class CollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CollectionScheduler.class);
    private final CollectionService collectionService;
    private final OfdProperties props;

    public CollectionScheduler(CollectionService collectionService, OfdProperties props) {
        this.collectionService = collectionService;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (props.isSyncOnStartup()) {
            log.info("Startup sync (mode={})", props.getMode());
            Thread t = new Thread(collectionService::runSync, "ofd-startup-sync");
            t.setDaemon(true);
            t.start();
        }
    }

    @Scheduled(cron = "${ofd.scheduler.cron:0 */15 * * * *}")
    public void scheduled() {
        if (!props.getScheduler().isEnabled()) {
            return;
        }
        collectionService.runSync();
    }
}

package ru.ofd.marking.agents;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ofd.marking.config.CentralProperties;

@Component
public class AgentPollScheduler {

    private final AgentSyncService sync;
    private final CentralProperties props;

    public AgentPollScheduler(AgentSyncService sync, CentralProperties props) {
        this.sync = sync;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (props.isPollOnStartup()) {
            Thread t = new Thread(() -> sync.runSync(false), "agent-startup-poll");
            t.setDaemon(true);
            t.start();
        }
    }

    @Scheduled(cron = "${central.poll-cron:0 */5 * * * *}")
    public void scheduled() {
        if (props.isPollEnabled()) {
            sync.runSync(false);
        }
    }
}

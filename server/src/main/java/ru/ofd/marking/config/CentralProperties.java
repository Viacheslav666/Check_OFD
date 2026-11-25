package ru.ofd.marking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Настройки опроса агентов центральным приложением. */
@ConfigurationProperties(prefix = "central")
public class CentralProperties {
    /** Периодический опрос агентов. */
    private boolean pollEnabled = true;
    private String pollCron = "0 */5 * * * *";
    private boolean pollOnStartup = true;
    /** Размер страницы чеков при выгрузке с агента (≤ 1000). */
    private int pageSize = 500;
    /** Сколько агентов опрашивать параллельно. */
    private int parallelism = 6;
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 60;
    /** По кнопке «Синхронизация»: сколько ждать окончания сбора на агентах перед выгрузкой. */
    private int collectWaitSeconds = 180;
    /** Агент считается «не на связи», если успешного опроса не было дольше этого времени. */
    private int offlineAfterMinutes = 15;
    /**
     * Предзаполнение списка агентов при старте (для демо/docker): "имя|хост|порт|токен;имя2|...".
     * Агент добавляется, только если агента с таким именем ещё нет.
     */
    private String bootstrapAgents = "";

    public boolean isPollEnabled() { return pollEnabled; }
    public void setPollEnabled(boolean pollEnabled) { this.pollEnabled = pollEnabled; }
    public String getPollCron() { return pollCron; }
    public void setPollCron(String pollCron) { this.pollCron = pollCron; }
    public boolean isPollOnStartup() { return pollOnStartup; }
    public void setPollOnStartup(boolean pollOnStartup) { this.pollOnStartup = pollOnStartup; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public int getParallelism() { return parallelism; }
    public void setParallelism(int parallelism) { this.parallelism = parallelism; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
    public int getCollectWaitSeconds() { return collectWaitSeconds; }
    public void setCollectWaitSeconds(int collectWaitSeconds) { this.collectWaitSeconds = collectWaitSeconds; }
    public int getOfflineAfterMinutes() { return offlineAfterMinutes; }
    public void setOfflineAfterMinutes(int offlineAfterMinutes) { this.offlineAfterMinutes = offlineAfterMinutes; }
    public String getBootstrapAgents() { return bootstrapAgents; }
    public void setBootstrapAgents(String bootstrapAgents) { this.bootstrapAgents = bootstrapAgents; }
}

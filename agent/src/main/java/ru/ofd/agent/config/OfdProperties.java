package ru.ofd.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ofd")
public class OfdProperties {

    public enum Mode { mock, live }
    public enum FilterTimeField { DOC_DATE_TIME, C_DATE_UTC }

    private Mode mode = Mode.mock;
    private String baseUrl = "https://ofd.ru";
    /** Never log this value. */
    private String apiKey = "";
    private String inn = "";
    /** Assumption Q4: receipts-info returns all receipts. */
    private boolean assumeReceiptsInfoAll = true;
    private FilterTimeField filterTimeField = FilterTimeField.DOC_DATE_TIME;
    private long rateLimitMs = 2000;
    private int maxWindowDays = 30;
    private int maxDepthDays = 90;
    private int maxRetries = 5;
    private long retryBackoffMs = 2000;
    /** Сбор сразу после старта агента. */
    private boolean syncOnStartup = false;
    private Scheduler scheduler = new Scheduler();

    public static class Scheduler {
        /** Периодический сбор из ОФД (настраивается в agent.yml). */
        private boolean enabled = false;
        private String cron = "0 */15 * * * *";
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
    }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getInn() { return inn; }
    /** ИНН через запятую: один ключ API может обслуживать несколько организаций. */
    public java.util.List<String> getInns() {
        if (inn == null || inn.isBlank()) return java.util.List.of();
        return java.util.Arrays.stream(inn.split("[,;\\s]+")).map(String::trim).filter(x -> !x.isEmpty()).distinct().toList();
    }
    public void setInn(String inn) { this.inn = inn; }
    public boolean isAssumeReceiptsInfoAll() { return assumeReceiptsInfoAll; }
    public void setAssumeReceiptsInfoAll(boolean assumeReceiptsInfoAll) { this.assumeReceiptsInfoAll = assumeReceiptsInfoAll; }
    public FilterTimeField getFilterTimeField() { return filterTimeField; }
    public void setFilterTimeField(FilterTimeField filterTimeField) { this.filterTimeField = filterTimeField; }
    public long getRateLimitMs() { return rateLimitMs; }
    public void setRateLimitMs(long rateLimitMs) { this.rateLimitMs = rateLimitMs; }
    public int getMaxWindowDays() { return maxWindowDays; }
    public void setMaxWindowDays(int maxWindowDays) { this.maxWindowDays = maxWindowDays; }
    public int getMaxDepthDays() { return maxDepthDays; }
    public void setMaxDepthDays(int maxDepthDays) { this.maxDepthDays = maxDepthDays; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public long getRetryBackoffMs() { return retryBackoffMs; }
    public void setRetryBackoffMs(long retryBackoffMs) { this.retryBackoffMs = retryBackoffMs; }
    public boolean isSyncOnStartup() { return syncOnStartup; }
    public void setSyncOnStartup(boolean syncOnStartup) { this.syncOnStartup = syncOnStartup; }
    public Scheduler getScheduler() { return scheduler; }
    public void setScheduler(Scheduler scheduler) { this.scheduler = scheduler; }
}

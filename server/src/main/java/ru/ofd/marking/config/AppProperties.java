package ru.ofd.marking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String clientApiKey = "";
    private double defaultWorstThresholdPercent = 25.0;

    public String getClientApiKey() { return clientApiKey; }
    public void setClientApiKey(String clientApiKey) { this.clientApiKey = clientApiKey; }
    public double getDefaultWorstThresholdPercent() { return defaultWorstThresholdPercent; }
    public void setDefaultWorstThresholdPercent(double defaultWorstThresholdPercent) {
        this.defaultWorstThresholdPercent = defaultWorstThresholdPercent;
    }
}

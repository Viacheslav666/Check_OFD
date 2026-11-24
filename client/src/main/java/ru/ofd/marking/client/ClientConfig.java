package ru.ofd.marking.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ClientConfig {
    private String serverUrl = "http://127.0.0.1:18443";
    private String apiKey = "";
    private double threshold = 25.0;
    private int periodDays = 30;

    public static ClientConfig load() {
        ClientConfig c = new ClientConfig();
        Path p = Path.of(System.getProperty("user.home"), ".ofd-marking", "client.properties");
        if (Files.exists(p)) {
            Properties props = new Properties();
            try (var in = Files.newInputStream(p)) {
                props.load(in);
                c.serverUrl = props.getProperty("serverUrl", c.serverUrl);
                c.apiKey = props.getProperty("apiKey", c.apiKey);
                c.threshold = Double.parseDouble(props.getProperty("threshold", "25.0"));
                c.periodDays = Integer.parseInt(props.getProperty("periodDays", "30"));
            } catch (Exception ignored) {
            }
        }
        String envUrl = System.getenv("OFD_CLIENT_SERVER_URL");
        if (envUrl != null && !envUrl.isBlank()) {
            c.serverUrl = envUrl;
        }
        return c;
    }

    public void save() {
        Path dir = Path.of(System.getProperty("user.home"), ".ofd-marking");
        try {
            Files.createDirectories(dir);
            Properties props = new Properties();
            props.setProperty("serverUrl", serverUrl);
            props.setProperty("apiKey", apiKey);
            props.setProperty("threshold", String.valueOf(threshold));
            props.setProperty("periodDays", String.valueOf(periodDays));
            try (var out = Files.newOutputStream(dir.resolve("client.properties"))) {
                props.store(out, "OFD Marking Client");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }
    public int getPeriodDays() { return periodDays; }
    public void setPeriodDays(int periodDays) { this.periodDays = periodDays; }
}

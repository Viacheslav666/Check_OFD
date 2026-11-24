package ru.ofd.marking.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private final ClientConfig config;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ApiClient(ClientConfig config) {
        this.config = config;
    }

    public Instant from() {
        return Instant.now().minusSeconds(config.getPeriodDays() * 86400L);
    }

    public Instant to() {
        return Instant.now().plusSeconds(3600);
    }

    public List<Map<String, Object>> tradePoints() {
        return getList("/api/trade-points?from=" + enc(from()) + "&to=" + enc(to()));
    }

    public List<Map<String, Object>> worst(double threshold) {
        return getList("/api/trade-points/worst?from=" + enc(from()) + "&to=" + enc(to())
                + "&threshold=" + threshold);
    }

    public List<Map<String, Object>> groups() {
        return getList("/api/groups?from=" + enc(from()) + "&to=" + enc(to()));
    }

    public List<Map<String, Object>> matrix() {
        return getList("/api/matrix?from=" + enc(from()) + "&to=" + enc(to()));
    }

    public JsonNode receipts(Long tpId, boolean markedOnly, String name, int page) {
        StringBuilder q = new StringBuilder("/api/receipts?from=" + enc(from()) + "&to=" + enc(to())
                + "&markedOnly=" + markedOnly + "&page=" + page + "&size=50");
        if (tpId != null) q.append("&tradePointId=").append(tpId);
        if (name != null && !name.isBlank()) q.append("&nameContains=").append(encRaw(name));
        return getJson(q.toString());
    }

    public JsonNode receipt(long id) {
        return getJson("/api/receipts/" + id);
    }

    public Map<String, String> settings() {
        return get(new TypeReference<>() {}, "/api/settings");
    }

    public Map<String, String> updateSettings(Map<String, String> body) {
        return put(new TypeReference<>() {}, "/api/settings", body);
    }

    public JsonNode syncStatus() {
        return getJson("/api/sync/status");
    }

    public void triggerSync() {
        post("/api/sync/trigger", "{}");
    }

    public byte[] export(String type) {
        return getBytes("/api/export/" + type + "?from=" + enc(from()) + "&to=" + enc(to()));
    }

    private List<Map<String, Object>> getList(String path) {
        return get(new TypeReference<>() {}, path);
    }

    private <T> T get(TypeReference<T> type, String path) {
        try {
            HttpResponse<String> resp = http.send(request("GET", path, null), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return mapper.readValue(resp.body(), type);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка запроса " + path + ": " + e.getMessage(), e);
        }
    }

    private JsonNode getJson(String path) {
        try {
            HttpResponse<String> resp = http.send(request("GET", path, null), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return mapper.readTree(resp.body());
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка запроса " + path + ": " + e.getMessage(), e);
        }
    }

    private <T> T put(TypeReference<T> type, String path, Object body) {
        try {
            String json = mapper.writeValueAsString(body);
            HttpResponse<String> resp = http.send(request("PUT", path, json), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
            return mapper.readValue(resp.body(), type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void post(String path, String body) {
        try {
            HttpResponse<String> resp = http.send(request("POST", path, body), HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + resp.statusCode() + ": " + resp.body());
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] getBytes(String path) {
        try {
            HttpResponse<byte[]> resp = http.send(request("GET", path, null), HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("HTTP " + resp.statusCode());
            }
            return resp.body();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpRequest request(String method, String path, String body) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(trimSlash(config.getServerUrl()) + path))
                .timeout(Duration.ofSeconds(60));
        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            b.header("X-Api-Key", config.getApiKey());
        }
        if ("GET".equals(method)) {
            return b.GET().build();
        }
        if ("PUT".equals(method)) {
            return b.header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body)).build();
        }
        return b.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body)).build();
    }

    private static String trimSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String enc(Instant i) {
        return encRaw(i.toString());
    }

    private static String encRaw(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

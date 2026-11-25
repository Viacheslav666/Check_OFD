package ru.ofd.agent.ofd;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.ofd.agent.config.OfdProperties;
import ru.ofd.marking.ofd.dto.OfdEnvelope;
import ru.ofd.marking.ofd.dto.OfdKktDto;
import ru.ofd.marking.ofd.dto.OfdReceiptDto;

@Component
@ConditionalOnProperty(name = "ofd.mode", havingValue = "live")
public class RateLimitedHttpOfdClient implements OfdApiClient {

    private static final Logger log = LoggerFactory.getLogger(RateLimitedHttpOfdClient.class);
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    private final OfdProperties props;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    private final AtomicLong lastReceiptsInfoAt = new AtomicLong(0);

    public RateLimitedHttpOfdClient(OfdProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        props.getInns().forEach(RateLimitedHttpOfdClient::validateInn);
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new IllegalStateException("OFD_API_KEY не задан: в режиме live агенту нужен ключ API ОФД");
        }
    }

    @Override
    public List<OfdKktDto> listKkts(String inn) {
        validateInn(inn);
        throttle();
        String url = props.getBaseUrl() + "/api/integration/v2/inn/" + enc(inn)
                + "/kkts?AuthToken=" + enc(props.getApiKey());
        return get(url, new TypeReference<OfdEnvelope<List<OfdKktDto>>>() {});
    }

    @Override
    public List<OfdReceiptDto> receiptsInfo(String inn, String rnm, Instant from, Instant to) {
        validateInn(inn);
        throttle();
        String url = props.getBaseUrl() + "/api/integration/v2/inn/" + enc(inn)
                + "/kkt/" + enc(rnm) + "/receipts-info"
                + "?dateFrom=" + enc(ISO.format(from))
                + "&dateTo=" + enc(ISO.format(to))
                + "&AuthToken=" + enc(props.getApiKey());
        List<OfdReceiptDto> data = get(url, new TypeReference<OfdEnvelope<List<OfdReceiptDto>>>() {});
        if (data != null && data.size() > 5000) {
            log.warn("receipts-info returned {} rows for RNM {} — possible truncation (Q1)", data.size(), rnm);
        }
        return data == null ? List.of() : data;
    }

    private void throttle() {
        long minGap = props.getRateLimitMs();
        while (true) {
            long now = System.currentTimeMillis();
            long last = lastReceiptsInfoAt.get();
            long wait = minGap - (now - last);
            if (wait <= 0 && lastReceiptsInfoAt.compareAndSet(last, now)) {
                return;
            }
            try {
                Thread.sleep(Math.max(wait, 50));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while rate-limiting", e);
            }
        }
    }

    private <T> T get(String url, TypeReference<OfdEnvelope<T>> type) {
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofMinutes(2))
                        .GET()
                        .header("Accept", "application/json")
                        .build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = resp.statusCode();
                if (code == 200) {
                    OfdEnvelope<T> env = mapper.readValue(resp.body(), type);
                    if (env != null && env.isSuccess()) {
                        return env.getData() == null ? empty(type) : env.getData();
                    }
                    throw new OfdApiException(400, "OFD Status!=Success: " + mask(url));
                }
                if (code == 429 || code >= 500) {
                    if (attempt > props.getMaxRetries()) {
                        throw new OfdApiException(code, "OFD retries exhausted for " + mask(url));
                    }
                    long sleep = props.getRetryBackoffMs() * attempt;
                    log.warn("OFD HTTP {} — retry {} in {} ms", code, attempt, sleep);
                    Thread.sleep(sleep);
                    continue;
                }
                throw new OfdApiException(code, "OFD HTTP " + code + " for " + mask(url));
            } catch (OfdApiException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (Exception e) {
                if (attempt > props.getMaxRetries()) {
                    throw new OfdApiException(0, "OFD call failed: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(props.getRetryBackoffMs() * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T empty(TypeReference<OfdEnvelope<T>> type) {
        return (T) Collections.emptyList();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private String mask(String url) {
        return url.replaceAll("AuthToken=[^&]*", "AuthToken=***");
    }

    public static void validateInn(String inn) {
        if (inn == null || !inn.matches("^\\d{10}(\\d{2})?$")) {
            throw new IllegalArgumentException("ИНН должен содержать 10 или 12 цифр: " + inn);
        }
    }
}

package ru.ofd.marking.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.ofd.marking.config.CentralProperties;
import ru.ofd.marking.contract.AgentApi;
import ru.ofd.marking.contract.AgentKktDto;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.contract.CollectResultDto;
import ru.ofd.marking.contract.ReceiptPageDto;
import ru.ofd.marking.domain.entity.Agent;

/** HTTP-клиент центра к API агента (инициатор всех запросов — центр). */
@Component
public class AgentHttpClient {

    private final ObjectMapper mapper;
    private final CentralProperties props;
    private final HttpClient http;

    public AgentHttpClient(ObjectMapper mapper, CentralProperties props) {
        this.mapper = mapper;
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(props.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public JsonNode health(Agent a) {
        return call(a, "GET", AgentApi.HEALTH, JsonNode.class);
    }

    public AgentStatusDto status(Agent a) {
        return call(a, "GET", AgentApi.STATUS, AgentStatusDto.class);
    }

    public List<AgentKktDto> kkts(Agent a) {
        return Arrays.asList(call(a, "GET", AgentApi.KKTS, AgentKktDto[].class));
    }

    public ReceiptPageDto receipts(Agent a, long after, int limit) {
        return call(a, "GET", AgentApi.RECEIPTS + "?after=" + after + "&limit=" + limit, ReceiptPageDto.class);
    }

    public CollectResultDto collect(Agent a) {
        return call(a, "POST", AgentApi.COLLECT, CollectResultDto.class);
    }

    private <T> T call(Agent a, String method, String path, Class<T> type) {
        String url = a.baseUrl() + path;
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(props.getReadTimeoutSeconds()))
                .header("Accept", "application/json")
                .header(AgentApi.TOKEN_HEADER, a.getToken() == null ? "" : a.getToken());
        b = "POST".equals(method) ? b.POST(HttpRequest.BodyPublishers.noBody()) : b.GET();
        HttpResponse<String> resp;
        try {
            resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (java.net.ConnectException e) {
            throw new AgentCallException(0, "нет соединения с " + a.baseUrl() + " (агент не запущен, порт закрыт или неверный IP)");
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AgentCallException(0, "таймаут запроса к " + a.baseUrl());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentCallException(0, "прервано");
        } catch (Exception e) {
            throw new AgentCallException(0, "ошибка связи с " + a.baseUrl() + ": " + e.getMessage());
        }
        int code = resp.statusCode();
        if (code == 401) {
            throw new AgentCallException(code, "агент отклонил токен (HTTP 401): проверьте токен в карточке агента");
        }
        if (code == 403) {
            throw new AgentCallException(code, "агент отклонил IP центра (HTTP 403): добавьте IP центра в agent.allowed-ips");
        }
        if (code >= 400) {
            throw new AgentCallException(code, "агент вернул HTTP " + code + " на " + path);
        }
        try {
            return mapper.readValue(resp.body(), type);
        } catch (Exception e) {
            throw new AgentCallException(code, "неверный ответ агента на " + path + ": " + e.getMessage());
        }
    }

    public static class AgentCallException extends RuntimeException {
        private final int httpStatus;

        public AgentCallException(int httpStatus, String message) {
            super(message);
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }
    }
}

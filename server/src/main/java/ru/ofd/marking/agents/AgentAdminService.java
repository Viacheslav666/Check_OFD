package ru.ofd.marking.agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.ofd.marking.config.CentralProperties;
import ru.ofd.marking.contract.AgentStatusDto;
import ru.ofd.marking.domain.entity.Agent;
import ru.ofd.marking.domain.repo.AgentRepository;
import ru.ofd.marking.domain.repo.KktRepository;
import ru.ofd.marking.domain.repo.ReceiptRepository;
import ru.ofd.marking.web.dto.AgentRequest;
import ru.ofd.marking.web.dto.AgentView;

/** Реестр агентов («ЛК приложения»): CRUD, проверка связи, статусы. */
@Service
public class AgentAdminService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AgentAdminService.class);
    private static final String HOST_RE =
            "^(\\d{1,3}(\\.\\d{1,3}){3}|\\[?[0-9a-fA-F:]+\\]?|[A-Za-z0-9]([A-Za-z0-9-]{0,62})(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,62}))*)$";

    private final AgentRepository repo;
    private final KktRepository kktRepository;
    private final ReceiptRepository receiptRepository;
    private final AgentHttpClient client;
    private final CentralProperties props;
    private final ObjectMapper mapper;

    public AgentAdminService(AgentRepository repo, KktRepository kktRepository, ReceiptRepository receiptRepository,
                             AgentHttpClient client, CentralProperties props, ObjectMapper mapper) {
        this.repo = repo;
        this.kktRepository = kktRepository;
        this.receiptRepository = receiptRepository;
        this.client = client;
        this.props = props;
        this.mapper = mapper;
    }

    /** Предзаполнение агентов из central.bootstrap-agents (демо/docker). */
    @Override
    public void run(ApplicationArguments args) {
        String spec = props.getBootstrapAgents();
        if (spec == null || spec.isBlank()) return;
        for (String item : spec.split(";")) {
            String[] p = item.trim().split("\\|");
            if (p.length < 4) continue;
            if (repo.findByName(p[0].trim()).isPresent()) continue;
            create(new AgentRequest(p[0].trim(), p[1].trim(), Integer.parseInt(p[2].trim()), false, p[3].trim(), true,
                    "Добавлен автоматически (central.bootstrap-agents)"));
            log.info("Добавлен агент из bootstrap: {} {}:{}", p[0], p[1], p[2]);
        }
    }

    @Transactional(readOnly = true)
    public List<AgentView> list() {
        return repo.findAllByOrderByNameAsc().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public AgentView get(Long id) {
        return view(find(id));
    }

    @Transactional
    public AgentView create(AgentRequest r) {
        Agent a = new Agent();
        apply(a, r, true);
        if (repo.findByName(a.getName()).isPresent()) {
            throw bad("Агент с именем «" + a.getName() + "» уже есть");
        }
        return view(repo.save(a));
    }

    @Transactional
    public AgentView update(Long id, AgentRequest r) {
        Agent a = find(id);
        String oldUrl = a.baseUrl();
        apply(a, r, false);
        repo.findByName(a.getName()).filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw bad("Агент с именем «" + o.getName() + "» уже есть"); });
        if (!oldUrl.equals(a.baseUrl())) {
            // Новый адрес — возможно, другой агент: начинаем выгрузку с начала (дубли отсекаются)
            a.setLastCursor(0);
            a.setAgentInstanceId(null);
        }
        a.setUpdatedAt(Instant.now());
        return view(repo.save(a));
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(find(id));
    }

    /** Проверка связи из ЛК: health (только IP) + status (IP + токен). */
    public Map<String, Object> test(Long id) {
        Agent a = find(id);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("agent", a.getName());
        res.put("url", a.baseUrl());
        try {
            client.health(a);
            res.put("reachable", true);
        } catch (Exception e) {
            res.put("reachable", false);
            res.put("ok", false);
            res.put("error", e.getMessage());
            return res;
        }
        try {
            AgentStatusDto st = client.status(a);
            res.put("ok", true);
            res.put("status", st);
        } catch (Exception e) {
            res.put("ok", false);
            res.put("error", e.getMessage());
        }
        return res;
    }

    private void apply(Agent a, AgentRequest r, boolean create) {
        if (r == null) throw bad("Пустой запрос");
        if (create || r.name() != null) {
            String name = r.name() == null ? "" : r.name().trim();
            if (name.isEmpty() || name.length() > 100) throw bad("Имя агента: от 1 до 100 символов");
            a.setName(name);
        }
        if (create || r.host() != null) {
            String host = r.host() == null ? "" : r.host().trim();
            if (host.isEmpty() || !host.matches(HOST_RE)) throw bad("Укажите IP-адрес или имя хоста агента");
            a.setHost(host.replace("[", "").replace("]", ""));
        }
        if (create || r.port() != null) {
            int port = r.port() == null ? 18500 : r.port();
            if (port < 1 || port > 65535) throw bad("Порт должен быть от 1 до 65535");
            a.setPort(port);
        }
        if (r.useTls() != null) a.setUseTls(r.useTls());
        if (r.enabled() != null) a.setEnabled(r.enabled());
        if (r.comment() != null) a.setComment(r.comment().trim());
        if (r.token() != null && !r.token().isBlank()) {
            if (r.token().trim().length() < 16) throw bad("Токен агента: не меньше 16 символов");
            a.setToken(r.token().trim());
        } else if (create) {
            throw bad("Укажите токен агента (agent.token или файл data/agent-token.txt на агенте)");
        }
    }

    private AgentView view(Agent a) {
        AgentStatusDto st = null;
        if (a.getLastStatusJson() != null) {
            try {
                st = mapper.readValue(a.getLastStatusJson(), AgentStatusDto.class);
            } catch (Exception ignored) {
            }
        }
        Instant now = Instant.now();
        boolean online = a.isEnabled() && a.getLastSuccessAt() != null
                && Duration.between(a.getLastSuccessAt(), now).toMinutes() < props.getOfflineAfterMinutes()
                && a.getLastError() == null;
        String state;
        if (!a.isEnabled()) state = "DISABLED";
        else if (a.getLastPollAt() == null) state = "NEW";
        else if (a.getLastError() != null) state = "ERROR";
        else if (online) state = "ONLINE";
        else state = "OFFLINE";
        Long pending = a.getAgentMaxSeq() == null ? null : Math.max(0, a.getAgentMaxSeq() - a.getLastCursor());
        Instant upTo = st == null ? null : st.collectedUpTo();
        Long lag = upTo == null ? null : Math.max(0, Duration.between(upTo, now).toMinutes());
        String token = a.getToken() == null ? "" : a.getToken();
        return new AgentView(a.getId(), a.getName(), a.getHost(), a.getPort(), a.isUseTls(), a.isEnabled(), a.getComment(),
                !token.isEmpty(), token.length() >= 4 ? "…" + token.substring(token.length() - 4) : "", a.baseUrl(),
                state, online, a.getLastPollAt(), a.getLastSuccessAt(), a.getLastError(), a.getConsecutiveFailures(),
                a.getLastCursor(), a.getAgentMaxSeq(), pending, a.getReceiptsReceived(),
                receiptRepository.countByAgentId(a.getId()), kktRepository.countByAgentId(a.getId()),
                st == null ? null : st.version(), st == null ? null : st.mode(), st == null ? List.of() : st.inns(),
                st == null ? null : st.collecting(), st == null ? null : st.lastCollectSuccessAt(),
                st == null ? null : st.lastCollectError(), upTo, lag);
    }

    private Agent find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Агент не найден"));
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}

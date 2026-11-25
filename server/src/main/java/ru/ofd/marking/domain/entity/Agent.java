package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Агент на RDP клиента, зарегистрированный в ЛК центрального приложения. */
@Entity
@Table(name = "agent")
public class Agent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    @Column(nullable = false)
    private String host;
    @Column(nullable = false)
    private int port;
    @Column(name = "use_tls", nullable = false)
    private boolean useTls;
    /** Общий секрет с агентом. Наружу через API не отдаётся. */
    @Column(nullable = false, columnDefinition = "text")
    private String token;
    @Column(nullable = false)
    private boolean enabled = true;
    @Column(columnDefinition = "text")
    private String comment;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Column(name = "last_cursor", nullable = false)
    private long lastCursor;
    @Column(name = "agent_instance_id", length = 64)
    private String agentInstanceId;
    @Column(name = "agent_max_seq")
    private Long agentMaxSeq;
    @Column(name = "last_poll_at")
    private Instant lastPollAt;
    @Column(name = "last_success_at")
    private Instant lastSuccessAt;
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
    @Column(name = "last_status_json", columnDefinition = "text")
    private String lastStatusJson;
    @Column(name = "receipts_received", nullable = false)
    private long receiptsReceived;
    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    public String baseUrl() {
        String h = host.contains(":") && !host.startsWith("[") ? "[" + host + "]" : host;
        return (useTls ? "https://" : "http://") + h + ":" + port;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public boolean isUseTls() { return useTls; }
    public void setUseTls(boolean useTls) { this.useTls = useTls; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getLastCursor() { return lastCursor; }
    public void setLastCursor(long lastCursor) { this.lastCursor = lastCursor; }
    public String getAgentInstanceId() { return agentInstanceId; }
    public void setAgentInstanceId(String agentInstanceId) { this.agentInstanceId = agentInstanceId; }
    public Long getAgentMaxSeq() { return agentMaxSeq; }
    public void setAgentMaxSeq(Long agentMaxSeq) { this.agentMaxSeq = agentMaxSeq; }
    public Instant getLastPollAt() { return lastPollAt; }
    public void setLastPollAt(Instant lastPollAt) { this.lastPollAt = lastPollAt; }
    public Instant getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(Instant lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getLastStatusJson() { return lastStatusJson; }
    public void setLastStatusJson(String lastStatusJson) { this.lastStatusJson = lastStatusJson; }
    public long getReceiptsReceived() { return receiptsReceived; }
    public void setReceiptsReceived(long receiptsReceived) { this.receiptsReceived = receiptsReceived; }
    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }
}

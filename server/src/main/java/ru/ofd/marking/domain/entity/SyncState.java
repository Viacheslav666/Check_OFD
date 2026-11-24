package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sync_state")
public class SyncState {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "kkt_reg_number", nullable = false, unique = true, length = 32)
    private String kktRegNumber;
    @Column(name = "last_success_to")
    private Instant lastSuccessTo;
    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;
    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;
    @Column(name = "initial_done", nullable = false)
    private boolean initialDone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKktRegNumber() { return kktRegNumber; }
    public void setKktRegNumber(String kktRegNumber) { this.kktRegNumber = kktRegNumber; }
    public Instant getLastSuccessTo() { return lastSuccessTo; }
    public void setLastSuccessTo(Instant lastSuccessTo) { this.lastSuccessTo = lastSuccessTo; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public boolean isInitialDone() { return initialDone; }
    public void setInitialDone(boolean initialDone) { this.initialDone = initialDone; }
}

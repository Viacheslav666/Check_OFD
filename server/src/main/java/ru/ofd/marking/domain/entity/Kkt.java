package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "kkt")
public class Kkt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ofd_id", length = 64)
    private String ofdId;
    @Column(name = "reg_number", nullable = false, unique = true, length = 32)
    private String regNumber;
    private String name;
    @Column(name = "serial_number", length = 64)
    private String serialNumber;
    @Column(name = "fn_number", length = 64)
    private String fnNumber;
    @Column(name = "lk_path", columnDefinition = "text")
    private String lkPath;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_point_id", nullable = false)
    private TradePoint tradePoint;
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOfdId() { return ofdId; }
    public void setOfdId(String ofdId) { this.ofdId = ofdId; }
    public String getRegNumber() { return regNumber; }
    public void setRegNumber(String regNumber) { this.regNumber = regNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getFnNumber() { return fnNumber; }
    public void setFnNumber(String fnNumber) { this.fnNumber = fnNumber; }
    public String getLkPath() { return lkPath; }
    public void setLkPath(String lkPath) { this.lkPath = lkPath; }
    public TradePoint getTradePoint() { return tradePoint; }
    public void setTradePoint(TradePoint tradePoint) { this.tradePoint = tradePoint; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}

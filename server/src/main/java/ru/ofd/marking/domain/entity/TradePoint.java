package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "trade_point")
public class TradePoint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;
    @Column(nullable = false, columnDefinition = "text")
    private String address;
    @Column(nullable = false, columnDefinition = "text")
    private String place;
    @Column(name = "display_name", nullable = false, columnDefinition = "text")
    private String displayName;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

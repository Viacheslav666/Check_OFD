package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receipt")
public class Receipt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ofd_raw_id", nullable = false, unique = true, length = 64)
    private String ofdRawId;
    @Column(name = "doc_tag", nullable = false)
    private int docTag;
    @Column(name = "is_bso", nullable = false)
    private boolean bso;
    @Column(name = "is_correction", nullable = false)
    private boolean correction;
    @Column(name = "operation_type", length = 32)
    private String operationType;
    @Column(name = "user_inn", length = 12)
    private String userInn;
    @Column(name = "kkt_reg_number", nullable = false, length = 32)
    private String kktRegNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_point_id", nullable = false)
    private TradePoint tradePoint;
    @Column(name = "fn_number", length = 64)
    private String fnNumber;
    @Column(name = "doc_number")
    private Long docNumber;
    @Column(name = "doc_shift_number")
    private Integer docShiftNumber;
    @Column(name = "receipt_number")
    private Integer receiptNumber;
    @Column(name = "doc_date_time", nullable = false)
    private Instant docDateTime;
    @Column(name = "c_date_utc")
    private Instant cDateUtc;
    @Column(name = "total_sum_kopecks", nullable = false)
    private long totalSumKopecks;
    @Column(name = "fiscal_sign", length = 64)
    private String fiscalSign;
    @Column(name = "has_marked_goods", nullable = false)
    private boolean hasMarkedGoods;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "agent_id")
    private Long agentId;
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo ASC")
    private List<ReceiptItem> items = new ArrayList<>();

    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOfdRawId() { return ofdRawId; }
    public void setOfdRawId(String ofdRawId) { this.ofdRawId = ofdRawId; }
    public int getDocTag() { return docTag; }
    public void setDocTag(int docTag) { this.docTag = docTag; }
    public boolean isBso() { return bso; }
    public void setBso(boolean bso) { this.bso = bso; }
    public boolean isCorrection() { return correction; }
    public void setCorrection(boolean correction) { this.correction = correction; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getUserInn() { return userInn; }
    public void setUserInn(String userInn) { this.userInn = userInn; }
    public String getKktRegNumber() { return kktRegNumber; }
    public void setKktRegNumber(String kktRegNumber) { this.kktRegNumber = kktRegNumber; }
    public TradePoint getTradePoint() { return tradePoint; }
    public void setTradePoint(TradePoint tradePoint) { this.tradePoint = tradePoint; }
    public String getFnNumber() { return fnNumber; }
    public void setFnNumber(String fnNumber) { this.fnNumber = fnNumber; }
    public Long getDocNumber() { return docNumber; }
    public void setDocNumber(Long docNumber) { this.docNumber = docNumber; }
    public Integer getDocShiftNumber() { return docShiftNumber; }
    public void setDocShiftNumber(Integer docShiftNumber) { this.docShiftNumber = docShiftNumber; }
    public Integer getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(Integer receiptNumber) { this.receiptNumber = receiptNumber; }
    public Instant getDocDateTime() { return docDateTime; }
    public void setDocDateTime(Instant docDateTime) { this.docDateTime = docDateTime; }
    public Instant getCDateUtc() { return cDateUtc; }
    public void setCDateUtc(Instant cDateUtc) { this.cDateUtc = cDateUtc; }
    public long getTotalSumKopecks() { return totalSumKopecks; }
    public void setTotalSumKopecks(long totalSumKopecks) { this.totalSumKopecks = totalSumKopecks; }
    public String getFiscalSign() { return fiscalSign; }
    public void setFiscalSign(String fiscalSign) { this.fiscalSign = fiscalSign; }
    public boolean isHasMarkedGoods() { return hasMarkedGoods; }
    public void setHasMarkedGoods(boolean hasMarkedGoods) { this.hasMarkedGoods = hasMarkedGoods; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<ReceiptItem> getItems() { return items; }
    public void setItems(List<ReceiptItem> items) { this.items = items; }

    public void addItem(ReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }
}

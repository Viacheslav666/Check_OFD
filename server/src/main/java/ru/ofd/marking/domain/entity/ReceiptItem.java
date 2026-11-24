package ru.ofd.marking.domain.entity;

import jakarta.persistence.*;
import ru.ofd.marking.domain.ProductGroup;
import java.math.BigDecimal;

@Entity
@Table(name = "receipt_item")
public class ReceiptItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id", nullable = false)
    private Receipt receipt;
    @Column(name = "line_no", nullable = false)
    private int lineNo;
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    @Column(name = "price_kopecks")
    private Long priceKopecks;
    private BigDecimal quantity;
    @Column(name = "total_kopecks")
    private Long totalKopecks;
    @Column(name = "subject_type")
    private Integer subjectType;
    @Column(name = "product_code_json", columnDefinition = "text")
    private String productCodeJson;
    @Enumerated(EnumType.STRING)
    @Column(name = "product_group", nullable = false, length = 32)
    private ProductGroup productGroup = ProductGroup.OTHER;
    @Column(name = "is_marked", nullable = false)
    private boolean marked;
    @Column(name = "nds_rate")
    private Integer ndsRate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Receipt getReceipt() { return receipt; }
    public void setReceipt(Receipt receipt) { this.receipt = receipt; }
    public int getLineNo() { return lineNo; }
    public void setLineNo(int lineNo) { this.lineNo = lineNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getPriceKopecks() { return priceKopecks; }
    public void setPriceKopecks(Long priceKopecks) { this.priceKopecks = priceKopecks; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public Long getTotalKopecks() { return totalKopecks; }
    public void setTotalKopecks(Long totalKopecks) { this.totalKopecks = totalKopecks; }
    public Integer getSubjectType() { return subjectType; }
    public void setSubjectType(Integer subjectType) { this.subjectType = subjectType; }
    public String getProductCodeJson() { return productCodeJson; }
    public void setProductCodeJson(String productCodeJson) { this.productCodeJson = productCodeJson; }
    public ProductGroup getProductGroup() { return productGroup; }
    public void setProductGroup(ProductGroup productGroup) { this.productGroup = productGroup; }
    public boolean isMarked() { return marked; }
    public void setMarked(boolean marked) { this.marked = marked; }
    public Integer getNdsRate() { return ndsRate; }
    public void setNdsRate(Integer ndsRate) { this.ndsRate = ndsRate; }
}

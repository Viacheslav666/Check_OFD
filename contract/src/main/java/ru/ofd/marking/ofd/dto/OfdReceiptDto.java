package ru.ofd.marking.ofd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfdReceiptDto {
    @JsonProperty("Id") private String id;
    @JsonProperty("CDateUtc") private String cDateUtc;
    @JsonProperty("Tag") private Integer tag;
    @JsonProperty("IsBso") private Boolean isBso;
    @JsonProperty("IsCorrection") private Boolean isCorrection;
    @JsonProperty("OperationType") private String operationType;
    @JsonProperty("UserInn") private String userInn;
    @JsonProperty("KktRegNumber") private String kktRegNumber;
    @JsonProperty("FnNumber") private String fnNumber;
    @JsonProperty("DocNumber") private Long docNumber;
    @JsonProperty("DocDateTime") private String docDateTime;
    @JsonProperty("DocShiftNumber") private Integer docShiftNumber;
    @JsonProperty("DecimalFiscalSign") private String decimalFiscalSign;
    @JsonProperty("ReceiptNumber") private Integer receiptNumber;
    @JsonProperty("DocRawId") private String docRawId;
    @JsonProperty("TotalSumm") private Long totalSumm;
    @JsonProperty("Items") private List<OfdItemDto> items;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCDateUtc() { return cDateUtc; }
    public void setCDateUtc(String cDateUtc) { this.cDateUtc = cDateUtc; }
    public Integer getTag() { return tag; }
    public void setTag(Integer tag) { this.tag = tag; }
    public Boolean getIsBso() { return isBso; }
    public void setIsBso(Boolean isBso) { this.isBso = isBso; }
    public Boolean getIsCorrection() { return isCorrection; }
    public void setIsCorrection(Boolean isCorrection) { this.isCorrection = isCorrection; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getUserInn() { return userInn; }
    public void setUserInn(String userInn) { this.userInn = userInn; }
    public String getKktRegNumber() { return kktRegNumber; }
    public void setKktRegNumber(String kktRegNumber) { this.kktRegNumber = kktRegNumber; }
    public String getFnNumber() { return fnNumber; }
    public void setFnNumber(String fnNumber) { this.fnNumber = fnNumber; }
    public Long getDocNumber() { return docNumber; }
    public void setDocNumber(Long docNumber) { this.docNumber = docNumber; }
    public String getDocDateTime() { return docDateTime; }
    public void setDocDateTime(String docDateTime) { this.docDateTime = docDateTime; }
    public Integer getDocShiftNumber() { return docShiftNumber; }
    public void setDocShiftNumber(Integer docShiftNumber) { this.docShiftNumber = docShiftNumber; }
    public String getDecimalFiscalSign() { return decimalFiscalSign; }
    public void setDecimalFiscalSign(String decimalFiscalSign) { this.decimalFiscalSign = decimalFiscalSign; }
    public Integer getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(Integer receiptNumber) { this.receiptNumber = receiptNumber; }
    public String getDocRawId() { return docRawId; }
    public void setDocRawId(String docRawId) { this.docRawId = docRawId; }
    public Long getTotalSumm() { return totalSumm; }
    public void setTotalSumm(Long totalSumm) { this.totalSumm = totalSumm; }
    public List<OfdItemDto> getItems() { return items; }
    public void setItems(List<OfdItemDto> items) { this.items = items; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OfdItemDto {
        @JsonProperty("Name") private String name;
        @JsonProperty("Price") private Long price;
        @JsonProperty("Quantity") private Double quantity;
        @JsonProperty("Total") private Long total;
        @JsonProperty("SubjectType") private Integer subjectType;
        @JsonProperty("ProductCode") private Map<String, Object> productCode;
        @JsonProperty("NDS_Rate") private Integer ndsRate;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getPrice() { return price; }
        public void setPrice(Long price) { this.price = price; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public Long getTotal() { return total; }
        public void setTotal(Long total) { this.total = total; }
        public Integer getSubjectType() { return subjectType; }
        public void setSubjectType(Integer subjectType) { this.subjectType = subjectType; }
        public Map<String, Object> getProductCode() { return productCode; }
        public void setProductCode(Map<String, Object> productCode) { this.productCode = productCode; }
        public Integer getNDS_Rate() { return ndsRate; }
        public void setNDS_Rate(Integer ndsRate) { this.ndsRate = ndsRate; }
    }
}

package ru.ofd.marking.ofd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfdKktDto {
    @JsonProperty("Id") private String id;
    @JsonProperty("KktRegId") private String kktRegId;
    @JsonProperty("KktName") private String kktName;
    @JsonProperty("SerialNumber") private String serialNumber;
    @JsonProperty("FnNumber") private String fnNumber;
    @JsonProperty("FiscalAddress") private String fiscalAddress;
    @JsonProperty("FiscalPlace") private String fiscalPlace;
    @JsonProperty("Path") private String path;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getKktRegId() { return kktRegId; }
    public void setKktRegId(String kktRegId) { this.kktRegId = kktRegId; }
    public String getKktName() { return kktName; }
    public void setKktName(String kktName) { this.kktName = kktName; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getFnNumber() { return fnNumber; }
    public void setFnNumber(String fnNumber) { this.fnNumber = fnNumber; }
    public String getFiscalAddress() { return fiscalAddress; }
    public void setFiscalAddress(String fiscalAddress) { this.fiscalAddress = fiscalAddress; }
    public String getFiscalPlace() { return fiscalPlace; }
    public void setFiscalPlace(String fiscalPlace) { this.fiscalPlace = fiscalPlace; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}

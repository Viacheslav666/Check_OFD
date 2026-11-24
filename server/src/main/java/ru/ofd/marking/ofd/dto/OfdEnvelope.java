package ru.ofd.marking.ofd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OfdEnvelope<T> {
    @JsonProperty("Status")
    private String status;
    @JsonProperty("Data")
    private T data;
    @JsonProperty("Elapsed")
    private String elapsed;
    @JsonProperty("Errors")
    private List<String> errors;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public String getElapsed() { return elapsed; }
    public void setElapsed(String elapsed) { this.elapsed = elapsed; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean isSuccess() {
        return "Success".equalsIgnoreCase(status);
    }
}

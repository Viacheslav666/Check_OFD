package ru.ofd.marking.ofd.client;

public class OfdApiException extends RuntimeException {
    private final int httpStatus;

    public OfdApiException(int httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public OfdApiException(int httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}

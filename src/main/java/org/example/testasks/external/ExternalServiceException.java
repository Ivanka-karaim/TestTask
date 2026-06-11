package org.example.testasks.external;

import org.springframework.http.HttpStatusCode;

public class ExternalServiceException extends RuntimeException {

    private final HttpStatusCode status;

    public ExternalServiceException(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}

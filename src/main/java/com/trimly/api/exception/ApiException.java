package com.trimly.api.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain-specific API runtime exceptions.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

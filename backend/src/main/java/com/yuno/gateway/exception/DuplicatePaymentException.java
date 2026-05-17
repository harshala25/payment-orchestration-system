package com.yuno.gateway.exception;

public class DuplicatePaymentException extends RuntimeException {
    private final Object existingResponse;

    public DuplicatePaymentException(String message, Object existingResponse) {
        super(message);
        this.existingResponse = existingResponse;
    }

    public Object getExistingResponse() {
        return existingResponse;
    }
}

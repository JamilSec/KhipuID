package com.jamilsec.khipuid.infrastructure.sunat.errors;

public class SunatHttpException extends RuntimeException {
    private final int statusCode;

    public SunatHttpException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
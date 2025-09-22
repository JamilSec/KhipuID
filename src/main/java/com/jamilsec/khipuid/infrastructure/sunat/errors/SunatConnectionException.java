package com.jamilsec.khipuid.infrastructure.sunat.errors;

public class SunatConnectionException extends RuntimeException {
    public SunatConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.example.ledger_service.exception;

import java.time.Instant;

public class ApiError {
    public enum Code { NOT_FOUND, VALIDATION, CONFLICT, INSUFFICIENT_FUNDS, SERVER_ERROR }
    private final Instant timestamp = Instant.now();
    private final Code code;
    private final String message;

    public ApiError(Code code, String message) {
        this.code = code;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public Code getCode() { return code; }
    public String getMessage() { return message; }
}
